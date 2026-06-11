# pull_doc_images.ps1
# 用于一键批量自动生成并拉取文档专属截图。
# 1. 自动执行 Android 真机/模拟器上的 Instrumented Test (DocScreenshotTest)
# 2. 将生成的 26 张图表截图 (13大类 * 2种明暗主题) 自动拉取至本地 images/ 目录下，确保图表外观与 Markdown 文档示例 100% 绝对一致。

$remoteDir = "/sdcard/Android/data/io.github.composechart.showroom/files/chart_screenshots"
$localDir = "images"

Write-Host "🔍 正在检测 ADB 连接设备..." -ForegroundColor Cyan
$devices = adb devices | Select-String -Pattern "device$"

if ($devices.Count -eq 0) {
    Write-Error "❌ 未检测到在线的 Android 设备或模拟器，请检查连接或打开 USB 调试并解锁屏幕！"
    exit 1
}

Write-Host "🚀 开始编译并运行 Android 自动化截图测试 (DocScreenshotTest)..." -ForegroundColor Yellow
# 执行指定的测试类，只生成文档所需的 26 张截图
.\gradlew :showroom:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.aike.composechart.DocScreenshotTest

if ($LASTEXITCODE -ne 0) {
    Write-Error "❌ 测试运行失败，请确认设备是否在线且屏幕已解锁！"
    exit 1
}

Write-Host "✅ 测试运行成功，开始从沙盒目录 $remoteDir 拉取截图..." -ForegroundColor Green

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
        Write-Host "🚚 正在拉取: $cleanFile" -ForegroundColor Yellow
        # adb pull 会覆盖本地的同名旧图
        adb pull "$remoteDir/$cleanFile" "$localDir/$cleanFile"
        $pulledCount++
    }
}

if ($pulledCount -gt 0) {
    Write-Host "🎉 成功拉取 $pulledCount 张文档截图至 $localDir/ 目录！" -ForegroundColor Green
    Write-Host "💡 提示：您现在可以提交这些截图并推送到 GitHub 仓库了。" -ForegroundColor Cyan
} else {
    Write-Host "⚠️ 在 $remoteDir 下未找到符合 doc_*.png 格式的截图文件。" -ForegroundColor Red
}
