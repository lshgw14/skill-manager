@echo off

rem 构建脚本
rem 构建跨平台可执行文件

echo 开始构建 InitGitSsh Go版本...
echo ===============================

rem 构建Windows版本
echo 构建Windows版本...
go build -o bin\init-git-ssh-windows.exe main.go
if errorlevel 1 (
    echo 构建Windows版本失败
    exit /b 1
)

rem 构建Linux版本
echo 构建Linux版本...
go build -o bin\init-git-ssh-linux -ldflags="-s -w" main.go
if errorlevel 1 (
    echo 构建Linux版本失败
    exit /b 1
)

rem 构建macOS版本
echo 构建macOS版本...
go build -o bin\init-git-ssh-darwin -ldflags="-s -w" main.go
if errorlevel 1 (
    echo 构建macOS版本失败
    exit /b 1
)

echo ===============================
echo 构建完成！
echo 可执行文件位于 bin\ 目录
