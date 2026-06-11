# pull_doc_images.ps1
# 用于一键批量拉取手机上已生成的文档和Showcase截图。
# 使用说明：
# 1. 确保手机已通过 ADB 连接，且已在 Showroom App 主页中通过“截图中心”手动一键生成了截图。
# 2. 运行此脚本，将手机中 /sdcard/Pictures/ComposeChart/ 的截图拉取至本地 docs_screenshots/ 目录下。

$remoteDir = "/sdcard/Pictures/ComposeChart"
$localDir = "docs_screenshots"

Write-Host "🔍 正在检测 ADB 连接设备..." -ForegroundColor Cyan
$devices = adb devices | Select-String -Pattern "\s+device"

if (@($devices).Count -eq 0) {
    Write-Error "❌ 未检测到在线的 Android 设备或模拟器，请确认已连接且开启 USB 调试！"
    exit 1
}

Write-Host "✅ 检测到可用设备，开始从手机相册目录 $remoteDir 拉取截图..." -ForegroundColor Green

# 创建本地目录（如果不存在的话）
if (!(Test-Path $localDir)) {
    New-Item -ItemType Directory -Path $localDir | Out-Null
}

Write-Host "🧹 正在清理本地的旧截图..." -ForegroundColor Cyan
Remove-Item -Path "$localDir\*" -Force -ErrorAction SilentlyContinue

# 直接通过一次 adb pull 拉取目录下所有文件，速度更快且稳定
Write-Host "🚚 正在拉取手机端目录下的所有截图..." -ForegroundColor Yellow
adb pull "$remoteDir/." "$localDir"

$pulledCount = (Get-ChildItem -Path $localDir -Filter *.png).Count
if ($pulledCount -gt 0) {
    Write-Host "🎉 成功拉取 $pulledCount 张图表截图至 $localDir/ 目录！" -ForegroundColor Green
    Write-Host "💡 提示：您现在可以提交这些截图并推送到 GitHub 仓库了。" -ForegroundColor Cyan
} else {
    Write-Host "⚠️ 拉取结果为空，未在 $remoteDir 下找到符合条件的截图文件，请先在手机 App 内运行自动截图。" -ForegroundColor Red
}
