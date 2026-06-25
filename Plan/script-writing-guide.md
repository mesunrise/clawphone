# 脚本编写实战指南

## 概述

本文档指导你从零开始编写一个可用的脚本。不需要预先知道屏幕树的数据格式——我们会提供发现和理解屏幕内容的方法。

---

## 第一步：获取屏幕树快照

脚本引擎内置了 `debugDumpScreenTree` 调试功能，可以在日志中直接显示屏幕树内容，无需修改代码。

### 方法 1：启用 debugDumpScreenTree（推荐）

[`ScriptEngine`](app/src/main/java/com/clawp/android/script/ScriptEngine.kt:42) 类有一个 `debugDumpScreenTree` 标志位，启用后每次轮次执行都会将完整的屏幕树输出到日志。

**操作步骤：**

1. 在手机上安装并运行 ClawP v0.2.x
2. 修改 [`ClawApplication.kt`](app/src/main/java/com/clawp/android/ClawApplication.kt) 的 `onCreate()` 方法，添加以下代码：

```kotlin
// 在 ClawApplication.onCreate() 中，scriptEngine 初始化后
scriptEngine.debugDumpScreenTree = true
```

3. 重新编译安装
4. 打开你要自动化的 APP（例如"惊喜红包"）
5. 在 ClawP 的"脚本模式" Tab 中选择脚本并点击"开始执行"
6. 查看 HomeActivity 底部的"执行日志"区域
7. 日志会显示：

```
=== SCREEN TREE ===
android.widget.FrameLayout
  android.widget.LinearLayout
    android.widget.TextView text="惊喜红包" desc=""
    android.widget.TextView text="看视频" desc=""
    android.widget.TextView text="关闭" desc=""
=== END SCREEN TREE ===
```

8. **重要：开发完成后记得将 `debugDumpScreenTree = false` 关闭，避免日志过多**

### 方法 2：使用 Agent 模式（无需编译）

切换到"Agent 模式"，发送消息让 Agent 调用 `get_screen_info` 工具，结果会返回屏幕树文本。这种方式不需要重新编译 APK。

---

## 第二步：保存屏幕树信息到项目

开发时，你需要将从手机上获取的屏幕树快照保存到项目中，以便反复查看和分析。

### 2.1 创建屏幕树存储目录

项目已创建 [`screen-trees/`](screen-trees/) 目录用于保存屏幕树快照。建议的组织方式：

```
screen-trees/
├── README.md                    # 本说明文件
├── example_hongbao_home.txt     # 示例：惊喜红包首页
├── hongbao/                     # 按 APP 分类
│   ├── home_page_20250625.txt   # 首页
│   ├── video_page_20250625.txt  # 视频页
│   └── close_button_20250625.txt # 关闭按钮
└── douyin/                      # 抖音
    ├── publish_page_20250625.txt
    └── comment_page_20250625.txt
```

### 2.2 从手机获取屏幕树并保存到项目

由于开发电脑和手机是分开的，需要通过以下方式将屏幕树数据传输到电脑：

#### 方法 A：手动复制（最简单）

1. 在手机上运行 ClawP，启用 `debugDumpScreenTree = true`
2. 执行脚本，观察 UI 底部的"执行日志"
3. **长按日志区域**，选择"全选"→"复制"
4. 粘贴到文本编辑器，保存为 `screen-trees/<app>/_<page>_<date>.txt`

#### 方法 B：通过 USB ADB 导出（推荐）

如果手机开启了 USB 调试，可以通过 ADB 查看日志：

```bash
# 查看实时日志
adb logcat | findstr "SCREEN TREE"

# 或者保存整个日志到文件
adb logcat > screen-trees/logcat_output.txt
```

#### 方法 C：临时添加文件导出功能

修改 [`ClawAccessibilityService.java`](app/src/main/java/com/clawp/android/service/ClawAccessibilityService.java:275) 的 `getScreenTree()` 方法，将屏幕树同时写入文件：

```java
public String getScreenTree() {
    String tree = buildNodeTree(getRootInActiveWindow());
    
    // 临时调试：写入文件
    try {
        File file = new File(Environment.getExternalStorageDirectory(),
            "clawp_screen_tree_" + System.currentTimeMillis() + ".txt");
        FileWriter writer = new FileWriter(file);
        writer.write(tree);
        writer.close();
        XLog.i("DEBUG", "Screen tree saved to: " + file.getAbsolutePath());
    } catch (Exception e) {
        e.printStackTrace();
    }
    
    return tree;
}
```

写入位置：`/storage/emulated/0/clawp_screen_tree_xxxxx.txt`
然后通过文件管理器或 ADB 下载到电脑：

```bash
adb pull /storage/emulated/0/clawp_screen_tree_*.txt screen-trees/
```

### 2.3 屏幕树文件格式规范

保存的屏幕树文件应包含以下信息：

```
# 示例：惊喜红包 APP 首页屏幕树快照
# 捕获时间：2025-06-25
# APP 包名：com.example.hongbao
# 页面描述：首页，包含签到、看视频、邀请卡片

android.widget.FrameLayout
  android.widget.LinearLayout (orientation=vertical)
    android.widget.Toolbar
      android.widget.TextView text="惊喜红包" desc=""
    ...
```

**必填元数据：**
- `捕获时间`：YYYY-MM-DD 格式
- `APP 包名`：如 `com.example.hongbao`
- `页面描述`：简要说明当前页面内容

### 2.4 使用屏幕树快照编写脚本

1. 打开保存的 `.txt` 文件
2. 查找目标控件的 `text="..."` 或 `desc="..."`
3. 根据查找到的信息编写条件：

```json
// 如果看到 text="看视频"，编写：
{ "type": "text_exists", "text": "看视频" }

// 如果看到 desc="搜索按钮"，编写：
{ "type": "desc_exists", "desc": "搜索按钮" }
```

---

## 第三步：理解屏幕树格式

屏幕树是通过 [`ClawAccessibilityService.buildNodeTree()`](app/src/main/java/com/clawp/android/service/ClawAccessibilityService.java:298) 生成的紧凑文本格式。

### 典型输出示例

```
android.widget.FrameLayout
  android.widget.LinearLayout (orientation=vertical)
    android.widget.TextView text="惊喜红包" desc=""
    android.widget.LinearLayout (clickable=true)
      android.widget.TextView text="邀请有奖" desc=""
      android.widget.TextView text="看视频" desc=""
    android.widget.RelativeLayout
      android.widget.TextView text="关闭" desc=""
    android.widget.TextView text="看完视频" desc=""
    android.widget.TextView text="完成视频要求" desc=""
```

### 关键字段说明

| 字段 | 含义 | 示例 |
|------|------|------|
| `text` | 控件显示的文本内容 | `text="看视频"` |
| `desc` | 控件的 contentDescription | `desc="搜索按钮"` |
| `clickable` | 是否可点击 | `clickable=true` |
| `enabled` | 是否启用 | `enabled=true` |
| 类名 | Android 控件类型 | `android.widget.TextView` |

### 条件求值器如何匹配

[`TextExistsEvaluator`](app/src/main/java/com/clawp/android/script/condition/TextExistsEvaluator.kt) 使用正则表达式 `text="([^"]*)"` 从屏幕树中提取所有 `text="..."` 的值，然后与条件中的 `text` 参数进行匹配。

匹配模式：
- `contains`（默认）：屏幕树中**任意**节点的 text 包含搜索值
- `exact`：完全相等
- `startsWith`：以搜索值开头
- `endsWith`：以搜索值结尾

---

## 第四步：编写脚本

### 3.1 基本结构

```json
{
  "meta": {
    "name": "我的任务",
    "version": "1.0",
    "description": "描述你的任务做什么"
  },
  "config": {
    "targetPackage": "com.example.app",
    "loopCount": 100,
    "roundDelay": { "min": 2, "max": 4 }
  },
  "setup": [
    { "type": "open_app", "package": "com.example.app" },
    { "type": "wait", "durationMs": 2000 }
  ],
  "rules": [
    // 规则列表
  ]
}
```

### 3.2 编写规则

规则是按顺序判断的。**第一条匹配的规则会被执行**。

#### 规则 1：目标 APP 检查（兜底切换）

```json
{
  "name": "不在目标APP时切换",
  "conditions": [
    { "type": "current_app_not", "package": "{{config.targetPackage}}" }
  ],
  "actions": [
    { "type": "open_app", "package": "{{config.targetPackage}}" },
    { "type": "wait", "durationMs": 2000 },
    { "type": "restart_round" }
  ]
}
```

#### 规则 2：条件匹配 + 点击

假设你在屏幕树中看到 `text="看视频"`：

```json
{
  "name": "点击看视频",
  "conditions": [
    { "type": "text_exists", "text": "看视频" }
  ],
  "actions": [
    {
      "type": "click",
      "target": { "by": "text", "value": "看视频", "match": "contains" },
      "humanize": { "offsetPx": 5, "tapDurationMs": 150 }
    },
    { "type": "end_round" }
  ]
}
```

#### 规则 3：多个条件 AND

```json
{
  "name": "邀请有奖 + 看视频都出现时点击",
  "conditions": [
    { "type": "text_exists", "text": "邀请有奖" },
    { "type": "text_exists", "text": "看视频" }
  ],
  "actions": [
    {
      "type": "click",
      "target": { "by": "text", "value": "看视频" },
      "humanize": { "offsetPx": 5 }
    },
    { "type": "end_round" }
  ]
}
```

#### 规则 N：兜底（无条件 = 总是匹配）

```json
{
  "name": "兜底：按返回",
  "conditions": [],
  "actions": [
    { "type": "press_back" },
    { "type": "end_round" }
  ]
}
```

---

## 第五步：测试和迭代

### 4.1 放置脚本文件

将你的 JSON 文件保存到手机的 `assets/scripts/` 目录。在开发阶段，可以直接放在项目的 `app/src/main/assets/scripts/` 目录下，然后重新编译安装。

### 4.2 在 UI 中查看

1. 打开 ClawP → 切换到"脚本模式" Tab
2. 脚本列表应显示你的脚本名称（来自 `meta.name`）
3. 点击选择脚本
4. 点击"开始执行"
5. 观察底部日志

### 4.3 启用屏幕树调试

在 [`ClawApplication.kt`](app/src/main/java/com/clawp/android/ClawApplication.kt:37) 的 `onCreate()` 方法中，找到 `scriptEngine` 初始化代码，添加一行：

```kotlin
// ClawApplication.kt - onCreate() 方法中
scriptEngine = ScriptEngine()
scriptLoader = AssetScriptLoader(this)

// === 添加这一行启用屏幕树调试 ===
scriptEngine.debugDumpScreenTree = true
// ==================================
```

重新编译安装后，每次轮次执行都会在日志中显示：

```
=== SCREEN TREE ===
android.widget.FrameLayout
  android.widget.LinearLayout (orientation=vertical)
    android.widget.TextView text="惊喜红包" desc=""
    android.widget.TextView text="看视频" desc="" clickable=true
    android.widget.TextView text="关闭" desc="" clickable=true
=== END SCREEN TREE ===
```

**重要：开发完成后记得将 `debugDumpScreenTree = false` 关闭，避免日志过多影响性能。**

### 4.4 调试技巧

| 问题 | 解决方法 |
|------|----------|
| 脚本没出现在列表 | 检查文件名是否为 `.json`，文件是否在 `assets/scripts/` |
| 规则不匹配 | 启用屏幕树调试，确认 `text="..."` 是否存在 |
| 点击位置不对 | 增大 `humanize.offsetPx`，或使用 `match: "exact"` 精确定位 |
| 点击后没反应 | 检查目标控件是否 `clickable=true`，可能需要点击父节点 |
| 循环太快/慢 | 调整 `roundDelay.min/max` 或 `wait.durationMs` |

---

## 第六步：常用模式

### 模式 1：点击后等待新页面加载

```json
{
  "name": "点击并等待",
  "conditions": [
    { "type": "text_exists", "text": "开始" }
  ],
  "actions": [
    {
      "type": "click",
      "target": { "by": "text", "value": "开始" }
    },
    { "type": "wait", "durationMs": 3000 },
    { "type": "end_round" }
  ]
}
```

### 模式 2：滑动操作

```json
{
  "name": "向上滑动",
  "conditions": [
    { "type": "text_exists", "text": "视频" }
  ],
  "actions": [
    {
      "type": "swipe",
      "from": { "by": "coordinate", "x": 540, "y": 1800 },
      "to": { "by": "coordinate", "x": 540, "y": 600 },
      "humanize": {
        "durationMs": 500,
        "durationVarMs": 200
      }
    },
    { "type": "end_round" }
  ]
}
```

### 模式 3：输入文本

```json
{
  "name": "输入搜索词",
  "conditions": [
    { "type": "text_exists", "text": "搜索" }
  ],
  "actions": [
    {
      "type": "click",
      "target": { "by": "text", "value": "搜索" }
    },
    { "type": "wait", "durationMs": 500 },
    {
      "type": "click",
      "target": { "by": "text", "value": "搜索" },
      "humanize": { "tapDurationMs": 100 }
    },
    { "type": "end_round" }
  ]
}
```

> 注意：setText 操作需要使用 `input_text` 工具，当前脚本引擎尚未直接支持。可以通过 click 聚焦输入框后，使用系统按键输入。

### 模式 4：多次重试

```json
{
  "name": "重试点击",
  "conditions": [
    { "type": "text_exists", "text": "确认" }
  ],
  "actions": [
    {
      "type": "click",
      "target": { "by": "text", "value": "确认" }
    },
    { "type": "wait", "durationMs": 1000 },
    {
      "type": "click",
      "target": { "by": "text", "value": "确认" }
    },
    { "type": "end_round" }
  ]
}
```

---

## 附录：完整脚本示例

参见 [`app/src/main/assets/scripts/example_hongbao.json`](app/src/main/assets/scripts/example_hongbao.json)。

---

## 附录：条件类型速查

| 类型 | 参数 | 说明 |
|------|------|------|
| `text_exists` | `text`, `match` | 屏幕树中存在匹配 text 的节点 |
| `text_not_exists` | `text`, `match` | 屏幕树中不存在匹配 text 的节点 |
| `desc_exists` | `desc`, `match` | 屏幕树中存在匹配 desc 的节点 |
| `current_app_is` | `package` | 当前前景 APP 包名匹配 |
| `current_app_not` | `package` | 当前前景 APP 包名不匹配 |
| `node_count` | `by`, `value`, `operator`, `count` | 匹配节点数量比较 |

## 附录：动作类型速查

| 类型 | 参数 | 说明 |
|------|------|------|
| `open_app` | `package` | 打开 APP |
| `click` | `target`, `humanize` | 点击控件/坐标 |
| `long_press` | `target`, `humanize` | 长按 |
| `swipe` | `from`, `to`, `humanize` | 滑动 |
| `wait` | `durationMs` | 等待 |
| `press_back` | 无 | 返回键 |
| `press_home` | 无 | Home 键 |
| `restart_round` | 无 | 重做本轮（不消耗轮次） |
| `end_round` | 无 | 结束本轮 |
| `exit_task` | 无 | 退出任务 |
