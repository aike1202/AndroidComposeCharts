package com.aike.composechart

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.composechart.charts.bar.*
import io.github.composechart.charts.bar3d.*
import io.github.composechart.charts.boxplot.*
import io.github.composechart.charts.calendar.*
import io.github.composechart.charts.funnel.*
import io.github.composechart.charts.gauge.*
import io.github.composechart.charts.kline.*
import io.github.composechart.charts.line.*
import io.github.composechart.charts.mixed.*
import io.github.composechart.charts.pie.*
import io.github.composechart.charts.polar.*
import io.github.composechart.charts.radar.*
import io.github.composechart.charts.scatter.*
import io.github.composechart.core.style.ChartStyle
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class DocScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val lightStyle = ChartStyle.Light
    private val darkStyle = ChartStyle.Dark

    private fun captureAndSave(style: ChartStyle, filename: String, chartContent: @Composable (ChartStyle) -> Unit) {
        composeTestRule.setContent {
            chartContent(style)
        }
        composeTestRule.waitForIdle()
        Thread.sleep(1200) // 等待动画与渲染稳定
        val bitmap = composeTestRule.onRoot().captureToImage().asAndroidBitmap()
        
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
    fun generateAllDocScreenshots() {
        val styles = listOf(
            Pair(lightStyle, "_light"),
            Pair(darkStyle, "_dark")
        )

        for ((style, suffix) in styles) {
            // 1. 折线图 (LineChart)
            captureAndSave(style, "doc_line$suffix.png") { s ->
                val lineChartData = LineChartData(
                    xLabels = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日"),
                    series = listOf(
                        LineSeries(
                            name = "邮件营销",
                            points = listOf(
                                LinePoint(0f, 120f), LinePoint(1f, 132f), LinePoint(2f, 101f),
                                LinePoint(3f, 134f), LinePoint(4f, 90f), LinePoint(5f, 230f), LinePoint(6f, 210f)
                            ),
                            color = Color(0xFF5470C6),
                            isSmooth = true,
                            drawArea = true,
                            areaBrush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF5470C6).copy(alpha = 0.4f), Color.Transparent)
                            )
                        ),
                        LineSeries(
                            name = "联盟广告",
                            points = listOf(
                                LinePoint(0f, 220f), LinePoint(1f, 182f), LinePoint(2f, 191f),
                                LinePoint(3f, 234f), LinePoint(4f, 290f), LinePoint(5f, 330f), LinePoint(6f, 310f)
                            ),
                            color = Color(0xFF91CC75),
                            isSmooth = true
                        )
                    )
                )
                LineChart(data = lineChartData, style = s, modifier = Modifier.fillMaxSize())
            }

            // 2. 柱状图 (BarChart)
            captureAndSave(style, "doc_bar$suffix.png") { s ->
                val barChartData = BarChartData(
                    xLabels = listOf("一月", "二月", "三月", "四月", "五月", "六月"),
                    series = listOf(
                        BarSeries(
                            name = "蒸发量",
                            values = listOf(
                                BarValue(2.0f), BarValue(4.9f), BarValue(7.0f),
                                BarValue(23.2f), BarValue(25.6f), BarValue(76.7f)
                            ),
                            color = Color(0xFF5470C6),
                            cornerRadius = CornerRadius(12f, 12f),
                            barWidthRatio = 0.5f
                        ),
                        BarSeries(
                            name = "降水量",
                            values = listOf(
                                BarValue(2.6f), BarValue(5.9f), BarValue(9.0f),
                                BarValue(26.4f), BarValue(28.7f), BarValue(70.7f)
                            ),
                            color = Color(0xFF91CC75),
                            cornerRadius = CornerRadius(12f, 12f),
                            barWidthRatio = 0.5f
                        )
                    )
                )
                BarChart(data = barChartData, style = s, modifier = Modifier.fillMaxSize())
            }

            // 3. 饼图/环形图 (PieChart)
            captureAndSave(style, "doc_pie$suffix.png") { s ->
                val pieChartData = PieChartData(
                    slices = listOf(
                        PieSlice("搜索引擎", 1048f, Color(0xFF5470C6)),
                        PieSlice("直接输入", 735f, Color(0xFF91CC75)),
                        PieSlice("友情链接", 580f, Color(0xFFFAC858)),
                        PieSlice("邮件营销", 484f, Color(0xFFEE6666))
                    )
                )
                val customStyle = s.copy(
                    pieOptions = s.pieOptions.copy(
                        innerRadiusRatio = 0.6f,
                        padAngle = 3f,
                        cornerRadius = 8.dp
                    )
                )
                PieChart(data = pieChartData, style = customStyle, modifier = Modifier.fillMaxSize())
            }

            // 4. 3D 柱状打卡图 (Bar3DChart)
            captureAndSave(style, "doc_bar3d$suffix.png") { s ->
                val bar3DChartData = Bar3DChartData(
                    xAxisLabels = listOf("12a", "1a", "2a", "3a", "4a", "5a", "6a"),
                    yAxisLabels = listOf("周六", "周日"),
                    points = listOf(
                        Bar3DPoint(xIndex = 0, yIndex = 0, zValue = 5f),
                        Bar3DPoint(xIndex = 2, yIndex = 0, zValue = 12f),
                        Bar3DPoint(xIndex = 4, yIndex = 1, zValue = 8f),
                        Bar3DPoint(xIndex = 6, yIndex = 1, zValue = 15f)
                    )
                )
                val options = Bar3DOptions(
                    initialYaw = -45f,
                    initialPitch = 30f,
                    initialZoom = 1.0f,
                    barWidthRatio = 0.5f,
                    visualMapColors = listOf(Color(0xFF73C0DE), Color(0xFF3BA272), Color(0xFFFAC858), Color(0xFFEE6666))
                )
                Bar3DChart(data = bar3DChartData, options = options, style = s, modifier = Modifier.fillMaxSize())
            }

            // 5. 矩阵日历热力图 (CalendarChart)
            captureAndSave(style, "doc_calendar$suffix.png") { s ->
                val days = mutableListOf<CalendarDayData>()
                val start = LocalDate.of(2026, 1, 1)
                for (i in 0..364) {
                    val date = start.plusDays(i.toLong())
                    days.add(
                        CalendarDayData(
                            date = date.toString(),
                            value = (i % 7 * 15).toFloat(),
                            label = if (date.dayOfMonth == 1) "${date.monthValue}月" else null
                        )
                    )
                }
                val calendarData = CalendarChartData(year = 2026, days = days)
                CalendarChart(
                    data = calendarData,
                    options = CalendarOptions(
                        orientation = CalendarOrientation.Horizontal,
                        cellSize = 12.dp,
                        cellGap = 3.dp,
                        visualMapColors = listOf(Color(0xFFEBEDF0), Color(0xFF9BE9A8), Color(0xFF40C463), Color(0xFF216E39))
                    ),
                    style = s,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 6. 仪表盘 (GaugeChart)
            captureAndSave(style, "doc_gauge$suffix.png") { s ->
                val gaugeData = GaugeChartData(value = 68.5f, min = 0f, max = 100f)
                val customOptions = GaugeOptions(
                    startAngle = 180f,
                    sweepAngle = 180f,
                    isProgress = true,
                    showPointer = false,
                    axisLineColors = listOf(
                        0.3f to Color(0xFF91CC75),
                        0.7f to Color(0xFFFAC858),
                        1.0f to Color(0xFFEE6666)
                    )
                )
                GaugeChart(data = gaugeData, options = customOptions, style = s, modifier = Modifier.fillMaxSize())
            }

            // 7. 雷达图 (RadarChart)
            captureAndSave(style, "doc_radar$suffix.png") { s ->
                val radarData = RadarChartData(
                    indicators = listOf(
                        RadarIndicator("销售", 6500f),
                        RadarIndicator("管理", 16000f),
                        RadarIndicator("信息技术", 30000f),
                        RadarIndicator("客服", 38000f),
                        RadarIndicator("研发", 52000f),
                        RadarIndicator("市场", 25000f)
                    ),
                    series = listOf(
                        RadarSeries(
                            name = "预算分配",
                            values = listOf(4300f, 10000f, 28000f, 35000f, 50000f, 19000f),
                            color = Color(0xFF5470C6)
                        ),
                        RadarSeries(
                            name = "实际开销",
                            values = listOf(5000f, 14000f, 28000f, 31000f, 42000f, 21000f),
                            color = Color(0xFF91CC75)
                        )
                    )
                )
                RadarChart(data = radarData, style = s, modifier = Modifier.fillMaxSize())
            }

            // 8. 金融K线图 (KLineChart)
            captureAndSave(style, "doc_kline$suffix.png") { s ->
                val entries = listOf(
                    KLineEntry("2026-06-01", 2320.26f, 2302.6f, 2287.3f, 2362.94f, 120000f),
                    KLineEntry("2026-06-02", 2300f, 2291.3f, 2281.4f, 2311.68f, 85000f),
                    KLineEntry("2026-06-03", 2293.09f, 2278.4f, 2272.24f, 2308.43f, 98000f),
                    KLineEntry("2026-06-04", 2280f, 2314.9f, 2273.4f, 2320.1f, 140000f),
                    KLineEntry("2026-06-05", 2315f, 2350.2f, 2310.5f, 2360f, 180000f)
                )
                val kLineData = KLineChartData(entries = entries)
                KLineChart(data = kLineData, style = s, modifier = Modifier.fillMaxSize())
            }

            // 9. 散点图 (ScatterChart)
            captureAndSave(style, "doc_scatter$suffix.png") { s ->
                val scatterData = ScatterChartData(
                    series = listOf(
                        ScatterSeries(
                            name = "常规数据",
                            points = listOf(
                                ScatterPoint(10f, 8.04f), ScatterPoint(8.0f, 6.95f),
                                ScatterPoint(13f, 7.58f), ScatterPoint(9.0f, 8.81f)
                            ),
                            color = Color(0xFF5470C6),
                            symbolSize = 10.dp
                        ),
                        ScatterSeries(
                            name = "涟漪告警点",
                            points = listOf(
                                ScatterPoint(11f, 8.33f), ScatterPoint(14f, 9.96f)
                            ),
                            color = Color(0xFFEE6666),
                            effectScatter = true,
                            effectOptions = EffectOptions(rippleCount = 3, rippleRadiusRatio = 2.5f)
                        )
                    )
                )
                ScatterChart(data = scatterData, style = s, modifier = Modifier.fillMaxSize())
            }

            // 10. 箱线图 (BoxplotChart)
            captureAndSave(style, "doc_boxplot$suffix.png") { s ->
                val boxplotData = BoxplotChartData(
                    xLabels = listOf("轴承A", "轴承B", "轴承C"),
                    series = listOf(
                        BoxplotSeries(
                            name = "工艺缺陷统计",
                            points = listOf(
                                BoxplotPoint(655f, 850f, 940f, 980f, 1075f),
                                BoxplotPoint(760f, 800f, 845f, 885f, 960f),
                                BoxplotPoint(700f, 750f, 810f, 880f, 940f)
                            ),
                            color = Color(0xFF91CC75),
                            outliers = listOf(
                                BoxplotOutlier(0, 600f),
                                BoxplotOutlier(0, 1150f),
                                BoxplotOutlier(2, 630f)
                            )
                        )
                    )
                )
                BoxplotChart(data = boxplotData, style = s, modifier = Modifier.fillMaxSize())
            }

            // 11. 漏斗图 (FunnelChart)
            captureAndSave(style, "doc_funnel$suffix.png") { s ->
                val funnelData = FunnelChartData(
                    slices = listOf(
                        FunnelSlice("展现", 100f, Color(0xFF5470C6)),
                        FunnelSlice("点击", 80f, Color(0xFF91CC75)),
                        FunnelSlice("访问", 60f, Color(0xFFFAC858)),
                        FunnelSlice("咨询", 40f, Color(0xFFEE6666)),
                        FunnelSlice("订单", 20f, Color(0xFF73C0DE))
                    )
                )
                FunnelChart(data = funnelData, style = s, modifier = Modifier.fillMaxSize())
            }

            // 12. 混合图 (MixedChart)
            captureAndSave(style, "doc_mixed$suffix.png") { s ->
                val mixedData = MixedChartData(
                    xLabels = listOf("2:00", "4:00", "6:00", "8:00", "10:00", "12:00"),
                    barSeries = listOf(
                        BarSeries(
                            name = "降水量",
                            values = listOf(BarValue(2.0f), BarValue(4.9f), BarValue(7.0f), BarValue(23.2f), BarValue(25.6f), BarValue(76.7f)),
                            color = Color(0xFF5470C6),
                            yAxisIndex = 0
                        )
                    ),
                    lineSeries = listOf(
                        LineSeries(
                            name = "平均温度",
                            points = listOf(LinePoint(0f, 2.0f), LinePoint(1f, 2.2f), LinePoint(2f, 3.3f), LinePoint(3f, 4.5f), LinePoint(4f, 6.3f), LinePoint(5f, 10.2f)),
                            color = Color(0xFFFAC858),
                            yAxisIndex = 1
                        )
                    )
                )
                MixedChart(data = mixedData, style = s, modifier = Modifier.fillMaxSize())
            }

            // 13. 极坐标图 (PolarBarChart)
            captureAndSave(style, "doc_polar$suffix.png") { s ->
                val polarData = PolarChartData(
                    xLabels = listOf("分类一", "分类二", "分类三", "分类四"),
                    series = listOf(
                        PolarBarSeries(
                            name = "系列A",
                            values = listOf(80f, 60f, 95f, 45f).map { PolarBarValue(it) },
                            color = Color(0xFF73C0DE)
                        )
                    )
                )
                PolarBarChart(data = polarData, style = s, modifier = Modifier.fillMaxSize())
            }
        }
    }
}
