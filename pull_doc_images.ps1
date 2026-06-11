# pull_doc_images.ps1
# 用于一键拉取在 Android 模拟器/真机上生成的文档专用截图，并存入 images/ 目录下。

$remoteDir = "/sdcard/Pictures/ComposeChart"
$localDir = "images"

Write-Host "🔍 正在检测 ADB 连接设备..." -ForegroundColor Cyan
$devices = adb devices | Select-String -Pattern "device$"

if ($devices.Count -eq 0) {
    Write-Error "❌ 未检测到在线的 Android 设备或模拟器，请检查连接或打开 USB 调试！"
    exit 1
}

Write-Host "✅ 已连接设备，正在从 $remoteDir 拉取最新的文档图表截图..." -ForegroundColor Green

# 创建本地 images 目录（如果不存在的话）
if (!(Test-Path $localDir)) {
    New-Item -ItemType Directory -Path $localDir | Out-Null
}

# 列出远程目录下的文件并拉取
$files = adb shell ls $remoteDir
$pulledCount = 0

foreach ($file in $files) {
    $cleanFile = $file.Trim()
    if ($cleanFile -like "doc_*.png") {
        Write-Host "🚀 正在拉取: $cleanFile" -ForegroundColor Yellow
        adb pull "$remoteDir/$cleanFile" "$localDir/$cleanFile"
        $pulledCount++
    }
}

if ($pulledCount -gt 0) {
    Write-Host "🎉 成功拉取 $pulledCount 张文档截图至 $localDir/ 目录！" -ForegroundColor Green
    Write-Host "💡 提示：您现在可以提交这些截图并推送到 GitHub 仓库了。" -ForegroundColor Cyan
} else {
    Write-Host "⚠️ 在 $remoteDir 下未找到符合 doc_*.png 格式的截图文件。" -ForegroundColor Red
    Write-Host "💡 请先在手机 App 中运行“文档截图生成器”，点击“一键生成全部 13 个图表截图”后再试。" -ForegroundColor Cyan
}
