# 子任务 1：脚本数据模型 (POJOs + Parser)

## 目标
创建脚本领域的所有 Kotlin 数据类，以及将 JSON 字符串解析为 POJO 的 `ScriptParser`。

## 输出文件

```
app/src/main/java/com/clawp/android/script/model/
  ├── Script.kt           # 顶层脚本对象
  ├── ScriptMeta.kt        # meta 元信息
  ├── ScriptConfig.kt      # config 全局配置
  ├── Rule.kt              # 规则（条件 + 动作列表）
  ├── Condition.kt         # 条件对象
  ├── Action.kt            # 动作对象
  ├── Target.kt            # 控件定位描述
  ├── HumanizeParams.kt    # 人类化参数
  ├── RoundDelay.kt        # 轮次延迟配置
  └── ActionResult.kt      # 动作执行结果枚举

app/src/main/java/com/clawp/android/script/
  └── ScriptParser.kt      # JSON → Script POJO
```

## 具体内容

### 1.1 `ScriptMeta.kt`
```kotlin
data class ScriptMeta(
    val name: String,
    val version: String? = null,
    val description: String? = null
)
```

### 1.2 `RoundDelay.kt`
```kotlin
data class RoundDelay(
    val min: Double = 3.0,
    val max: Double = 5.0
)
```

### 1.3 `ScriptConfig.kt`
```kotlin
data class ScriptConfig(
    val targetPackage: String,
    val loopCount: Int = 1000,
    val loopDurationSec: Int? = null,
    val roundDelay: RoundDelay = RoundDelay()
)
```

### 1.4 `Target.kt`
```kotlin
data class Target(
    val by: String,           // "text", "desc", "id", "coordinate"
    val value: String? = null,
    val match: String? = null, // "contains", "exact", "startsWith", "endsWith"
    val index: Int = 0,
    val x: Int? = null,       // 仅 coordinate 模式
    val y: Int? = null         // 仅 coordinate 模式
)
```

### 1.5 `HumanizeParams.kt`
```kotlin
data class HumanizeParams(
    val offsetPx: Int = 0,        // 位置随机偏移
    val tapDurationMs: Long = 100, // 点击时长
    val durationMs: Long = 1000,   // 长按/滑动基准时长
    val durationVarMs: Int = 0,    // 时长随机变化范围
    val fromOffsetPx: Int = 0,     // 滑动起点偏移
    val toOffsetPx: Int = 0,       // 滑动终点偏移
    val jitterPx: Int = 0          // 路径抖动
)
```

### 1.6 `Condition.kt`
```kotlin
data class Condition(
    val type: String,           // "text_exists", "text_not_exists", "desc_exists",
                                // "current_app_is", "current_app_not", "node_count"
    val text: String? = null,
    val desc: String? = null,
    val `package`: String? = null,
    val match: String? = null,
    val by: String? = null,     // node_count 用
    val value: String? = null,  // node_count 用
    val operator: String? = null, // node_count 用
    val count: Int? = null      // node_count 用
)
```

### 1.7 `Action.kt`
```kotlin
data class Action(
    val type: String,           // "open_app", "click", "long_press", "swipe",
                                // "wait", "press_back", "press_home", "restart_round",
                                // "end_round", "exit_task"
    val `package`: String? = null,
    val target: Target? = null,
    val from: Target? = null,   // swipe from
    val to: Target? = null,     // swipe to
    val humanize: HumanizeParams? = null,
    val durationMs: Long? = null // wait 用
)
```

### 1.8 `Rule.kt`
```kotlin
data class Rule(
    val name: String? = null,
    val conditions: List<Condition> = emptyList(),
    val actions: List<Action> = emptyList()
)
```

### 1.9 `Script.kt`
```kotlin
data class Script(
    val meta: ScriptMeta,
    val config: ScriptConfig,
    val setup: List<Action> = emptyList(),
    val rules: List<Rule> = emptyList()
)
```

### 1.10 `ActionResult.kt`
```kotlin
enum class ActionResult {
    SUCCESS,        // 动作成功，继续执行下一个
    FAILURE,        // 动作失败，终止当前轮次
    RESTART_ROUND,  // 重新开始当前轮次
    END_ROUND,      // 结束当前轮次
    EXIT_TASK       // 终止整个任务
}
```

### 1.11 `ScriptParser.kt`
- 使用 `org.json.JSONObject` 解析（Android 内置，无需额外依赖）
- 支持 `{{config.targetPackage}}` 变量替换
- 解析方法：`fun parse(jsonString: String): Script`
- 错误处理：字段缺失时抛 `IllegalArgumentException` 并附带明确错误信息

## 依赖
无（纯数据层，不依赖 Android 组件）

## 验收标准
- 所有 POJO 编译通过
- `ScriptParser.parse()` 能正确解析 `Plan/script-format-design.md` 中的示例 JSON
- 变量替换 `{{config.targetPackage}}` 正常工作
- 缺失必填字段时抛出明确异常