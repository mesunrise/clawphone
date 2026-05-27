# CLAWP 构建指南

## 自动化构建

### 使用构建脚本（推荐）

```bash
# 构建 Debug 版本
./build.sh debug

# 构建 Release 版本
./build.sh release
```

脚本会自动：
1. 清理旧的构建产物
2. 执行 Gradle 构建
3. 显示生成的 APK 路径
4. 询问是否安装到设备

### 直接使用 Gradle 命令

```bash
# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK
./gradlew assembleRelease

# 清理构建产物
./gradlew clean

# 查看所有任务
./gradlew tasks
```

## 输出位置

- Debug APK: `app/build/outputs/apk/debug/clawp_v0.1.0_*.apk`
- Release APK: `app/build/outputs/apk/release/clawp_v0.1.0_*.apk`

## 安装到设备

```bash
# 安装 Debug 版本
adb install -r app/build/outputs/apk/debug/clawp_v0.1.0_*.apk

# 安装 Release 版本
adb install -r app/build/outputs/apk/release/clawp_v0.1.0_*.apk

# 卸载应用
adb uninstall com.clawp.android
```

## 环境要求

- JDK 17+
- Android SDK (API 30+)
- 设置 `ANDROID_HOME` 环境变量

```bash
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools
```

## Release 签名配置

在 `local.properties` 中配置签名信息（可选）：

```properties
KEYSTORE_FILE=/path/to/keystore.jks
KEYSTORE_PASSWORD=your_keystore_password
KEY_ALIAS=your_key_alias
KEY_PASSWORD=your_key_password
```

如果未配置签名，Release 构建会失败。Debug 版本使用默认签名。

## CI/CD 集成

### GitHub Actions 示例

```yaml
name: Build APK

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Setup Android SDK
        uses: android-actions/setup-android@v2
      
      - name: Build Debug APK
        run: ./gradlew assembleDebug
      
      - name: Upload APK
        uses: actions/upload-artifact@v3
        with:
          name: app-debug
          path: app/build/outputs/apk/debug/*.apk
```

## 常见问题

### 1. `ANDROID_HOME not set`
设置环境变量：`export ANDROID_HOME=/path/to/android/sdk`

### 2. `Permission denied: ./gradlew`
添加执行权限：`chmod +x gradlew`

### 3. SDK 版本不匹配
安装所需的 SDK：`sdkmanager "platforms;android-36"`

### 4. 构建缓存问题
清理并重新构建：`./gradlew clean assembleDebug`



