# Git推送说明

## 将项目推送到GitHub仓库

### 步骤1: 初始化Git仓库（如果还没有）

```bash
git init
```

### 步骤2: 添加所有文件

```bash
git add .
```

### 步骤3: 创建提交

```bash
git commit -m "Initial commit: Android字幕遮挡应用"
```

### 步骤4: 添加远程仓库

```bash
git remote add origin https://github.com/glqzk/android-minesweeper.git
```

如果远程仓库已存在，使用：

```bash
git remote set-url origin https://github.com/glqzk/android-minesweeper.git
```

### 步骤5: 强制推送到main分支（覆盖现有内容）

```bash
git branch -M main
git push -f origin main
```

**注意**: `-f` 参数会强制推送并覆盖远程仓库的所有内容。

### 完整命令序列（一次性执行）

```bash
git init
git add .
git commit -m "Initial commit: Android字幕遮挡应用"
git remote add origin https://github.com/glqzk/android-minesweeper.git
git branch -M main
git push -f origin main
```

如果远程仓库已存在，使用：

```bash
git init
git add .
git commit -m "Initial commit: Android字幕遮挡应用"
git remote set-url origin https://github.com/glqzk/android-minesweeper.git
git branch -M main
git push -f origin main
```

## 验证

推送成功后，访问 https://github.com/glqzk/android-minesweeper 查看代码。

GitHub Actions会自动触发构建，可以在仓库的"Actions"标签页查看构建状态。

