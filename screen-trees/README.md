# 屏幕树快照目录

## 用途

保存从手机上捕获的屏幕树信息，用于分析和编写自动化脚本。

## 目录结构

```
screen-trees/
├── README.md                  # 本文件
├── .gitkeep                   # 确保空目录被 git 跟踪
├── example_hongbao_home.txt   # 示例：惊喜红包首页
└── <app_name>/                # 按 APP 分类
    ├── <page>_<date>.txt      # 页面快照
    └── ...
```

## 如何捕获屏幕树

### 方法 1：启用 debugDumpScreenTree（推荐）

1. 修改 [`ClawApplication.kt`](../app/src/main/java/com/clawp/android/ClawApplication.kt) 的 `onCreate()` 方法：

```kotlin
scriptEngine = ScriptEngine()
scriptLoader = AssetScriptLoader(this)
scriptEngine.debugDumpScreenTree = true  // 添加这行
```

2. 编译安装 APK
3. 在手机上打开目标 APP
4. 在 ClawP 脚本模式中执行任意脚本
5. 查看底部日志中的 `=== SCREEN TREE ===` 和 `=== END SCREEN TREE ===` 之间的内容
6. 复制内容，保存为 `screen-trees/<app_name>/<page>_<日期>.txt`

### 方法 2：通过 ADB 导出

```bash
# 查看实时日志
adb logcat | findstr "SCREEN TREE"

# 或保存整个日志
adb logcat > screen-trees/logcat_output.txt
```

### 方法 3：临时修改代码写入文件

修改 [`ClawAccessibilityService.java`](../app/src/main/java/com/clawp/android/service/ClawAccessibilityService.java:275) 的 `getScreenTree()` 方法，将屏幕树写入外部存储，然后通过 ADB 拉取：

```bash
adb pull /storage/emulated/0/clawp_screen_tree_*.txt screen-trees/
```

## 文件格式

```
# 示例：<app_name> APP <page_description> 屏幕树快照
# 捕获时间：YYYY-MM-DD
# APP 包名：com.example.app
# 页面描述：简要说明当前页面

android.widget.FrameLayout
  android.widget.LinearLayout
    android.widget.TextView text="目标文本" desc=""
    ...
```

## 使用屏幕树编写脚本

1. 打开保存的 `.txt` 文件
2. 查找目标控件的 `text="..."` 或 `desc="..."`
3. 根据查找到的信息编写条件：

```json
// 如果看到 text="看视频"，编写：
{ "type": "text_exists", "text": "看视频" }

// 如果看到 desc="搜索按钮"，编写：
{ "type": "desc_exists", "desc": "搜索按钮" }
```

## 完整指南

详细使用方法请参考：[`Plan/script-writing-guide.md`](../Plan/script-writing-guide.md)
