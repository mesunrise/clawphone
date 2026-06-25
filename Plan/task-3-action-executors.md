# 子任务 3：动作执行器

## 目标
实现所有动作类型的执行逻辑，包含人类化随机参数。

## 输出文件

```
app/src/main/java/com/clawp/android/script/action/
  ├── ActionExecutor.kt            # 执行器接口
  ├── OpenAppExecutor.kt           # open_app
  ├── ClickExecutor.kt             # click
  ├── LongPressExecutor.kt         # long_press
  ├── SwipeExecutor.kt             # swipe
  ├── WaitExecutor.kt              # wait
  ├── SystemKeyExecutor.kt         # press_back / press_home
  ├── FlowControlExecutor.kt       # restart_round / end_round / exit_task

app/src/main/java/com/clawp/android/script/
  └── ActionExecutorFactory.kt     # 工厂类
  └── HumanizeUtils.kt             # 人类化随机工具
```

## 具体内容

### 3.1 `ActionExecutor.kt` — 接口
```kotlin
interface ActionExecutor {
    suspend fun execute(
        action: Action,
        service: ClawAccessibilityService
    ): ActionResult
}
```

### 3.2 `HumanizeUtils.kt` — 人类化工具
```kotlin
object HumanizeUtils {
    private val rng = Random()

    /** 坐标随机偏移 */
    fun offset(baseX: Int, baseY: Int, offsetPx: Int): Pair<Int, Int> {
        val dx = if (offsetPx > 0) rng.nextInt(-offsetPx, offsetPx + 1) else 0
        val dy = if (offsetPx > 0) rng.nextInt(-offsetPx, offsetPx + 1) else 0
        return (baseX + dx) to (baseY + dy)
    }

    /** 点击时长随机化 */
    fun tapDuration(baseMs: Long): Long = baseMs

    /** 长按/滑动时长随机化 */
    fun varDuration(baseMs: Long, varMs: Int): Long {
        return if (varMs > 0) baseMs + rng.nextInt(-varMs, varMs + 1) else baseMs
    }

    /** 随机延迟（秒） */
    fun randomDelay(minSec: Double, maxSec: Double): Long {
        val ms = (minSec + rng.nextDouble() * (maxSec - minSec)) * 1000
        return ms.toLong()
    }
}
```

### 3.3 `ClickExecutor.kt` — click
- 根据 `target.by` 定位控件：
  - `"text"`: 调用 `service.findNodesByText(value)` → 按 `match` 过滤 → 按 `index` 选择 → 优先选可点击节点
  - `"desc"`: 调用 `service.findNodesByText(value)`（contentDescription 也通过 text 搜索）→ 同上
  - `"id"`: 调用 `service.findNodesById(value)` → 按 `index` 选择
  - `"coordinate"`: 直接使用 `target.x`, `target.y`
- 如果 `humanize.offsetPx > 0`，在中心点坐标上添加随机偏移
- 调用 `service.performClick(node)` 或 `service.performTap(x, y, humanize.tapDurationMs)`
- 成功返回 `SUCCESS`，失败返回 `FAILURE`

### 3.4 `LongPressExecutor.kt` — long_press
- 定位逻辑同 ClickExecutor
- 时长 = `humanizeUtils.varDuration(humanize.durationMs, humanize.durationVarMs)`
- 调用 `service.performLongPress(x, y, duration)`

### 3.5 `SwipeExecutor.kt` — swipe
- 起点/终点坐标来自 `from` 和 `to` 的 `Target`
- 起点偏移：`HumanizeUtils.offset(fromX, fromY, humanize.fromOffsetPx)`
- 终点偏移：`HumanizeUtils.offset(toX, toY, humanize.toOffsetPx)`
- 路径抖动：在起点和终点之间插入随机中间点（`humanize.jitterPx`）
- 时长 = `HumanizeUtils.varDuration(humanize.durationMs, humanize.durationVarMs)`
- 调用 `service.performSwipe(...)`

### 3.6 `OpenAppExecutor.kt` — open_app
- 调用 `service.openApp(action.package)`
- 返回 `SUCCESS` 或 `FAILURE`

### 3.7 `WaitExecutor.kt` — wait
- `delay(action.durationMs ?: 1000)`
- 返回 `SUCCESS`

### 3.8 `SystemKeyExecutor.kt` — press_back / press_home
- `press_back`: `service.performGlobalBack()`
- `press_home`: `service.performGlobalHome()`
- 返回 `SUCCESS`

### 3.9 `FlowControlExecutor.kt` — restart_round / end_round / exit_task
- `restart_round` → `ActionResult.RESTART_ROUND`
- `end_round` → `ActionResult.END_ROUND`
- `exit_task` → `ActionResult.EXIT_TASK`

### 3.10 `ActionExecutorFactory.kt`
```kotlin
object ActionExecutorFactory {
    fun create(type: String): ActionExecutor = when (type) {
        "open_app" -> OpenAppExecutor()
        "click" -> ClickExecutor()
        "long_press" -> LongPressExecutor()
        "swipe" -> SwipeExecutor()
        "wait" -> WaitExecutor()
        "press_back", "press_home" -> SystemKeyExecutor()
        "restart_round", "end_round", "exit_task" -> FlowControlExecutor()
        else -> throw IllegalArgumentException("Unknown action type: $type")
    }
}
```

## 依赖
- 子任务 1 的 POJO（`Action`, `Target`, `HumanizeParams`）
- `ClawAccessibilityService`

## 验收标准
- 每种动作类型执行正确
- 人类化随机参数生效（偏移、时长变化）
- 控件定位失败时返回 `FAILURE`