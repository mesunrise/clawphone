# CLAWP

飞书视频 → 抖音极速版自动发布工具

## 功能

- 通过飞书接收视频和发布指令
- 使用 AI Agent（Claude）自动操作抖音极速版
- 支持批量发布、话题设置

## 快速开始

### 1. 下载 APK

从 [GitHub Actions](../../actions) 下载最新构建的 APK，或查看 [Releases](../../releases)。

### 2. 安装配置

1. 安装 APK 到 Android 手机（需 Android 11+）
2. 打开 CLAWP，进入设置页
3. 配置 LLM：
   - **API Key**: 你的 GPUGeek API Key
   - **Base URL**: `https://api.gpugeek.com/v1`
   - **Model Name**: `Vendor2/Claude-4.5-Sonnet`
4. 配置飞书（可选）：
   - **App ID**: 飞书应用的 App ID
   - **App Secret**: 飞书应用的 App Secret
5. 返回主页，点击"开启无障碍服务"，在系统设置中启用 CLAWP

### 3. 使用

通过飞书发送视频文件和文本指令，例如：
```
把这几个视频发到抖音，话题加上 #美食 #日常
```

CLAWP 会自动：
1. 下载视频到本地
2. 逐个打开抖音极速版
3. 选择视频、添加话题
4. 发布作品
5. 回报结果到飞书

## 开发

### 构建

```bash
# 使用构建脚本
./build.sh debug

# 或直接用 Gradle
./gradlew assembleDebug
```

详见 [BUILD.md](BUILD.md)

### 架构

- **Agent 系统**: 基于 LangChain4j + Claude API
- **无障碍服务**: Android Accessibility Service
- **工具系统**: 12 个工具（tap, swipe, input, get_screen_info 等）
- **通道系统**: 飞书 WebSocket 实时消息

详见 [规划文档](docs/)

## 许可

MIT License

## 致谢

基于 [ApkClaw](research/ApkClaw/) 项目改造
