# 待确定问题

## 问题 1：脚本选择方式

**背景：** 需求提到"需要支持多个任务，每一个任务对应一个脚本。多任务不可并行执行，仅可选择一个任务"。

**问题：** 用户如何选择要执行的任务脚本？

选项：
- A) 在 APP 主界面（HomeActivity）中展示脚本列表，用户手动点击选择
- B) 通过飞书机器人发送指令选择（如 "@bot 执行脚本 惊喜红包"）
- C) A + B 都支持

**建议：** 选项 A（UI 选择），因为需求明确 V0.2.1 不依赖服务端，脚本打包在 APK 内。UI 选择最直接。

---

## 问题 2：多条件逻辑

**背景：** 需求描述中写 "判断当前屏幕的内容是否有控件文字包括：邀请有奖，并且是否有控件文字包括：看视频，领金币"，这里的"并且"是 AND 逻辑。

**问题：** 确认条件之间使用 AND 逻辑：规则内所有 condition 必须同时满足才触发该规则的 actions？

**建议：** AND 逻辑。如需 OR，写成多条规则即可（规则之间本就是 OR）。

---

## 问题 3：`current_app` 获取方式

**背景：** 需求中需要"判断当前屏幕所在APP名称是哪个"。当前 `ClawAccessibilityService` 的 `getRootInActiveWindow()` 返回的 `AccessibilityNodeInfo` 上有 `getPackageName()` 方法。

**问题：** `current_app_is` / `current_app_not` 条件依赖 accessibility 节点树中的 packageName。但某些场景下（如系统对话框弹出），packageName 可能是 `com.android.systemui`。是否需要在 `ClawAccessibilityService` 中单独暴露一个 `getForegroundPackage()` 方法，通过 `UsageStatsManager` 或 `ActivityManager` 获取更准确的前景应用？

**建议：** V0.2.1 先用节点树的 `packageName`。后续可考虑 `UsageStatsManager` 增强。

---

## 问题 4：规则匹配后是否继续

**背景：** 当前设计是 "第一条匹配规则执行完即结束当前轮次"。但需求中的"上一条判断不满足，则进入下一条判断"暗示了一种"继续判断"的语义。

**问题：** 确认行为：
- A) 第一条匹配 → 执行 actions → 结束本轮（我的设计）
- B) 第一条匹配 → 执行 actions 中的非 `end_round` / `restart_round` / `exit_task` 动作 → 继续判断下一条规则

**建议：** 选项 A（匹配即结束本轮）。这样逻辑更清晰，且可以通过 `restart_round` 动作在 actions 末尾指定"继续判断"。见脚本示例中规则 1 的 `restart_round`。

---

## 问题 5：脚本文件存放位置和命名

**背景：** 需求要求"脚本打包到 apk"。

**问题：** 
- 脚本存放路径：`app/src/main/assets/scripts/*.json`？
- 脚本命名规则：文件名即为任务名？如 `hongbao.json` → UI 显示 "hongbao"？
- 还是通过脚本内 `meta.name` 字段作为展示名？

**建议：** 存放于 `assets/scripts/`，通过脚本内 `meta.name` 字段作为 UI 显示名，文件名仅作标识符。

---

## 问题 6：任务失败/异常处理

**背景：** 长时间运行（1000 轮次）中可能遇到各种异常：AccessibilityService 断开、目标 APP 崩溃、屏幕锁屏等。

**问题：** 
- 单个 action 失败时（如找不到目标控件）如何处理？
  - A) 跳过当前 action，继续执行后续 actions
  - B) 终止当前轮次，进入下一轮
  - C) 终止整个任务
- AccessibilityService 意外断开时如何恢复？

**建议：** 单个 action 失败时终止当前轮次（不执行后续 actions），等待 roundDelay 后进入下一轮。AccessibilityService 断开则终止任务并通知用户。

---

## 问题 7：脚本执行时的 UI 状态

**背景：** 脚本执行是长时间操作。

**问题：** 
- 是否需要前台通知（Notification）展示执行状态？
- 是否需要悬浮窗显示当前轮次和进度？
- 用户如何取消正在执行的任务？

**建议：** 复用现有的 `FloatingCircleManager` 悬浮窗显示轮次，复用 `ForegroundService` 保持进程存活性。HomeActivity 中提供"停止任务"按钮。通过 Notification 展示执行状态。

---

## 问题 8：调试/日志支持

**背景：** 需求是"现阶段调试"。

**问题：** 
- 是否需要在 APP UI 中展示脚本执行日志（实时输出当前轮次、匹配的规则、执行的动作）？
- 是否需要将日志发送到飞书？
- 是否需要"单步执行"调试模式？

**建议：** V0.2.1 先实现：HomeActivity 中显示运行状态（当前轮次/总轮次、最近日志），XLog 记录详细日志。飞书推送在后续版本支持。

---

## 问题 9：action 失败时的 target 定位策略

**背景：** 使用 `by: "text"` 定位控件时，可能匹配到多个节点或匹配不到。

**问题：** 多个匹配时：
- A) 用 `index` 参数选择（当前设计）
- B) 自动选择第一个可点击的（更智能）
- C) 自动选择 bounds 面积最大的（最可能的）

**建议：** 支持 `index` 参数（默认 0）+ `match` 模式 + 自动优先选择可点击节点。三个策略组合使用。

---

## 问题 10：是否需要支持 Android 权限动态检查

**背景：** 脚本执行前需要确保无障碍服务已开启、悬浮窗权限已授予。

**问题：** 脚本引擎启动前是否自动检查并引导用户授权？还是假定用户已配置好？

**建议：** 启动前检查，如未配置则弹出引导弹窗。

---

## 问题 11：脚本是否可以动态更新

**背景：** 需求提到"后续版本可能需要支持从服务器获取任务"。

**问题：** V0.2.1 的脚本架构是否需要预留远程脚本加载的能力？

**建议：** 引擎设计时预留 `ScriptLoader` 接口，V0.2.1 实现 `AssetScriptLoader`（从 assets 加载），后续实现 `RemoteScriptLoader`（从服务器加载）。脚本格式保持一致。
