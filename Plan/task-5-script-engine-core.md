# 子任务 5：ScriptEngine — 核心执行引擎

## 目标
实现脚本主循环引擎，串联条件求值、动作执行、轮次控制。

## 输出文件

```
app/src/main/java/com/clawp/android/script/
  └── ScriptEngine.kt               # 核心执行引擎
```

## 具体内容

### 5.1 `ScriptEngine.kt` — 核心类

```kotlin
class ScriptEngine {
    enum class State { IDLE, RUNNING, STOPPING }

    @Volatile var state: State = State.IDLE
        private set
    @Volatile var currentRound: Int = 0
        private set
    @Volatile var currentRuleName: String? = null
        private set

    // 回调
    var onStateChanged: ((State) -> Unit)? = null
    var onRoundUpdated: ((Int, String?) -> Unit)? = null  // round, ruleName
    var onLog: ((String) -> Unit)? = null

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var job: Job? = null

    fun start(script: Script): Boolean {
        // 1. 检查无障碍服务
        val service = ClawAccessibilityService.getInstance()
        if (service == null) {
            onLog?.invoke("AccessibilityService 未连接")
            return false
        }

        // 2. 启动前台服务
        ForegroundService.start(context)

        // 3. 启动执行协程
        state = State.RUNNING
        job = scope.launch {
            try {
                // 3a. 执行 setup
                executeSetup(script, service)

                // 3b. 主循环
                currentRound = 0
                val maxRounds = script.config.loopCount
                val startTime = System.currentTimeMillis()
                val maxDurationMs = script.config.loopDurationSec?.let { it * 1000L }

                while (currentRound < maxRounds && state == State.RUNNING) {
                    // 检查时长限制
                    if (maxDurationMs != null &&
                        System.currentTimeMillis() - startTime > maxDurationMs) {
                        onLog?.invoke("达到最大运行时长，任务结束")
                        break
                    }

                    // 执行一轮
                    val roundResult = executeRound(script, service)
                    when (roundResult) {
                        ActionResult.EXIT_TASK -> break
                        else -> {
                            currentRound++
                            onRoundUpdated?.invoke(currentRound, currentRuleName)
                        }
                    }

                    // 轮次间延迟
                    if (state == State.RUNNING && currentRound < maxRounds) {
                        val delay = HumanizeUtils.randomDelay(
                            script.config.roundDelay.min,
                            script.config.roundDelay.max
                        )
                        delay(delay)
                    }
                }
            } catch (e: CancellationException) {
                onLog?.invoke("任务被取消")
            } catch (e: Exception) {
                onLog?.invoke("任务异常: ${e.message}")
            } finally {
                state = State.IDLE
                onStateChanged?.invoke(State.IDLE)
            }
        }
        return true
    }

    fun stop() {
        state = State.STOPPING
        job?.cancel()
        scope.launch {
            delay(1000)
            if (state == State.STOPPING) {
                state = State.IDLE
                onStateChanged?.invoke(State.IDLE)
            }
        }
    }

    private suspend fun executeSetup(script: Script, service: ClawAccessibilityService) {
        for (action in script.setup) {
            if (state != State.RUNNING) return
            val executor = ActionExecutorFactory.create(action.type)
            executor.execute(action, service)
        }
    }

    private suspend fun executeRound(script: Script, service: ClawAccessibilityService): ActionResult {
        // 1. 读取屏幕树
        val screenTree = service.getScreenTree()

        // 2. 遍历规则
        for (rule in script.rules) {
            if (state != State.RUNNING) return ActionResult.EXIT_TASK

            val matched = evaluateConditions(rule, screenTree, service)
            if (!matched) continue

            onLog?.invoke("规则匹配: ${rule.name ?: "(unnamed)"}")
            currentRuleName = rule.name

            // 3. 执行 actions
            for (action in rule.actions) {
                if (state != State.RUNNING) return ActionResult.EXIT_TASK

                val executor = ActionExecutorFactory.create(action.type)
                val result = executor.execute(action, service)

                when (result) {
                    ActionResult.RESTART_ROUND -> {
                        onLog?.invoke("→ 重新开始本轮")
                        // 重新执行当前轮（不消耗轮次计数）
                        return ActionResult.RESTART_ROUND
                    }
                    ActionResult.END_ROUND -> {
                        onLog?.invoke("→ 结束本轮")
                        return ActionResult.END_ROUND
                    }
                    ActionResult.EXIT_TASK -> {
                        onLog?.invoke("→ 退出任务")
                        return ActionResult.EXIT_TASK
                    }
                    ActionResult.FAILURE -> {
                        onLog?.invoke("动作失败，跳过本轮")
                        return ActionResult.FAILURE
                    }
                    ActionResult.SUCCESS -> {
                        // 继续执行下一个 action
                    }
                }
            }
            // 规则 actions 全部执行完 → 结束本轮
            return ActionResult.END_ROUND
        }

        // 没有规则匹配
        onLog?.invoke("无规则匹配，结束本轮")
        return ActionResult.END_ROUND
    }

    private fun evaluateConditions(
        rule: Rule,
        screenTree: String?,
        service: ClawAccessibilityService
    ): Boolean {
        for (condition in rule.conditions) {
            val evaluator = ConditionEvaluatorFactory.create(condition.type)
            if (!evaluator.evaluate(condition, screenTree, service)) {
                return false
            }
        }
        return true
    }
}
```

### 5.2 关键设计要点

#### 5.2.1 单例 vs 多实例
`ScriptEngine` 作为全局单例，创建于 `ClawApplication.onCreate()` 中。

#### 5.2.2 错误处理策略（按问题6的确认）
| 场景 | 处理 |
|------|------|
| 单个 action 失败 | 终止当前轮次 → 等待 delay → 下一轮 |
| AccessibilityService 断开 | 终止任务 → 通知用户 → 恢复到 IDLE |
| 协程取消 | 优雅停止 → 恢复到 IDLE |

#### 5.2.3 `restart_round` 不消耗轮次
restart_round 返回 RESTART_ROUND 后，外层循环不递增 `currentRound`，直接重新执行规则判断。

#### 5.2.4 最大时长限制
`loopDurationSec` 和 `loopCount` 同时存在时取其先到达者。

## 依赖
- 子任务 1-4 全部产出
- `ClawAccessibilityService`
- `ForegroundService`
- `HumanizeUtils`

## 验收标准
- setup 步骤仅执行一次
- 主循环按规则顺序匹配，第一条匹配即执行
- `restart_round` 不消耗轮次计数
- `end_round` / 全部 actions 执行完 → 等待 delay → 下一轮
- `exit_task` 退出循环
- `stop()` 能中止执行
- 最大轮次和最大时长限制生效