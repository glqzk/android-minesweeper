@echo off
echo 正在初始化Git仓库并推送到GitHub...
echo.

REM 初始化Git仓库
git init
if errorlevel 1 (
    echo 错误: Git未安装或不在PATH中
    echo 请先安装Git: https://git-scm.com/download/win
    pause
    exit /b 1
)

REM 添加所有文件
echo 添加文件...
git add .

REM 创建提交
echo 创建提交...
git commit -m "Initial commit: Android字幕遮挡应用"

REM 检查远程仓库是否存在
git remote get-url origin >nul 2>&1
if errorlevel 1 (
    echo 添加远程仓库...
    git remote add origin https://github.com/glqzk/android-minesweeper.git
) else (
    echo 更新远程仓库URL...
    git remote set-url origin https://github.com/glqzk/android-minesweeper.git
)

REM 设置main分支
git branch -M main

REM 强制推送（覆盖现有内容）
echo.
echo 警告: 即将强制推送到GitHub，这将覆盖远程仓库的所有内容！
echo 按任意键继续，或按Ctrl+C取消...
pause >nul

echo 正在推送到GitHub...
git push -f origin main

if errorlevel 1 (
    echo.
    echo 推送失败！可能的原因：
    echo 1. 未配置GitHub认证
    echo 2. 没有仓库的写入权限
    echo.
    echo 请确保：
    echo - 已登录GitHub账户
    echo - 有glqzk/android-minesweeper仓库的写入权限
    echo - 已配置Git凭据（用户名和密码/Personal Access Token）
    pause
    exit /b 1
) else (
    echo.
    echo 推送成功！
    echo 访问 https://github.com/glqzk/android-minesweeper 查看代码
    echo GitHub Actions将自动开始构建APK
    pause
)




