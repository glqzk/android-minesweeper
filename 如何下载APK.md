# 如何下载GitHub Actions构建的APK

## 方法1: 从Actions页面下载（推荐）

1. 访问你的GitHub仓库：https://github.com/glqzk/android-minesweeper
2. 点击顶部的 **"Actions"** 标签页
3. 在左侧工作流列表中找到 **"Build APK"** 工作流
4. 点击最新的构建运行（应该显示绿色的 ✓ 标记，表示成功）
5. 在构建详情页面，向下滚动找到 **"Artifacts"** 部分
6. 点击 **"app-release"** 下载APK文件

## 方法2: 直接访问Artifacts

1. 访问：https://github.com/glqzk/android-minesweeper/actions
2. 找到最新的成功构建
3. 点击构建标题进入详情
4. 在页面底部找到 **"Artifacts"** 区域
5. 下载 **"app-release"** 文件

## 如果找不到Artifacts

如果构建成功但看不到Artifacts，可能的原因：

1. **APK文件路径不正确** - 检查构建日志确认APK是否生成
2. **Artifact上传失败** - 查看构建日志中的"Upload APK"步骤
3. **浏览器缓存** - 尝试刷新页面或使用无痕模式

## 检查构建日志

如果找不到APK，可以：

1. 进入Actions页面
2. 点击最新的构建
3. 展开 **"Build with Gradle"** 步骤
4. 查看日志，搜索 "app-release.apk" 确认文件是否生成
5. 展开 **"Upload APK"** 步骤，查看是否有错误

## 备用方案：本地构建

如果GitHub Actions有问题，可以在本地构建：

```bash
# Windows
gradlew.bat assembleRelease

# Linux/Mac
./gradlew assembleRelease
```

构建完成后，APK文件位于：
`app/build/outputs/apk/release/app-release.apk`

