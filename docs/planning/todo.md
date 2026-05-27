# CLAWP 开发待办清单

> 更新时间：2026-05-27
> 核心目标：飞书发视频 → 手机抖音极速版自动发布作品（含话题设置）

---

## 已完成

- [x] Phase 0.1: 项目初始化（Android 原生 Kotlin）
- [x] Phase 0.2: 移植核心模块（Agent、LLM、Tool、Channel）
- [x] Phase 0.3: 最简 UI（HomeActivity + SettingsActivity）
- [x] Phase 0.4: CI/CD 搭建（GitHub Actions 自动构建 APK）
- [x] 编译修复：FeiShuFileDownloader、VideoPublishOrchestrator (`4350ee4`)
- [x] CI 修复：Maven 仓库优先级 (`0551584`)
- [x] 兼容性：minSdk 降至 26 支持 HarmonyOS (`af007e4`)
- [x] 版本管理：build_number.txt 自动递增 (`5ea3ac1`)
- [x] 签名修复：debug 签名配置 (`321171a`)

---

## 当前：未提交的本地修改（需先处理）

- [ ] **提交设置页 LLM 测试功能**
  - SettingsActivity 新增"测试 LLM 连接"按钮
  - KVUtils 添加 LLM/飞书默认配置值
  - ⚠️ 安全问题：API key 和飞书 secret 硬编码在 KVUtils.kt 中
  - 建议：将默认密钥移到 `local.properties` 或 `BuildConfig`，不进 git

---

## Phase 1: 端到端基础验证（优先级最高）

目标：在真机上跑通"飞书收视频 → 本地存储 → 能手动确认视频到位"

- [ ] **1.1 APK 安装验证**
  - 在 HarmonyOS 设备上安装最新 APK
  - 如果失败，添加 armeabi-v7a 32 位架构支持
  - 验证：应用启动无崩溃、无障碍服务可开启

- [ ] **1.2 LLM 连接验证**
  - 使用"测试 LLM 连接"按钮验证 GPUGeek API 可用
  - 确认 DeepSeek-V4-Flash 模型响应正常
  - 如果不通，检查网络 / API key / base URL

- [ ] **1.3 飞书消息接收**
  - 配置飞书 App ID / Secret
  - 向飞书机器人发送文本消息，确认 WebSocket 连接成功
  - 查看 logcat 日志确认消息到达

- [ ] **1.4 飞书视频下载**
  - 向飞书机器人发送 1 个视频文件
  - 确认视频下载到 `/Movies/clawp/` 目录
  - 确认视频在系统媒体库中可见（抖音能选到）

---

## Phase 2: 抖音发布自动化（核心功能）

目标：Agent 自动操作抖音极速版完成发布

- [ ] **2.1 UI 研究：手动走通抖音发布流程**
  - 用户配合：手动操作抖音极速版发布一条视频
  - 截图或录屏每个步骤的界面
  - 记录 UI 元素的 resource-id、text、content-description
  - 可用 `adb shell uiautomator dump` 导出 UI 树

- [ ] **2.2 实现 ClawAccessibilityService 核心方法**
  - `hasSystemDialog()` — 检测并处理系统弹窗（权限请求、更新提示等）
  - `getScreenInfo()` — 获取当前屏幕 UI 树的结构化摘要
  - `takeScreenshot()` — 截屏供 LLM 参考

- [ ] **2.3 实现抖音发布 Tool 集合**
  - `open_app("抖音极速版")` — 打开应用
  - `tap_element(selector)` — 点击指定元素
  - `input_text(text)` — 输入文本（话题关键词等）
  - `swipe(direction)` — 滑动（选择视频时翻页等）
  - `wait_for_element(selector, timeout)` — 等待元素出现

- [ ] **2.4 抖音发布 Agent Prompt 调优**
  - 完善 DouyinPublishPrompts.kt 中的系统提示词
  - 明确步骤：打开抖音 → 点击"+" → 选视频 → 加话题 → 发布
  - 添加常见异常处理指引（广告弹窗、登录态失效等）

- [ ] **2.5 单视频端到端测试**
  - 飞书发送 1 个视频 + "发到抖音 #测试"
  - 观察 Agent 自动操作全流程
  - 验证抖音上作品发布成功、话题正确

---

## Phase 3: 多视频与任务编排

目标：支持批量发布、失败重试

- [ ] **3.1 多视频批量发布**
  - 飞书发送 2-3 个视频 + 指令
  - VideoPublishOrchestrator 按顺序逐个发布
  - 每个视频独立一条作品

- [ ] **3.2 发布结果上报**
  - 通过飞书回复发布结果（成功/失败/截图）
  - PublishProgressReporter 实时汇报进度

- [ ] **3.3 失败重试与错误恢复**
  - Agent 死循环检测（滑动窗口指纹）
  - 单视频发布失败不影响后续视频
  - 系统弹窗自动处理后继续任务

---

## Phase 4: 打磨与加固

- [ ] **4.1 稳定性**
  - 前台服务保活优化
  - 飞书 WebSocket 断线重连
  - Agent 超时与最大轮次限制

- [ ] **4.2 用户体验**
  - HomeActivity 显示任务队列和执行状态
  - 悬浮球显示当前 Agent 状态
  - 通知栏显示发布进度

- [ ] **4.3 安全**
  - 敏感配置加密存储
  - API key 不硬编码到代码中
  - 操作审计日志

---

## 注意事项

- 每次推送代码后 GitHub Actions 自动构建 APK
- 问题排查需要：错误截图 + `adb logcat | grep CLAWP` + 操作步骤
- 当前 APK 要求：Android 8.0+ (minSdk 26)、arm64-v8a / x86_64
- 测试设备：HarmonyOS（兼容 Android 8.0）
