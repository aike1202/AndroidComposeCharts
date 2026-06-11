package com.aike.composechart

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.composechart.core.style.ChartStyle
import io.github.composechart.showroom.screens.DemoBar3DPunchCard
import io.github.composechart.showroom.screens.DemoRoundedDoughnut
import io.github.composechart.showroom.screens.DemoLunarCalendar
import io.github.composechart.showroom.screens.DemoGradeGauge
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * 自动化截图测试类，运行在 Android 真机或模拟器上。
 * 依次渲染 3D 柱体打卡图、圆角环形图、进度仪表盘以及自定义日历热力图，
 * 并将真实的 Canvas 渲染图像以高品质 PNG 形式导出至设备外置存储中。
 */
@RunWith(AndroidJUnit4::class)
class ChartScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // 统一样式定义
    private val darkStyle = ChartStyle.Default.copy(
        backgroundColor = Color(0xFF1B1B1D) // 深色模式背景
    )
    
    private val lightStyle = ChartStyle.Default.copy(
        backgroundColor = Color(0xFFF8FAFC) // 浅白底色背景
    )

    /**
     * 辅助保存函数：将截取的 Bitmap 保存到 App 外置缓存沙盒中，避免运行时申请存储写权限
     */
    private fun saveScreenshot(bitmap: Bitmap, filename: String) {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val screenshotsDir = File(appContext.getExternalFilesDir(null), "chart_screenshots")
        if (!screenshotsDir.exists()) {
            screenshotsDir.mkdirs()
        }
        val targetFile = File(screenshotsDir, filename)
        FileOutputStream(targetFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    @Test
    fun captureAllCharts() {
        // 1. 截取 3D 柱体打卡图 (Bar3D - Punch Card)
        composeTestRule.setContent {
            DemoBar3DPunchCard(style = darkStyle)
        }
        composeTestRule.waitForIdle()
        Thread.sleep(1500) // 等待动画稳定
        val bar3dBitmap = composeTestRule.onRoot().captureToImage().asAndroidBitmap()
        saveScreenshot(bar3dBitmap, "bar3d_punch_card.png")

        // 2. 截取圆角环形图 (Pie - Rounded Doughnut)
        composeTestRule.setContent {
            DemoRoundedDoughnut(style = lightStyle)
        }
        composeTestRule.waitForIdle()
        Thread.sleep(1500)
        val pieBitmap = composeTestRule.onRoot().captureToImage().asAndroidBitmap()
        saveScreenshot(pieBitmap, "pie_doughnut.png")

        // 3. 截取等级进度仪表盘 (Gauge - Grade Gauge)
        composeTestRule.setContent {
            DemoGradeGauge(style = lightStyle)
        }
        composeTestRule.waitForIdle()
        Thread.sleep(1500)
        val gaugeBitmap = composeTestRule.onRoot().captureToImage().asAndroidBitmap()
        saveScreenshot(gaugeBitmap, "gauge_chart.png")

        // 4. 截取农历自定义日历热力图 (Calendar - Lunar Contribution Grid)
        composeTestRule.setContent {
            DemoLunarCalendar(style = lightStyle)
        }
        composeTestRule.waitForIdle()
        Thread.sleep(1500)
        val calendarBitmap = composeTestRule.onRoot().captureToImage().asAndroidBitmap()
        saveScreenshot(calendarBitmap, "calendar_lunar.png")
    }
}
