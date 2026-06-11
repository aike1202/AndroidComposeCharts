package io.github.composechart.showroom.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import io.github.composechart.showroom.ScreenshotHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 截图控制器，用于从外部触发 Composable 截图
 */
class ScreenshotController {
    var onCapture: ((String) -> Unit)? = null
    fun capture(fileName: String) {
        onCapture?.invoke(fileName)
    }
}

/**
 * 图表截图容器，利用 Picture 绘图录制，只截取 Box 内的 Composable 内容
 */
@Composable
fun ChartScreenshotContainer(
    fileName: String,
    controller: ScreenshotController,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val picture = remember { android.graphics.Picture() }
    val context = LocalContext.current
    
    DisposableEffect(controller, fileName) {
        controller.onCapture = { name ->
            val width = picture.width
            val height = picture.height
            if (width > 0 && height > 0) {
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                picture.draw(canvas)
                
                CoroutineScope(Dispatchers.IO).launch {
                    val (success, path) = ScreenshotHelper.saveBitmap(context, bitmap, name)
                    withContext(Dispatchers.Main) {
                        if (success) {
                            Toast.makeText(context, "图表已截图并保存: $path", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "保存失败: $path", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                Toast.makeText(context, "图表尚未准备好截图", Toast.LENGTH_SHORT).show()
            }
        }
        onDispose {
            if (controller.onCapture != null) {
                controller.onCapture = null
            }
        }
    }

    Box(
        modifier = modifier
            .drawWithCache {
                val width = size.width.toInt()
                val height = size.height.toInt()
                onDrawWithContent {
                    val pictureCanvas = androidx.compose.ui.graphics.Canvas(
                        picture.beginRecording(width, height)
                    )
                    drawIntoCanvas { canvas ->
                        val drawContext = drawContext
                        val originalCanvas = drawContext.canvas
                        drawContext.canvas = pictureCanvas
                        this@onDrawWithContent.drawContent()
                        drawContext.canvas = originalCanvas
                    }
                    picture.endRecording()
                    drawContent()
                }
            }
    ) {
        content()
    }
}

@Composable
fun DocChartCard(
    title: String,
    fileName: String,
    controller: ScreenshotController,
    modifier: Modifier = Modifier,
    parameters: @Composable () -> Unit,
    chartContent: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { controller.capture(fileName) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF67C23A)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("截取图标", fontSize = 11.sp)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            ChartScreenshotContainer(
                fileName = fileName,
                controller = controller,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                chartContent()
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color.LightGray.copy(alpha = 0.3f)))
            Spacer(modifier = Modifier.height(12.dp))
            
            parameters()
        }
    }
}

@Composable
fun ParamRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun DocScreenshotsScreen(style: ChartStyle) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isCapturingAll by remember { mutableStateOf(false) }

    // 初始化 13 个图表的控制器
    val cLine = remember { ScreenshotController() }
    val cBar = remember { ScreenshotController() }
    val cPie = remember { ScreenshotController() }
    val cBar3D = remember { ScreenshotController() }
    val cCalendar = remember { ScreenshotController() }
    val cGauge = remember { ScreenshotController() }
    val cRadar = remember { ScreenshotController() }
    val cKLine = remember { ScreenshotController() }
    val cScatter = remember { ScreenshotController() }
    val cBoxplot = remember { ScreenshotController() }
    val cFunnel = remember { ScreenshotController() }
    val cMixed = remember { ScreenshotController() }
    val cPolar = remember { ScreenshotController() }

    val themeSuffix = if (style.backgroundColor == Color(0xFF1B1B1D)) "_dark" else "_light"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "文档截图自动对齐生成器",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "以下 13 类图表的数据和样式与 GUIDE.md / README.md 中的快速上手及使用手册代码完全一致。点击“一键截取全部文档图表”或者单卡片的“截取图标”按钮，可截取保存纯图表区域至相册 Pictures/ComposeChart/ 目录。",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = {
                        if (!isCapturingAll) {
                            coroutineScope.launch {
                                isCapturingAll = true
                                Toast.makeText(context, "开始一键生成全部 13 个图表截图...", Toast.LENGTH_SHORT).show()
                                
                                cLine.capture("doc_line$themeSuffix")
                                delay(800)
                                cBar.capture("doc_bar$themeSuffix")
                                delay(800)
                                cPie.capture("doc_pie$themeSuffix")
                                delay(800)
                                cBar3D.capture("doc_bar3d$themeSuffix")
                                delay(800)
                                cCalendar.capture("doc_calendar$themeSuffix")
                                delay(800)
                                cGauge.capture("doc_gauge$themeSuffix")
                                delay(800)
                                cRadar.capture("doc_radar$themeSuffix")
                                delay(800)
                                cKLine.capture("doc_kline$themeSuffix")
                                delay(800)
                                cScatter.capture("doc_scatter$themeSuffix")
                                delay(800)
                                cBoxplot.capture("doc_boxplot$themeSuffix")
                                delay(800)
                                cFunnel.capture("doc_funnel$themeSuffix")
                                delay(800)
                                cMixed.capture("doc_mixed$themeSuffix")
                                delay(800)
                                cPolar.capture("doc_polar$themeSuffix")
                                delay(800)
                                
                                isCapturingAll = false
                                Toast.makeText(context, "🎉 一键生成全部文档截图完成！", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE6A23C)),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCapturingAll
                ) {
                    Text(if (isCapturingAll) "生成中..." else "一键生成全部 13 个图表截图")
                }
            }
        }

        // 1. 折线图
        DocChartCard(
            title = "1. 折线图 (LineChart)",
            fileName = "doc_line$themeSuffix",
            controller = cLine,
            parameters = {
                ParamRow("数据分类 (xLabels)", "周一 至 周日 (7大项)")
                ParamRow("系列 1 (series[0])", "邮件营销 (平滑线, 渐变面积, 颜色: #5470C6)")
                ParamRow("系列 2 (series[1])", "联盟广告 (平滑线, 无面积, 颜色: #91CC75)")
            }
        ) {
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
            LineChart(data = lineChartData, style = style, modifier = Modifier.fillMaxSize())
        }

        // 2. 柱状图
        DocChartCard(
            title = "2. 柱状图 (BarChart)",
            fileName = "doc_bar$themeSuffix",
            controller = cBar,
            parameters = {
                ParamRow("数据分类 (xLabels)", "一月 至 六月 (6大项)")
                ParamRow("系列 1 (series[0])", "蒸发量 (占比: 0.5, 圆角: 12dp, 颜色: #5470C6)")
                ParamRow("系列 2 (series[1])", "降水量 (占比: 0.5, 圆角: 12dp, 颜色: #91CC75)")
            }
        ) {
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
            BarChart(data = barChartData, style = style, modifier = Modifier.fillMaxSize())
        }

        // 3. 饼图/环形图
        DocChartCard(
            title = "3. 饼图/环形图/南丁格尔玫瑰图 (PieChart)",
            fileName = "doc_pie$themeSuffix",
            controller = cPie,
            parameters = {
                ParamRow("扇区数据 (slices)", "搜索引擎(1048), 直接输入(735), 友情链接(580), 邮件营销(484)")
                ParamRow("内圆内径占比 (innerRadiusRatio)", "0.6f (空心圆环)")
                ParamRow("扇区间隙 (padAngle)", "3.0f (白色分割线)")
                ParamRow("边缘圆角 (cornerRadius)", "8.dp (圆滑胶囊边)")
            }
        ) {
            val pieChartData = PieChartData(
                slices = listOf(
                    PieSlice("搜索引擎", 1048f, Color(0xFF5470C6)),
                    PieSlice("直接输入", 735f, Color(0xFF91CC75)),
                    PieSlice("友情链接", 580f, Color(0xFFFAC858)),
                    PieSlice("邮件营销", 484f, Color(0xFFEE6666))
                )
            )
            val customStyle = style.copy(
                pieOptions = style.pieOptions.copy(
                    innerRadiusRatio = 0.6f,
                    padAngle = 3f,
                    cornerRadius = 8.dp
                )
            )
            PieChart(data = pieChartData, style = customStyle, modifier = Modifier.fillMaxSize())
        }

        // 4. 3D 柱状图
        DocChartCard(
            title = "4. 3D 柱状打卡图 (Bar3DChart)",
            fileName = "doc_bar3d$themeSuffix",
            controller = cBar3D,
            parameters = {
                ParamRow("初始偏航角 / 俯仰角 / 缩放", "Yaw = -45f, Pitch = 30f, Zoom = 1.0f")
                ParamRow("柱体宽度比例 (barWidthRatio)", "0.5f")
                ParamRow("三维热力色彩 (visualMapColors)", "十段/四段渐变色带映射")
            }
        ) {
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
            Bar3DChart(data = bar3DChartData, options = options, style = style, modifier = Modifier.fillMaxSize())
        }

        // 5. 日历图
        DocChartCard(
            title = "5. 矩阵日历热力图 (CalendarChart)",
            fileName = "doc_calendar$themeSuffix",
            controller = cCalendar,
            parameters = {
                ParamRow("排布方向 / 格子大小 / 间距", "Horizontal, cellSize = 12.dp, cellGap = 3.dp")
                ParamRow("每日数值计算方式", "value = (dayIndex % 7 * 15)")
                ParamRow("热力色彩色带 (visualMapColors)", "GitHub contributions 四段绿度渐变色带")
            }
        ) {
            val calendarData = remember {
                val days = mutableListOf<CalendarDayData>()
                val start = java.time.LocalDate.of(2026, 1, 1)
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
                CalendarChartData(year = 2026, days = days)
            }
            CalendarChart(
                data = calendarData,
                options = CalendarOptions(
                    orientation = CalendarOrientation.Horizontal,
                    cellSize = 12.dp,
                    cellGap = 3.dp,
                    visualMapColors = listOf(Color(0xFFEBEDF0), Color(0xFF9BE9A8), Color(0xFF40C463), Color(0xFF216E39))
                ),
                style = style,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 6. 仪表盘
        DocChartCard(
            title = "6. 仪表盘 (GaugeChart)",
            fileName = "doc_gauge$themeSuffix",
            controller = cGauge,
            parameters = {
                ParamRow("当前刻度值 / 最小值 / 最大值", "value = 68.5f, min = 0f, max = 100f")
                ParamRow("角度选项 (startAngle/sweep)", "180f / 180f (半圆仪表)")
                ParamRow("进度弧模式 (isProgress/pointer)", "isProgress = true, showPointer = false")
                ParamRow("等级色彩分段 (colors)", "0.3->#91CC75, 0.7->#FAC858, 1.0->#EE6666")
            }
        ) {
            val gaugeData = remember { GaugeChartData(value = 68.5f, min = 0f, max = 100f) }
            val customOptions = remember {
                GaugeOptions(
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
            }
            GaugeChart(data = gaugeData, options = customOptions, style = style, modifier = Modifier.fillMaxSize())
        }

        // 7. 雷达图
        DocChartCard(
            title = "7. 战力对比雷达图 (RadarChart)",
            fileName = "doc_radar$themeSuffix",
            controller = cRadar,
            parameters = {
                ParamRow("评估维度数 (indicators)", "销售, 管理, 信息技术, 客服, 研发, 市场 (6维)")
                ParamRow("系列 1 (预算分配)", "4300, 10000, 28000, 35000, 50000, 19000, 色彩: #5470C6")
                ParamRow("系列 2 (实际开销)", "5000, 14000, 28000, 31000, 42000, 21000, 色彩: #91CC75")
            }
        ) {
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
            RadarChart(data = radarData, style = style, modifier = Modifier.fillMaxSize())
        }

        // 8. 金融K线图
        DocChartCard(
            title = "8. 金融个股日K线图 (KLineChart)",
            fileName = "doc_kline$themeSuffix",
            controller = cKLine,
            parameters = {
                ParamRow("数据天数", "5日行情数据")
                ParamRow("单条内容 (KLineEntry)", "日期, 开盘, 收盘, 最低, 最高, 成交量")
            }
        ) {
            val entries = listOf(
                KLineEntry("2026-06-01", 2320.26f, 2302.6f, 2287.3f, 2362.94f, 120000f),
                KLineEntry("2026-06-02", 2300f, 2291.3f, 2281.4f, 2311.68f, 85000f),
                KLineEntry("2026-06-03", 2293.09f, 2278.4f, 2272.24f, 2308.43f, 98000f),
                KLineEntry("2026-06-04", 2280f, 2314.9f, 2273.4f, 2320.1f, 140000f),
                KLineEntry("2026-06-05", 2315f, 2350.2f, 2310.5f, 2360f, 180000f)
            )
            val kLineData = KLineChartData(entries = entries)
            KLineChart(data = kLineData, style = style, modifier = Modifier.fillMaxSize())
        }

        // 9. 散点图/气泡图
        DocChartCard(
            title = "9. 告警监控散点图/气泡图 (ScatterChart)",
            fileName = "doc_scatter$themeSuffix",
            controller = cScatter,
            parameters = {
                ParamRow("系列 1 (常规数据)", "4个数据点, 散点半径: 10.dp, 颜色: #5470C6")
                ParamRow("系列 2 (涟漪告警点)", "2个数据点, 涟漪开关 = true, 颜色: #EE6666")
            }
        ) {
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
            ScatterChart(data = scatterData, style = style, modifier = Modifier.fillMaxSize())
        }

        // 10. 箱线图
        DocChartCard(
            title = "10. 工艺缺陷箱线图 (BoxplotChart)",
            fileName = "doc_boxplot$themeSuffix",
            controller = cBoxplot,
            parameters = {
                ParamRow("特征分类 (xLabels)", "轴承A, 轴承B, 轴承C")
                ParamRow("系列 1 (series[0])", "工艺缺陷统计 (颜色: #91CC75)")
                ParamRow("数据值格式", "五数大观 + 离群点")
            }
        ) {
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
            BoxplotChart(data = boxplotData, style = style, modifier = Modifier.fillMaxSize())
        }

        // 11. 漏斗图
        DocChartCard(
            title = "11. 流失转化漏斗图 (FunnelChart)",
            fileName = "doc_funnel$themeSuffix",
            controller = cFunnel,
            parameters = {
                ParamRow("转化环节 (items)", "展现(100), 点击(80), 访问(60), 咨询(40), 订单(20)")
                ParamRow("环节色彩", "#5470C6, #91CC75, #FAC858, #EE6666, #73C0DE")
            }
        ) {
            val funnelData = FunnelChartData(
                slices = listOf(
                    FunnelSlice("展现", 100f, Color(0xFF5470C6)),
                    FunnelSlice("点击", 80f, Color(0xFF91CC75)),
                    FunnelSlice("访问", 60f, Color(0xFFFAC858)),
                    FunnelSlice("咨询", 40f, Color(0xFFEE6666)),
                    FunnelSlice("订单", 20f, Color(0xFF73C0DE))
                )
            )
            FunnelChart(data = funnelData, style = style, modifier = Modifier.fillMaxSize())
        }

        // 12. 混合图
        DocChartCard(
            title = "12. 雨量蒸发混合图 (MixedChart)",
            fileName = "doc_mixed$themeSuffix",
            controller = cMixed,
            parameters = {
                ParamRow("时间轴 (xLabels)", "2:00, 4:00, 6:00, 8:00, 10:00, 12:00")
                ParamRow("柱状数据 (左Y轴)", "降水量 (颜色: #5470C6, 绑定 yAxisIndex = 0)")
                ParamRow("折线数据 (右Y轴)", "平均温度 (颜色: #FAC858, 绑定 yAxisIndex = 1)")
            }
        ) {
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
            MixedChart(data = mixedData, style = style, modifier = Modifier.fillMaxSize())
        }

        // 13. 极坐标图
        DocChartCard(
            title = "13. 极坐标系扇形/圆环图 (PolarBarChart)",
            fileName = "doc_polar$themeSuffix",
            controller = cPolar,
            parameters = {
                ParamRow("极轴分类 (xLabels)", "分类一, 分类二, 分类三, 分类四")
                ParamRow("系列 1 (series[0])", "系列A (数值: 80, 60, 95, 45, 颜色: #73C0DE)")
            }
        ) {
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
            PolarBarChart(data = polarData, style = style, modifier = Modifier.fillMaxSize())
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
