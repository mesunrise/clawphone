#!/bin/bash

# CLAWP 自动化构建脚本
# 使用方法: ./build.sh [debug|release]

set -e

BUILD_TYPE=${1:-debug}

echo "=========================================="
echo "CLAWP 自动化构建"
echo "构建类型: $BUILD_TYPE"
echo "=========================================="

# 检查 ANDROID_HOME 环境变量
if [ -z "$ANDROID_HOME" ]; then
    echo "错误: ANDROID_HOME 环境变量未设置"
    echo "请设置 Android SDK 路径，例如:"
    echo "export ANDROID_HOME=\$HOME/Android/Sdk"
    exit 1
fi

# 清理旧的构建产物
echo ""
echo "清理旧的构建产物..."
./gradlew clean

# 执行构建
echo ""
echo "开始构建 APK..."
if [ "$BUILD_TYPE" = "release" ]; then
    ./gradlew assembleRelease
    APK_PATH="app/build/outputs/apk/release"
else
    ./gradlew assembleDebug
    APK_PATH="app/build/outputs/apk/debug"
fi

# 显示构建结果
echo ""
echo "=========================================="
echo "构建完成！"
echo "=========================================="
echo "APK 文件位置:"
ls -lh $APK_PATH/*.apk

# 可选：自动安装到连接的设备
echo ""
read -p "是否安装到连接的设备？(y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    APK_FILE=$(ls $APK_PATH/*.apk | head -1)
    echo "安装 $APK_FILE ..."
    adb install -r "$APK_FILE"
    echo "安装完成！"
fi
