# 子任务 4：ScriptLoader — 脚本加载器

## 目标
实现 `ScriptLoader` 接口和 `AssetScriptLoader` 实现，从 APK 内置 assets 加载脚本并展示可选列表。

## 输出文件

```
app/src/main/java/com/clawp/android/script/
  └── ScriptLoader.kt              # 加载器接口

app/src/main/java/com/clawp/android/script/loader/
  └── AssetScriptLoader.kt         # 从 assets 加载
```

## 具体内容

### 4.1 `ScriptLoader.kt` — 接口
```kotlin
interface ScriptLoader {
    /** 列出所有可用脚本的元信息 */
    fun listScripts(): List<ScriptMeta>

    /** 加载完整脚本 */
    fun loadScript(name: String): Script
}
```

### 4.2 `AssetScriptLoader.kt` — 从 assets 加载
- 构造函数接收 `Context`
- `listScripts()`:
  - 扫描 `assets/scripts/` 目录下所有 `.json` 文件
  - 对每个文件只解析 `meta` 字段（轻量，不全量解析）
  - 返回 `List<ScriptMeta>`
- `loadScript(name)`:
  - 从 `assets/scripts/{name}.json` 读取完整内容
  - 调用 `ScriptParser.parse()` 解析为 `Script` 对象

### 4.3 示例脚本
创建 `app/src/main/assets/scripts/example_hongbao.json`：
- 使用 `Plan/script-format-design.md` 中的完整示例
- 作为测试和参考用

## 依赖
- 子任务 1 的 `ScriptParser`、POJOs

## 验收标准
- `listScripts()` 能列出 assets 中所有脚本的 meta 信息
- `loadScript()` 能正确加载并解析完整脚本
- 不支持的文件名或解析失败时抛出明确异常