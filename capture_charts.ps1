# ComposeChart 一键式自动化真实截图捕获拉取脚本

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "   ComposeChart 真实渲染效果图自动捕获与拉取脚本" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "【前置条件说明】" -ForegroundColor Yellow
Write-Host "1. 请确保您的电脑已连上 Android 物理测试真机，或者已拉起 Android Emulator 模拟器。"
Write-Host "2. 请确保可以使用 'adb devices' 识别到在线设备。"
Write-Host ""

# 1. 检测 ADB 连接设备
$devices = adb devices | Out-String
if ($devices -match "(?m)^[^\s]+(?=\s+device\b)") {
    Write-Host "[✓] 成功检测到在线 Android 测试设备，准备运行截图测试..." -ForegroundColor Green
} else {
    Write-Host "[✗] 错误：未检测到任何在线 Android 设备，请开启 USB 调试或启动模拟器后重试。" -ForegroundColor Red
    Exit
}

# 2. 编译并运行 Android Instrumented Screenshot Test
Write-Host ""
Write-Host "==========================================================" -ForegroundColor Gray
Write-Host "1. 正在启动 connectedAndroidTest 进行 Canvas 真实截图渲染..." -ForegroundColor Yellow
Write-Host "==========================================================" -ForegroundColor Gray
Write-Host ""

# 执行 connectedAndroidTest 命令
.\gradlew :showroom:connectedAndroidTest

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "[✗] 编译或测试执行失败。请检查设备连接是否稳固，或者运行环境配置。" -ForegroundColor Red
    Exit
}

Write-Host ""
Write-Host "[✓] 自动化截图测试顺利执行完毕！正在将截图拉取到项目目录..." -ForegroundColor Green

# 3. 将截图拉取到本地的 .docs/images/ 目录下
Write-Host ""
Write-Host "==========================================================" -ForegroundColor Gray
Write-Host "2. 正在通过 ADB 拉取设备外置沙盒中的截图图片..." -ForegroundColor Yellow
Write-Host "==========================================================" -ForegroundColor Gray
Write-Host ""

# 确保本地 .docs/images 目录存在
if (!(Test-Path -Path ".docs\images")) {
    New-Item -ItemType Directory -Path ".docs\images" -Force | Out-Null
}

# 从设备外置沙盒拉取图片 (com.aike.composechart 包名下的沙盒文件夹)
adb pull /sdcard/Android/data/com.aike.composechart/files/chart_screenshots/. .docs/images/

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "==========================================================" -ForegroundColor Green
    Write-Host "[✓] 一键截图拉取成功！" -ForegroundColor Green
    Write-Host "真实图表效果图已全部更新至项目的 '.docs/images/' 目录下。" -ForegroundColor Green
    Write-Host "您可以立即打开 README.md 或提交代码到 Git 查看高颜值真实排版！" -ForegroundColor Green
    Write-Host "==========================================================" -ForegroundColor Green
} else {
    Write-Host "[✗] ADB 拉取截图失败，请确认 App 是否成功在设备上创建了截图文件。" -ForegroundColor Red
}
