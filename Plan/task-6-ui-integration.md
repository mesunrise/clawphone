# 子任务 6：UI 集成 — HomeActivity 脚本列表

## 目标
修改 `HomeActivity`，增加脚本模式 Tab，展示可用脚本列表，支持选择、启动、停止脚本。

## 输出文件

```
app/src/main/java/com/clawp/android/ui/HomeActivity.kt   # 修改
app/src/main/res/layout/activity_home.xml                 # 修改
app/src/main/res/values/strings.xml                       # 新增字符串
app/src/main/java/com/clawp/android/ClawApplication.kt   # 修改（初始化 ScriptEngine）
```

## 具体内容

### 6.1 初始化 ScriptEngine

在 [`ClawApplication.kt`](app/src/main/java/com/clawp/android/ClawApplication.kt) 中：
```kotlin
// 新增
lateinit var scriptEngine: ScriptEngine
    private set

// 在 onCreate() 中
scriptEngine = ScriptEngine()
scriptLoader = AssetScriptLoader(this)
```

### 6.2 HomeActivity 布局变更

增加两个 Tab："Agent 模式" 和 "脚本模式"（或两个 Button 切换）。

脚本模式 UI：
```
┌──────────────────────────────┐
│  [Agent 模式]  [脚本模式]      │
├──────────────────────────────┤
│  可用脚本:                    │
│  ┌────────────────────────┐  │
│  │ 惊喜红包自动任务         │  │ ← RecyclerView item
│  │ 描述: 自动完成...        │  │
│  │                        │  │
│  └────────────────────────┘  │
│                              │
│  选择: 惊喜红包自动任务        │ ← 选中脚本名
│──────────────────────────────│
│  状态: IDLE                  │ ← 状态展示
│  轮次: 0 / 1000              │ ← 运行中展示
│                              │
│  [▶ 开始执行]  [■ 停止]      │ ← 按钮
│──────────────────────────────│
│  日志:                       │
│  规则匹配: 邀请有奖+看视频    │ ← 滚动日志
│  动作: click → 成功          │
│  ...                        │
└──────────────────────────────┘
```

### 6.3 HomeActivity 代码改动

```kotlin
// 新增成员
private lateinit var scriptLoader: ScriptLoader
private val scriptEngine get() = ClawApplication.instance.scriptEngine
private var selectedScript: ScriptMeta? = null

// 新增方法
private fun setupScriptTab() {
    // 加载脚本列表
    val scripts = scriptLoader.listScripts()
    // 绑定到 RecyclerView adapter
    // 点击选择脚本
}

private fun startScript() {
    val meta = selectedScript ?: return

    // 1. 检查无障碍服务
    if (!ClawAccessibilityService.isRunning()) {
        Toast.makeText(this, "请先开启无障碍服务", Toast.LENGTH_LONG).show()
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        return
    }

    // 2. 加载脚本
    val script = scriptLoader.loadScript(meta.name)

    // 3. 设置引擎回调
    scriptEngine.onStateChanged = { state ->
        runOnUiThread {
            updateStateDisplay(state)
        }
    }
    scriptEngine.onRoundUpdated = { round, ruleName ->
        runOnUiThread {
            tvRound.text = "轮次: $round / ${script.config.loopCount}"
            tvRuleName.text = "规则: ${ruleName ?: "-"}"
        }
    }
    scriptEngine.onLog = { msg ->
        runOnUiThread {
            appendLog(msg)
        }
    }

    // 4. 启动
    val success = scriptEngine.start(script)
    if (!success) {
        Toast.makeText(this, "启动失败：无障碍服务未连接", Toast.LENGTH_SHORT).show()
    }
}

private fun stopScript() {
    scriptEngine.stop()
}
```

### 6.4 悬浮窗集成

脚本执行时更新 `FloatingCircleManager`：
- `RUNNING` → 显示当前轮次
- `IDLE` → 恢复默认状态
- 点击悬浮窗 → 回到 HomeActivity

复用现有的 `FloatingCircleManager.setRunningState(round)`。

### 6.5 前台通知

执行时通过 `ForegroundService` 发送通知：
- 标题：脚本任务名
- 内容：当前轮次 / 总轮次
- 操作：停止按钮

## 依赖
- 子任务 1-5 全部产出
- `ClawApplication`
- `FloatingCircleManager`
- `ForegroundService`

## 验收标准
- Tab 切换正常
- 脚本列表正确显示（meta.name, meta.description）
- 选择脚本后可启动
- 运行时显示轮次和状态
- 停止按钮可用
- 无障碍服务未开启时弹出引导