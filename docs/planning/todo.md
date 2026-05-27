# CLAWP 开发待办清单

> 更新时间：2026-05-27
> 核心目标：飞书发视频 → 手机抖音极速版自动发布作品（含话题设置）

---

## 已完成

### Phase 0: 项目基础搭建
- ✅ Phase 0.1: 项目初始化（Android 原生 Kotlin）
- ✅ Phase 0.2: 移植核心模块（Agent、LLM、Tool、Channel）
- ✅ Phase 0.3: 最简 UI（HomeActivity + SettingsActivity）
- ✅ Phase 0.4: CI/CD 搭建（GitHub Actions 自动构建 APK）
- ✅ 编译修复：FeiShuFileDownloader、VideoPublishOrchestrator (`4350ee4`)
- ✅ CI 修复：Maven 仓库优先级 (`0551584`)
- ✅ 兼容性：minSdk 降至 26 支持 HarmonyOS (`af007e4`)
- ✅ 版本管理：build_number.txt 自动递增 (`5ea3ac1`)
- ✅ 签名修复：debug 签名配置 (`321171a`)

### Phase 1: 端到端基础验证 ✅
目标：在真机上跑通"飞书收视频 → 本地存储 → 能手动确认视频到位"

- ✅ **1.1 APK 安装验证** (`bf7976c`)
  - OK 在 HarmonyOS 设备上安装 APK 成功
  - OK 应用启动无崩溃
  - OK 修复 LLM 测试按钮闪退问题（协程生命周期管理）

- ✅ **1.2 LLM 连接验证** (`bf7976c`)
  - OK "测试 LLM 连接"按钮验证 GPUGeek API 可用
  - OK DeepSeek-V4-Flash 模型响应正常

- ✅ **1.3 飞书消息接收** (`b6a022d`, `c3704dd`, `935676d`, `5288558`)
  - OK 配置飞书 App ID / Secret
  - OK WebSocket 连接成功
  - OK 收到文本消息，自动回复正常
  - OK 添加版本显示和连接诊断功能
  - OK 添加消息日志显示（最近 20 条）
  - OK 添加通知权限诊断工具
  - OK 修复多监听器支持（VideoPublishCoordinator + SettingsActivity）
  - OK 修复测试消息发送（使用 last messageID）

- ✅ **1.4 飞书视频下载** (用户确认)
  - OK 向飞书机器人发送视频文件
  - OK 视频成功下载到手机
  - OK 系统通知显示正常

---

## Phase 2: 抖音发布自动化（当前进行中）

目标：Agent 自动操作抖音极速版完成发布

### 2.1 UI 研究：手动走通抖音发布流程 ✅
- ✅ 用户手动操作抖音极速版发布一条视频
- ✅ 记录发布流程步骤文档 (`docs/requirements/douyin_publish_flow.md`)
- ✅ 整理 8 步发布流程：打开应用 → 点击"+" → 选择"相册" → 选择视频 → 点击"下一步" → 添加话题 → 添加自主声明 → 点击"发布"

### 2.2 实现 ClawAccessibilityService 核心方法 ✅
- ✅ `hasSystemDialog()` — 检测系统弹窗（权限请求、更新提示等）
- ✅ `dismissSystemDialog()` — 自动关闭系统弹窗
- ✅ `getScreenInfo()` — 获取当前屏幕 UI 树的结构化摘要
- ✅ `takeScreenshot()` — 截屏供 LLM 参考

### 2.3 实现抖音发布 Tool 集合 ✅
- ✅ `ClickElementByTextTool` — 通过文本查找并点击元素
- ✅ `WaitForElementTool` — 等待元素出现
- ✅ `DismissSystemDialogTool` — 检测并关闭系统弹窗
- ✅ 已注册到 ToolRegistry.registerMobileTools()
- ✅ 其他工具已存在：`open_app`, `input_text`, `swipe`, `back`, `home` 等

### 2.4 抖音发布 Agent Prompt 调优 ✅
- ✅ 创建 DouyinPublishPrompts.kt 系统提示词
- ✅ 明确 8 步发布流程指引
- ✅ 添加异常处理策略（系统弹窗、元素未找到、登录态失效等）
- ✅ 添加工具使用规范（文本匹配优先、等待策略、截图时机等）
- ✅ 实现 buildPrompt() 方法动态构建提示词

### 2.5 端到端集成 ✅
- ✅ VideoPublishOrchestrator 已集成 DouyinPublishPrompts
- ✅ VideoPublishCoordinator 已连接飞书消息监听
- ✅ 编译通过，无错误

### 2.6 单视频端到端测试（待测试）
- [ ] 飞书发送 1 个视频 + "发到抖音 #测试"
- [ ] 观察 Agent 自动操作全流程
- [ ] 验证抖音上作品发布成功、话题正确
- [ ] 记录失败案例，优化 Prompt

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
