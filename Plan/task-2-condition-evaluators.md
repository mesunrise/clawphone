# 子任务 2：条件求值器

## 目标
实现所有条件类型的求值逻辑。条件求值基于屏幕树文本字符串（不重新解析节点树），以提高性能。

## 输出文件

```
app/src/main/java/com/clawp/android/script/condition/
  ├── ConditionEvaluator.kt           # 求值器接口
  ├── TextExistsEvaluator.kt          # text_exists / text_not_exists
  ├── DescExistsEvaluator.kt          # desc_exists  
  ├── CurrentAppEvaluator.kt          # current_app_is / current_app_not
  └── NodeCountEvaluator.kt           # node_count

app/src/main/java/com/clawp/android/script/
  └── ConditionEvaluatorFactory.kt    # 工厂类
```

## 具体内容

### 2.1 `ConditionEvaluator.kt` — 接口
```kotlin
interface ConditionEvaluator {
    /**
     * @param condition 条件对象
     * @param screenTreeText 当前屏幕树文本（来自 getScreenTree()）
     * @param service AccessibilityService 实例（用于获取 packageName 等）
     * @return 是否满足条件
     */
    fun evaluate(
        condition: Condition,
        screenTreeText: String?,
        service: ClawAccessibilityService?
    ): Boolean
}
```

### 2.2 `TextExistsEvaluator.kt` — text_exists / text_not_exists
- 在 `screenTreeText` 中搜索 `text="...值..."` 模式
- 支持 4 种匹配模式：`contains`、`exact`、`startsWith`、`endsWith`
- `text_exists` 返回 `true` 表示找到，`text_not_exists` 返回 `true` 表示未找到

### 2.3 `DescExistsEvaluator.kt` — desc_exists
- 在 `screenTreeText` 中搜索 `desc="...值..."` 模式
- 匹配逻辑同上

### 2.4 `CurrentAppEvaluator.kt` — current_app_is / current_app_not
- 从 `service.getRootInActiveWindow()?.packageName` 获取当前前景包名
- 与 `condition.package` 比较
- `current_app_is` 返回是否匹配，`current_app_not` 返回是否不匹配

### 2.5 `NodeCountEvaluator.kt` — node_count
- 使用 `service.findNodesByText()` 或 `service.findNodesById()` 获取匹配节点列表
- 根据 `operator`（`eq`、`gt`、`gte`、`lt`、`lte`）与 `count` 比较

### 2.6 `ConditionEvaluatorFactory.kt`
```kotlin
object ConditionEvaluatorFactory {
    fun create(type: String): ConditionEvaluator = when (type) {
        "text_exists" -> TextExistsEvaluator(exists = true)
        "text_not_exists" -> TextExistsEvaluator(exists = false)
        "desc_exists" -> DescExistsEvaluator()
        "current_app_is" -> CurrentAppEvaluator(isCheck = true)
        "current_app_not" -> CurrentAppEvaluator(isCheck = false)
        "node_count" -> NodeCountEvaluator()
        else -> throw IllegalArgumentException("Unknown condition type: $type")
    }
}
```

## 依赖
- 子任务 1 的 POJO（`Condition`）
- `ClawAccessibilityService`（仅 `CurrentAppEvaluator` 和 `NodeCountEvaluator` 需要）

## 验收标准
- 每种条件类型求值正确
- 屏幕树为 null 时优雅降级（返回 false）
- AccessibilityService 未连接时返回 false