# 脚本引擎架构设计

## 1. 新增/修改文件清单

```
app/src/main/assets/scripts/             # 脚本存放目录（新增）
  └── example_hongbao.json                # 示例脚本

app/src/main/java/com/clawp/android/script/   # 脚本引擎包（新增）
  ├── ScriptEngine.kt                     # 脚本执行引擎（核心）
  ├── ScriptParser.kt                     # JSON 解析为 POJO
  ├── model/
  │   ├── Script.kt                       # 脚本顶层 POJO
  │   ├── ScriptMeta.kt                   # meta 元信息
  │   ├── ScriptConfig.kt                 # config 配置
  │   ├── Rule.kt                         # 规则（conditions + actions）
  │   ├── Condition.kt                    # 条件
  │   ├── Action.kt                       # 动作
  │   ├── Target.kt                       # 控件定位
  │   └── HumanizeParams.kt              # 人类化参数
  ├── condition/
  │   ├── ConditionEvaluator.kt           # 条件求值接口
  │   ├── TextExistsEvaluator.kt          # text_exists
  │   ├── TextNotExistsEvaluator.kt       # text_not_exists
  │   ├── CurrentAppIsEvaluator.kt        # current_app_is
  │   ├── CurrentAppNotEvaluator.kt       # current_app_not
  │   └── NodeCountEvaluator.kt           # node_count
  ├── action/
  │   ├── ActionExecutor.kt               # 动作执行接口
  │   ├── OpenAppExecutor.kt              # open_app
  │   ├── ClickExecutor.kt                # click
  │   ├── LongPressExecutor.kt            # long_press
  │   ├── SwipeExecutor.kt                # swipe
  │   ├── WaitExecutor.kt                 # wait
  │   ├── PressBackExecutor.kt            # press_back
  │   ├── PressHomeExecutor.kt            # press_home
  │   ├── RestartRoundExecutor.kt         # restart_round
  │   ├── EndRoundExecutor.kt             # end_round
  │   └── ExitTaskExecutor.kt             # exit_task
  └── ScriptLoader.kt                     # 脚本加载接口

app/src/main/java/com/clawp/android/script/loader/
  └── AssetScriptLoader.kt               # 从 assets 加载（V0.2.1 实现）

app/src/main/java/com/clawp/android/     # 修改现有文件
  └── ClawApplication.kt                 # 添加 ScriptEngine 初始化

app/src/main/java/com/clawp/android/ui/
  └── HomeActivity.kt                    # 添加脚本列表 UI + 运行控制
```

---

## 2. 核心类设计

### 2.1 ScriptEngine — 脚本执行引擎

```kotlin
class ScriptEngine(
    private val accessibilityService: ClawAccessibilityService,
    private val scope: CoroutineScope
) {
    // 执行状态
    enum class State { IDLE, RUNNING, PAUSED, STOPPING }
    
    @Volatile var state: State = State.IDLE
    @Volatile var currentRound: Int = 0
    @Volatile var currentRuleName: String? = null
    
    // 回调
    var onStateChanged: ((State) -> Unit)? = null
    var onRoundUpdated: ((Int) -> Unit)? = null
    var onLog: ((String) -> Unit)? = null
    
    fun loadScript(json: String): Script
    fun start(script: Script)
    fun stop()
    
    // 内部循环
    private suspend fun executeLoop(script: Script)
    private fun executeSetup(script: Script)
    private fun evaluateConditions(rule: Rule, screenTree: String): Boolean
    private suspend fun executeActions(actions: List<Action>): ActionResult
}
```

### 2.2 条件求值流程

```
evaluateConditions(rule, screenTree):
    for each condition in rule.conditions:
        evaluator = ConditionEvaluatorFactory.create(condition.type)
        result = evaluator.evaluate(condition, screenTree, accessibilityService)
        if !result: return false
    return true
```

每个 `ConditionEvaluator` 实现：
- `TextExistsEvaluator`: 从 screenTree 文本中搜索指定文字（字符串匹配，无需再解析节点树）
- `CurrentAppIsEvaluator`: 从 `accessibilityService.getRootInActiveWindow().packageName` 获取

**关键设计点：screenTree 是纯文本字符串**。条件求值不重新解析节点树，而是在已有的 tree 文本上做字符串搜索。这避免了重复的 `getRootInActiveWindow()` 调用。

### 2.3 动作执行流程

```
executeActions(actions):
    for each action in actions:
        executor = ActionExecutorFactory.create(action.type)
        result = executor.execute(action, accessibilityService)
        if result is RESTART_ROUND: return RESTART_ROUND
        if result is END_ROUND: return END_ROUND
        if result is EXIT_TASK: return EXIT_TASK
        if result is FAILURE: return FAILURE  // 默认终止本轮
    return SUCCESS
```

### 2.4 人类化随机算法

```kotlin
// 坐标随机偏移：在实际点击位置周围 ±offsetPx 范围内随机
fun humanizeOffset(baseX: Int, baseY: Int, offsetPx: Int): Pair<Int, Int> {
    val dx = Random.nextInt(-offsetPx, offsetPx + 1)
    val dy = Random.nextInt(-offsetPx, offsetPx + 1)
    return (baseX + dx) to (baseY + dy)
}

// 时长随机化
fun humanizeDuration(baseDurationMs: Long, varMs: Int): Long {
    return baseDurationMs + Random.nextInt(-varMs, varMs + 1)
}

// 滑动路径抖动：在起点和终点之间的直线上添加随机中间点
fun humanizeSwipePath(from: Point, to: Point, jitterPx: Int): Path {
    val path = Path()
    path.moveTo(from.x.toFloat(), from.y.toFloat())
    val midX = (from.x + to.x) / 2 + Random.nextInt(-jitterPx, jitterPx + 1)
    val midY = (from.y + to.y) / 2 + Random.nextInt(-jitterPx, jitterPx + 1)
    path.quadTo(midX.toFloat(), midY.toFloat(), to.x.toFloat(), to.y.toFloat())
    return path
}
```

---

## 3. 与现有代码的关系

| 现有组件 | 脚本引擎使用方式 |
|----------|-----------------|
| `ClawAccessibilityService.getScreenTree()` | 每轮开始时调用，获取屏幕树文本 |
| `ClawAccessibilityService.performClick(node)` | ClickExecutor 使用（查找节点 → performClick） |
| `ClawAccessibilityService.performTap(x, y)` | ClickExecutor 坐标模式备用 |
| `ClawAccessibilityService.performSwipe()` | SwipeExecutor 使用 |
| `ClawAccessibilityService.findNodesByText()` | 定位控件时使用 |
| `ClawAccessibilityService.openApp()` | OpenAppExecutor 使用 |
| `ClawAccessibilityService.performGlobalBack()` | PressBackExecutor 使用 |
| `ForegroundService` | 脚本执行时启动前台服务保持进程存活 |
| `FloatingCircleManager` | 显示当前轮次和状态 |

**ScriptEngine 是 Agent 模式的替代方案**，与 `TaskOrchestrator` 平级。两者不冲突——Agent 模式通过飞书消息触发 agent 决策，Script 模式通过 UI 选择脚本后自动循环执行。

---

## 4. UI 交互流程

```
HomeActivity:
  ┌─────────────────────────────┐
  │  ClawP v0.2.x               │
  │─────────────────────────────│
  │  [Agent 模式]  [脚本模式]    │  ← Tab 切换
  │─────────────────────────────│
  │  脚本列表:                   │
  │  ┌───────────────────────┐  │
  │  │ 惊喜红包自动任务        │  │  ← 可点击选择
  │  │ 描述: 自动完成...       │  │
  │  └───────────────────────┘  │
  │  ┌───────────────────────┐  │
  │  │ (更多脚本...)          │  │
  │  └───────────────────────┘  │
  │─────────────────────────────│
  │  状态: IDLE                 │
  │  [▶ 开始执行]               │  ← 选中脚本后可用
  └─────────────────────────────┘

执行中状态:
  ┌─────────────────────────────┐
  │  执行中: 惊喜红包自动任务     │
  │  轮次: 42 / 1000            │
  │  当前规则: 邀请有奖+看视频    │
  │─────────────────────────────│
  │  [■ 停止]                   │
  └─────────────────────────────┘
```

---

## 5. 版本号升级

`build_number.txt` → 切换到 `0.2.1` 版本号体系：
- 修改 `build.gradle.kts` 中的 versionName 逻辑
- 新格式: `clawp_v0.2.1_YYYYMMDD_HHmmss.apk`

---

## 6. 实现优先级

| 优先级 | 模块 | 说明 |
|--------|------|------|
| P0 | `model/` POJO + `ScriptParser` | 脚本解析是基础 |
| P0 | `ScriptLoader` + `AssetScriptLoader` | 从 assets 加载脚本 |
| P0 | `ScriptEngine` 主循环 | 核心执行逻辑 |
| P0 | `ClickExecutor` + `OpenAppExecutor` + `WaitExecutor` | 基本操作 |
| P0 | `TextExistsEvaluator` + `CurrentAppIsEvaluator` | 基本条件 |
| P0 | `PressBackExecutor` + `EndRoundExecutor` + `RestartRoundExecutor` | 流程控制 |
| P1 | `LongPressExecutor` + `SwipeExecutor` | 高级操作 |
| P1 | `humanize` 随机化 | 模拟人类操作 |
| P1 | HomeActivity 脚本选择 UI | UI 交互 |
| P2 | `TextNotExistsEvaluator` + `NodeCountEvaluator` | 扩展条件 |
| P2 | 前台通知 + 悬浮窗状态更新 | 用户体验 |
