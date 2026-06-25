package com.clawp.android.script

import com.clawp.android.ClawApplication
import com.clawp.android.script.model.*
import com.clawp.android.service.ClawAccessibilityService
import com.clawp.android.service.ForegroundService
import kotlinx.coroutines.*

/**
 * Core script execution engine.
 *
 * Lifecycle: IDLE → RUNNING → STOPPING → IDLE
 *
 * Created as a singleton in [ClawApplication.onCreate].
 */
class ScriptEngine {

    enum class State { IDLE, RUNNING, STOPPING }

    @Volatile var state: State = State.IDLE
        private set

    @Volatile var currentRound: Int = 0
        private set

    @Volatile var currentRuleName: String? = null
        private set

    /** Callback when the engine state changes. */
    var onStateChanged: ((State) -> Unit)? = null

    /** Callback for round progress updates. Parameters: round number, rule name. */
    var onRoundUpdated: ((Int, String?) -> Unit)? = null

    /** Callback for log messages (UI log display). */
    var onLog: ((String) -> Unit)? = null

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var job: Job? = null

    /** Set to true temporarily to dump screen tree to logs for debugging. */
    var debugDumpScreenTree: Boolean = false

    /**
     * Start executing [script].
     *
     * @return true if the engine started successfully, false if the
     *         accessibility service is unavailable or the engine is
     *         already running.
     */
    fun start(script: Script): Boolean {
        if (state != State.IDLE) {
            onLog?.invoke("引擎已在运行中")
            return false
        }

        val service = ClawAccessibilityService.getInstance()
        if (service == null) {
            onLog?.invoke("✗ AccessibilityService 未连接，请先开启无障碍服务")
            return false
        }

        val context = ClawApplication.instance
        ForegroundService.start(context)

        state = State.RUNNING
        onStateChanged?.invoke(State.RUNNING)
        onLog?.invoke("✓ 脚本「${script.meta.name}」开始执行")

        job = scope.launch {
            try {
                executeSetup(script, service)

                currentRound = 0
                val maxRounds = script.config.loopCount
                val startTime = System.currentTimeMillis()
                val maxDurationMs = script.config.loopDurationSec?.let { it * 1000L }

                while (currentRound < maxRounds && state == State.RUNNING) {
                    // Check duration limit
                    if (maxDurationMs != null &&
                        System.currentTimeMillis() - startTime > maxDurationMs
                    ) {
                        onLog?.invoke("✓ 达到最大运行时长 (${script.config.loopDurationSec}s)，任务结束")
                        break
                    }

                    // Check accessibility service still alive
                    if (ClawAccessibilityService.getInstance() == null) {
                        onLog?.invoke("✗ AccessibilityService 连接断开，任务终止")
                        break
                    }

                    val roundResult = executeRound(script, service)

                    when (roundResult) {
                        ActionResult.EXIT_TASK -> {
                            onLog?.invoke("✓ 任务完成（exit_task）")
                            break
                        }
                        ActionResult.RESTART_ROUND -> {
                            onLog?.invoke("→ 重新开始本轮（不消耗轮次计数）")
                            // Do NOT increment currentRound; restart_round don't consume
                            continue
                        }
                        else -> {
                            currentRound++
                            onRoundUpdated?.invoke(currentRound, currentRuleName)
                        }
                    }

                    // Inter-round delay
                    if (state == State.RUNNING && currentRound < maxRounds) {
                        val delayMs = HumanizeUtils.randomDelay(
                            script.config.roundDelay.min,
                            script.config.roundDelay.max
                        )
                        onLog?.invoke("⏳ 等待 ${delayMs / 1000.0}s 后进入下一轮...")
                        delay(delayMs)
                    }
                }

                if (currentRound >= maxRounds) {
                    onLog?.invoke("✓ 达到最大轮次 ($maxRounds)，任务结束")
                }
            } catch (e: CancellationException) {
                onLog?.invoke("⚠ 任务被取消")
            } catch (e: Exception) {
                onLog?.invoke("✗ 任务异常: ${e.message}")
                com.clawp.android.utils.XLog.e("ScriptEngine", "Execution error", e)
            } finally {
                state = State.IDLE
                currentRuleName = null
                onStateChanged?.invoke(State.IDLE)
                onLog?.invoke("引擎已停止")
            }
        }
        return true
    }

    /**
     * Stop the engine gracefully.
     */
    fun stop() {
        if (state != State.RUNNING) return
        state = State.STOPPING
        onLog?.invoke("正在停止引擎...")
        job?.cancel()
        // Fallback: force reset after 3 seconds if coroutine didn't finish
        scope.launch {
            delay(3000)
            if (state == State.STOPPING) {
                state = State.IDLE
                onStateChanged?.invoke(State.IDLE)
            }
        }
    }

    // ── private ────────────────────────────────────────────────────────

    private suspend fun executeSetup(script: Script, service: ClawAccessibilityService) {
        if (script.setup.isEmpty()) return
        onLog?.invoke("→ 执行 setup 步骤 (${script.setup.size} 个)")
        for (action in script.setup) {
            if (state != State.RUNNING) return
            val executor = ActionExecutorFactory.create(action.type)
            executor.execute(action, service)
        }
        onLog?.invoke("✓ setup 完成，进入主循环")
    }

    private suspend fun executeRound(
        script: Script,
        service: ClawAccessibilityService
    ): ActionResult {
        // 1. Read screen tree
        val screenTree = service.getScreenTree()

        // Debug: dump screen tree to logs
        if (debugDumpScreenTree && screenTree != null) {
            onLog?.invoke("=== SCREEN TREE ===")
            onLog?.invoke(screenTree)
            onLog?.invoke("=== END SCREEN TREE ===")
        }

        // 2. Find first matching rule
        for (rule in script.rules) {
            if (state != State.RUNNING) return ActionResult.EXIT_TASK

            val matched = evaluateConditions(rule, screenTree, service)
            if (!matched) continue

            onLog?.invoke("  ✓ 规则匹配: ${rule.name ?: "(unnamed)"}")
            currentRuleName = rule.name

            // 3. Execute actions in sequence
            for (action in rule.actions) {
                if (state != State.RUNNING) return ActionResult.EXIT_TASK

                val executor = ActionExecutorFactory.create(action.type)
                val result = executor.execute(action, service)

                when (result) {
                    ActionResult.RESTART_ROUND -> return ActionResult.RESTART_ROUND
                    ActionResult.END_ROUND -> return ActionResult.END_ROUND
                    ActionResult.EXIT_TASK -> return ActionResult.EXIT_TASK
                    ActionResult.FAILURE -> {
                        onLog?.invoke("  ✗ 动作 ${action.type} 失败，跳过本轮")
                        return ActionResult.FAILURE
                    }
                    ActionResult.SUCCESS -> {
                        // Continue to next action
                    }
                }
            }

            // All actions executed successfully → end round
            return ActionResult.END_ROUND
        }

        // No rule matched
        onLog?.invoke("  - 无规则匹配，结束本轮")
        return ActionResult.END_ROUND
    }

    private fun evaluateConditions(
        rule: Rule,
        screenTree: String?,
        service: ClawAccessibilityService
    ): Boolean {
        // Empty conditions → always match (fallback rule)
        if (rule.conditions.isEmpty()) return true

        for (condition in rule.conditions) {
            val evaluator = ConditionEvaluatorFactory.create(condition.type)
            if (!evaluator.evaluate(condition, screenTree, service)) {
                return false
            }
        }
        return true
    }
}