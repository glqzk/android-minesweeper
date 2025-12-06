# 字幕遮挡应用 (Zimu Overlay)

一个专为Android折叠屏设备设计的字幕遮挡应用，可以在观看Bilibili视频时遮挡中文字幕。

## 快速开始

### 推送到GitHub

**Windows用户**：双击运行 `push_to_github.bat`

**Linux/Mac用户**：运行 `chmod +x push_to_github.sh && ./push_to_github.sh`

**手动推送**：查看 `GIT_PUSH_INSTRUCTIONS.md` 文件获取详细说明

## 功能特性

- ✅ 悬浮窗遮挡层，支持半透明显示
- ✅ 手动调整遮挡区域位置和大小
- ✅ 透明度调节
- ✅ 折叠屏双屏模式支持
- ✅ 设置自动保存
- ✅ 悬浮控制面板

## 使用方法

1. 安装应用后，首次启动需要授予悬浮窗权限
2. 点击"授予权限"按钮，前往系统设置开启悬浮窗权限
3. 返回应用，点击"启动遮挡"按钮
4. 通过悬浮控制面板调整遮挡层的位置、大小和透明度
5. 打开Bilibili等视频应用，遮挡层会自动显示在视频上方

## 技术栈

- Kotlin
- Android SDK 21+
- Jetpack WindowManager (折叠屏支持)
- Material Design Components

## 构建

### 初始化Gradle Wrapper

首次构建前，需要生成Gradle Wrapper文件：

```bash
# 如果已安装Gradle
gradle wrapper

# 或者使用Android Studio，它会自动生成
```

### 本地构建

```bash
# Windows
gradlew.bat assembleRelease

# Linux/Mac
./gradlew assembleRelease
```

构建产物位于：`app/build/outputs/apk/release/app-release.apk`

### GitHub Actions

项目已配置GitHub Actions自动构建，推送到main/master分支时会自动构建APK。

**注意**：首次使用GitHub Actions构建时，需要确保`gradle/wrapper/gradle-wrapper.jar`文件存在。如果不存在，可以在本地运行`gradle wrapper`命令生成，或让GitHub Actions自动下载。

## 权限说明

- `SYSTEM_ALERT_WINDOW`: 用于显示悬浮窗
- `FOREGROUND_SERVICE`: 用于前台服务运行遮挡层

## 系统要求

- Android 5.0 (API 21) 及以上
- 需要授予悬浮窗权限

## 注意事项

- 首次使用需要在系统设置中手动授予悬浮窗权限
- 遮挡层位置和大小可以通过控制面板或直接拖拽调整
- 设置会自动保存，下次启动时会恢复上次的配置

