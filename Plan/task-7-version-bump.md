# 子任务 7：版本号升级到 v0.2.1

## 目标
修改构建配置，将版本号从 v0.1.x 升级到 v0.2.x。

## 输出文件

```
build.gradle.kts          # 修改
build_number.txt          # 修改
app/build.gradle.kts      # 修改
```

## 具体内容

### 7.1 版本号体系

- 当前：`clawp_v0.1.33_20260528_173913.apk`
- 目标：`clawp_v0.2.1_YYYYMMDD_HHmmss.apk`

### 7.2 修改位置

- `build.gradle.kts` / `app/build.gradle.kts`：找到 `versionName` 定义，改为 `0.2.`
- `build_number.txt`：重置 build number（可选，保留递增）
- 检查 `getVersionGit()` / `getBuildNumber()` / `getDateTime()` 等辅助函数

## 验收标准
- 构建输出 APK 文件名格式为 `clawp_v0.2.x_YYYYMMDD_HHmmss.apk`
- versionName 包含 `0.2.` 前缀