package io.github.composechart.showroom.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.composechart.charts.bar.BarChart
import io.github.composechart.charts.bar.BarChartData
import io.github.composechart.charts.bar.BarSeries
import io.github.composechart.charts.bar.BarValue
import io.github.composechart.charts.boxplot.BoxplotChart
import io.github.composechart.charts.boxplot.BoxplotChartData
import io.github.composechart.charts.boxplot.BoxplotOutlier
import io.github.composechart.charts.boxplot.BoxplotPoint
import io.github.composechart.charts.boxplot.BoxplotSeries
import io.github.composechart.charts.funnel.FunnelAlign
import io.github.composechart.charts.funnel.FunnelChart
import io.github.composechart.charts.funnel.FunnelChartData
import io.github.composechart.charts.funnel.FunnelOptions
import io.github.composechart.charts.funnel.FunnelSlice
import io.github.composechart.charts.funnel.FunnelSort
import io.github.composechart.charts.gauge.GaugeChart
import io.github.composechart.charts.gauge.GaugeChartData
import io.github.composechart.charts.gauge.GaugeOptions
import io.github.composechart.charts.gauge.PointerType
import io.github.composechart.charts.kline.KLineChart
import io.github.composechart.charts.kline.KLineChartData
import io.github.composechart.charts.kline.KLineEntry
import io.github.composechart.charts.kline.KLineStyle
import io.github.composechart.charts.radar.RadarChart
import io.github.composechart.charts.radar.RadarChartData
import io.github.composechart.charts.radar.RadarIndicator
import io.github.composechart.charts.radar.RadarSeries
import io.github.composechart.charts.scatter.EffectOptions
import io.github.composechart.charts.scatter.ScatterChart
import io.github.composechart.charts.scatter.ScatterChartData
import io.github.composechart.charts.scatter.ScatterPoint
import io.github.composechart.charts.scatter.ScatterSeries
import io.github.composechart.charts.scatter.ScatterSymbolType
import io.github.composechart.charts.scatter.ScatterVisualMap
import io.github.composechart.core.state.rememberInteractionState
import io.github.composechart.core.state.rememberViewportState
import io.github.composechart.core.style.ChartStyle
import io.github.composechart.core.style.RadarShape
import io.github.composechart.core.style.TitleOptions
import io.github.composechart.core.style.TooltipOptions

/**
 * Demo 10: 多维对比雷达图
 */
@Composable
fun DemoRadar(style: ChartStyle) {
    var shape by remember { mutableStateOf(RadarShape.Polygon) }
    var splitNumber by remember { mutableIntStateOf(5) }
    var drawScaleLabel by remember { mutableStateOf(true) }

    val characterData = remember {
        RadarChartData(
            indicators = listOf(
                RadarIndicator("力量", 100f),
                RadarIndicator("智力", 100f),
                RadarIndicator("敏捷", 100f),
                RadarIndicator("耐力", 100f),
                RadarIndicator("防御", 100f),
                RadarIndicator("速度", 100f)
            ),
            series = listOf(
                RadarSeries(
                    name = "狂战士",
                    values = listOf(90f, 40f, 75f, 85f, 60f, 70f),
                    color = Color(0xFFEE6666) // 红色
                ),
                RadarSeries(
                    name = "圣骑士",
                    values = listOf(70f, 65f, 50f, 90f, 95f, 55f),
                    color = Color(0xFFFAC858) // 黄色
                )
            )
        )
    }

    val customStyle = style.copy(
        titleOptions = TitleOptions(
            text = "角色战斗属性对比雷达",
            subtext = "支持网格样式与圈数自定义、引线刻度标签显示",
            textStyle = style.titleOptions.textStyle.copy(fontSize = 16.sp)
        ),
        radarOptions = style.radarOptions.copy(
            shape = shape,
            splitNumber = splitNumber,
            drawScaleLabel = drawScaleLabel
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
        ) {
            RadarChart(
                data = characterData,
                style = customStyle,
                modifier = Modifier.fillMaxSize()
            )
        }

        ControlPanel {
            ControlSelector(
                label = "网格多边形线框形状 (shape)",
                options = listOf(RadarShape.Polygon, RadarShape.Circle),
                selectedOption = shape,
                onOptionSelected = { shape = it },
                optionLabel = {
                    when (it) {
                        RadarShape.Polygon -> "Polygon (直角多边形蛛网)"
                        RadarShape.Circle -> "Circle (同心圆圈层蛛网)"
                    }
                }
            )

            ControlSwitch(label = "显示 12 点钟主骨架轴刻度文本", checked = drawScaleLabel, onCheckedChange = { drawScaleLabel = it })

            ControlSlider(
                label = "同心圆/多边形等分圈数 (splitNumber)",
                value = splitNumber.toFloat(),
                valueRange = 3f..8f,
                onValueChange = { splitNumber = it.toInt() },
                valueFormatter = { "${it.toInt()} 圈" }
            )
        }
    }
}

/**
 * Demo 11: 金融指数 K线与成交量联动图
 */
@Composable
fun DemoKLine(style: ChartStyle) {
    var upFilled by remember { mutableStateOf(true) }
    var downFilled by remember { mutableStateOf(true) }
    var maLineWidth by remember { mutableFloatStateOf(1.2f) }

    val kLineEntries = remember { generateKLineData(100) }
    val sharedViewport = rememberViewportState(initialMinX = 50f, initialMaxX = 85f)
    val sharedInteraction = rememberInteractionState()

    val volumeData = remember(kLineEntries) {
        val upVals = kLineEntries.map { BarValue(if (it.close >= it.open) it.volume else 0f) }
        val downVals = kLineEntries.map { BarValue(if (it.close < it.open) it.volume else 0f) }
        BarChartData(
            xLabels = kLineEntries.map { it.time },
            series = listOf(
                BarSeries(
                    name = "上涨成交量",
                    values = upVals,
                    color = Color(0xFFEE6666),
                    stack = "vol"
                ),
                BarSeries(
                    name = "下跌成交量",
                    values = downVals,
                    color = Color(0xFF3BA272),
                    stack = "vol"
                )
            )
        )
    }

    val customKLineStyle = KLineStyle.Default.copy(
        upFilled = upFilled,
        downFilled = downFilled,
        maLineWidth = maLineWidth.dp
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 主 K 线图 (70% 占比)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
        ) {
            KLineChart(
                data = KLineChartData(entries = kLineEntries),
                style = style.copy(
                    titleOptions = TitleOptions(
                        text = "沪深300 金融日 K线行情",
                        subtext = "双指滑放联动下挂柱状成交量、MA5/10/20均线实时参数化配置",
                        textStyle = style.titleOptions.textStyle.copy(fontSize = 15.sp)
                    ),
                    xAxisOptions = style.xAxisOptions.copy(showLabels = false),
                    gridOptions = style.gridOptions.copy(bottom = 8.dp)
                ),
                kLineStyle = customKLineStyle,
                viewportState = sharedViewport,
                interactionState = sharedInteraction,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 辅成交量图 (30% 占比)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            BarChart(
                data = volumeData,
                style = style.copy(
                    titleOptions = style.titleOptions.copy(show = false),
                    legendOptions = style.legendOptions.copy(show = false),
                    gridOptions = style.gridOptions.copy(top = 8.dp)
                ),
                viewportState = sharedViewport,
                interactionState = sharedInteraction,
                modifier = Modifier.fillMaxSize()
            )
        }

        ControlPanel {
            ControlSwitch(label = "上涨蜡烛烛体实心填充 (upFilled)", checked = upFilled, onCheckedChange = { upFilled = it })
            ControlSwitch(label = "下跌蜡烛烛体实心填充 (downFilled)", checked = downFilled, onCheckedChange = { downFilled = it })

            ControlSlider(
                label = "均线线条粗细 (maLineWidth)",
                value = maLineWidth,
                valueRange = 0.5f..4f,
                onValueChange = { maLineWidth = it },
                valueFormatter = { String.format("%.1f dp", it) }
            )
        }
    }
}

private fun generateKLineData(count: Int): List<KLineEntry> {
    val entries = mutableListOf<KLineEntry>()
    var prevClose = 3200f
    
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    val cal = java.util.Calendar.getInstance()
    cal.add(java.util.Calendar.DAY_OF_YEAR, -count)

    for (i in 0 until count) {
        cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        val time = sdf.format(cal.time)

        val changePercent = (Math.random().toFloat() - 0.47f) * 0.03f
        val open = prevClose * (1f + (Math.random().toFloat() - 0.5f) * 0.008f)
        val close = open * (1f + changePercent)
        
        val high = kotlin.math.max(open, close) * (1f + Math.random().toFloat() * 0.012f)
        val low = kotlin.math.min(open, close) * (1f - Math.random().toFloat() * 0.012f)
        val volume = 150000f + Math.random().toFloat() * 600000f

        entries.add(KLineEntry(time, open, close, high, low, volume))
        prevClose = close
    }
    return entries
}

/**
 * Demo 12: 城市气泡图与系统故障告警特效图
 */
@Composable
fun DemoScatter(style: ChartStyle) {
    var rippleCount by remember { mutableIntStateOf(2) }
    var rippleRadiusRatio by remember { mutableFloatStateOf(2.5f) }

    val alertData = remember(rippleCount, rippleRadiusRatio) {
        ScatterChartData(
            series = listOf(
                ScatterSeries(
                    name = "常规调用波动",
                    points = listOf(
                        ScatterPoint(2.5f, 120f),
                        ScatterPoint(4f, 95f),
                        ScatterPoint(6.5f, 110f),
                        ScatterPoint(8.2f, 230f),
                        ScatterPoint(10.5f, 310f),
                        ScatterPoint(12f, 290f),
                        ScatterPoint(14.8f, 260f),
                        ScatterPoint(16.2f, 340f),
                        ScatterPoint(18.5f, 410f),
                        ScatterPoint(20.1f, 380f),
                        ScatterPoint(22.4f, 210f)
                    ),
                    color = Color(0xFF91CC75),
                    symbol = ScatterSymbolType.Circle,
                    symbolSize = 7.dp
                ),
                ScatterSeries(
                    name = "严重故障告警 (涟漪特效)",
                    points = listOf(
                        ScatterPoint(9.0f, 450f, name = "09:00 主库连接池占满"),
                        ScatterPoint(15.5f, 520f, name = "15:30 支付网关超时阻断"),
                        ScatterPoint(19.2f, 490f, name = "19:12 核心结算服务 OOM")
                    ),
                    color = Color(0xFFFF003C), // 刺眼红
                    symbol = ScatterSymbolType.Diamond,
                    symbolSize = 10.dp,
                    effectScatter = true,
                    effectOptions = EffectOptions(
                        rippleRadiusRatio = rippleRadiusRatio,
                        rippleCount = rippleCount,
                        durationMs = 1500
                    )
                )
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
        ) {
            ScatterChart(
                data = alertData,
                style = style.copy(
                    titleOptions = TitleOptions(
                        text = "服务系统异常告警监控 (EffectScatter)",
                        subtext = "严重警告点开启不断外扩渐隐的霓虹涟漪动画",
                        textStyle = style.titleOptions.textStyle.copy(fontSize = 15.sp)
                    ),
                    xAxisOptions = style.xAxisOptions.copy(
                        showGridLines = true,
                        name = "时间 (h)"
                    ),
                    yAxisOptions = style.yAxisOptions.copy(
                        showGridLines = true,
                        name = "QPS/次数"
                    )
                ),
                modifier = Modifier.fillMaxSize()
            )
        }

        ControlPanel {
            ControlSlider(
                label = "故障点发光涟漪圈数 (rippleCount)",
                value = rippleCount.toFloat(),
                valueRange = 1f..4f,
                onValueChange = { rippleCount = it.toInt() },
                valueFormatter = { "${it.toInt()} 层" }
            )

            ControlSlider(
                label = "涟漪最大扩张半径比率 (rippleRadiusRatio)",
                value = rippleRadiusRatio,
                valueRange = 1.5f..4.0f,
                onValueChange = { rippleRadiusRatio = it },
                valueFormatter = { String.format("%.2f 倍", it) }
            )
        }
    }
}

/**
 * Demo 13: 仪表盘进度指示器
 */
@Composable
fun DemoGauge(style: ChartStyle) {
    var healthValue by remember { mutableStateOf(85f) }
    var speedValue by remember { mutableStateOf(120f) }
    var pointerWidth by remember { mutableFloatStateOf(8f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 赛车时速仪表盘
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
        ) {
            GaugeChart(
                data = GaugeChartData(
                    value = speedValue,
                    min = 0f,
                    max = 240f,
                    name = "车速表 (ENGINE COMP)",
                    unit = " km/h"
                ),
                style = style.copy(
                    titleOptions = TitleOptions(
                        text = "高性能赛车车速仪表",
                        subtext = "定制 240km/h 极速刻度、特制红色指针及控制台实时联动",
                        textStyle = style.titleOptions.textStyle.copy(fontSize = 15.sp)
                    )
                ),
                options = GaugeOptions(
                    startAngle = 135f,
                    sweepAngle = 270f,
                    axisLineWidth = 10.dp,
                    axisLineColors = listOf(
                        0.5f to Color(0xFF5470C6), // 0-120km/h 蓝色经济速度
                        0.8f to Color(0xFFFAC858), // 120-192km/h 高速行驶
                        1.0f to Color(0xFFFF003C)  // 192-240km/h 极速警告
                    ),
                    pointerWidth = pointerWidth.dp,
                    pointerLengthRatio = 0.72f,
                    pointerColor = Color(0xFFFF003C), // 统一红色烈焰指针
                    centerCircleRadius = 12.dp
                ),
                modifier = Modifier.fillMaxSize()
            )
        }

        // 交互按钮与控制
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Button(
                onClick = { speedValue = (speedValue - 30f).coerceAtLeast(0f) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEE6666)),
                modifier = Modifier.weight(1f)
            ) {
                Text("减速 -30")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = { speedValue = (Math.random().toFloat() * 240f) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF73C0DE)),
                modifier = Modifier.weight(1.2f)
            ) {
                Text("随机车速")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = { speedValue = (speedValue + 30f).coerceAtMost(240f) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF91CC75)),
                modifier = Modifier.weight(1f)
            ) {
                Text("加速 +30")
            }
        }

        ControlPanel {
            ControlSlider(
                label = "车速表指针宽度 (pointerWidth)",
                value = pointerWidth,
                valueRange = 4f..16f,
                onValueChange = { pointerWidth = it },
                valueFormatter = { String.format("%.0f dp", it) }
            )
        }
    }
}

/**
 * Demo 13B: 等级仪表盘 (Grade Gauge)
 */
@Composable
fun DemoGradeGauge(style: ChartStyle) {
    var score by remember { mutableStateOf(70f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
        ) {
            GaugeChart(
                data = GaugeChartData(
                    value = score,
                    min = 0f,
                    max = 100f,
                    name = "Grade Rating",
                    unit = "分"
                ),
                style = style.copy(
                    titleOptions = TitleOptions(
                        text = "等级分类仪表盘",
                        subtext = "对标 ECharts Grade Gauge：使用短三角悬空指针、自定义段标签 (D/C/B/A)",
                        textStyle = style.titleOptions.textStyle.copy(fontSize = 15.sp)
                    )
                ),
                options = GaugeOptions(
                    startAngle = 180f,
                    sweepAngle = 180f,
                    axisLineWidth = 12.dp,
                    axisLineColors = listOf(
                        0.3f to Color(0xFFEE6666), // D: Red
                        0.5f to Color(0xFFFAC858), // C: Yellow
                        0.7f to Color(0xFF73C0DE), // B: Cyan/Blue
                        1.0f to Color(0xFF91CC75)  // A: Green
                    ),
                    tickCount = 6,
                    subTickCount = 5,
                    pointerType = PointerType.ShortTriangle,
                    valueAboveName = false,
                    customLabels = listOf("", "D", "C", "", "B", "A", "")
                ),
                modifier = Modifier.fillMaxSize()
            )
        }

        // 交互控制
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Button(
                onClick = { score = (score - 10f).coerceAtLeast(0f) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEE6666)),
                modifier = Modifier.weight(1f)
            ) {
                Text("减 10 分")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = { score = (Math.random().toFloat() * 100f) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF73C0DE)),
                modifier = Modifier.weight(1.2f)
            ) {
                Text("随机分数")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = { score = (score + 10f).coerceAtMost(100f) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF91CC75)),
                modifier = Modifier.weight(1f)
            ) {
                Text("加 10 分")
            }
        }

        ControlPanel {
            ControlSlider(
                label = "当前分数",
                value = score,
                valueRange = 0f..100f,
                onValueChange = { score = it },
                valueFormatter = { String.format("%.1f 分", it) }
            )
        }
    }
}

/**
 * Demo 13C: 气温进度仪表盘 (Temperature Gauge)
 */
@Composable
fun DemoTemperatureGauge(style: ChartStyle) {
    var temp by remember { mutableStateOf(18.76f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
        ) {
            GaugeChart(
                data = GaugeChartData(
                    value = temp,
                    min = 0f,
                    max = 60f,
                    name = "气温",
                    unit = " ℃"
                ),
                style = style.copy(
                    titleOptions = TitleOptions(
                        text = "气温测量仪表盘",
                        subtext = "对标 ECharts Temperature Gauge：无指针进度条模式，使用圆角端点",
                        textStyle = style.titleOptions.textStyle.copy(fontSize = 15.sp)
                    )
                ),
                options = GaugeOptions(
                    startAngle = 180f,
                    sweepAngle = 180f,
                    axisLineWidth = 12.dp,
                    axisLineColors = listOf(
                        1.0f to Color(0xFFFF6B6B) // 单个色彩进度段
                    ),
                    tickCount = 12,
                    subTickCount = 5,
                    showPointer = false,
                    isProgress = true,
                    valueAboveName = true
                ),
                modifier = Modifier.fillMaxSize()
            )
        }

        // 交互控制
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Button(
                onClick = { temp = (temp - 5f).coerceAtLeast(0f) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF73C0DE)),
                modifier = Modifier.weight(1f)
            ) {
                Text("降温 -5℃")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = { temp = (Math.random().toFloat() * 60f) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFAC858)),
                modifier = Modifier.weight(1.2f)
            ) {
                Text("随机温度")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = { temp = (temp + 5f).coerceAtMost(60f) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B6B)),
                modifier = Modifier.weight(1f)
            ) {
                Text("升温 +5℃")
            }
        }

        ControlPanel {
            ControlSlider(
                label = "当前温度",
                value = temp,
                valueRange = 0f..60f,
                onValueChange = { temp = it },
                valueFormatter = { String.format("%.2f ℃", it) }
            )
        }
    }
}

/**
 * Demo 14: 漏斗图流失转化分析
 */
@Composable
fun DemoFunnel(style: ChartStyle) {
    var align by remember { mutableStateOf(FunnelAlign.Center) }
    var sort by remember { mutableStateOf(FunnelSort.Descending) }
    var gapValue by remember { mutableFloatStateOf(5f) }

    val purchaseData = remember {
        FunnelChartData(
            slices = listOf(
                FunnelSlice("浏览商品", 1200f),
                FunnelSlice("搜索对比", 950f),
                FunnelSlice("加入购物车", 580f),
                FunnelSlice("生成订单", 320f),
                FunnelSlice("支付成功", 240f)
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
        ) {
            FunnelChart(
                data = purchaseData,
                style = style.copy(
                    titleOptions = TitleOptions(
                        text = "电商购买转化率漏斗",
                        subtext = "展示漏斗对齐方向、缝隙间隔、升降排序与转化率气泡标定",
                        textStyle = style.titleOptions.textStyle.copy(fontSize = 15.sp)
                    )
                ),
                options = FunnelOptions(
                    align = align,
                    sort = sort,
                    gap = gapValue.dp,
                    cornerRadius = 6.dp,
                    minWidthRatio = 0.15f
                ),
                modifier = Modifier.fillMaxSize()
            )
        }

        ControlPanel {
            ControlSelector(
                label = "漏斗水平对齐方向 (align)",
                options = listOf(FunnelAlign.Left, FunnelAlign.Center, FunnelAlign.Right),
                selectedOption = align,
                onOptionSelected = { align = it },
                optionLabel = {
                    when (it) {
                        FunnelAlign.Left -> "左对齐 (Left)"
                        FunnelAlign.Center -> "居中对称 (Center)"
                        FunnelAlign.Right -> "右对齐 (Right)"
                    }
                }
            )

            ControlSelector(
                label = "漏斗分层排序规则 (sort)",
                options = listOf(FunnelSort.Descending, FunnelSort.Ascending),
                selectedOption = sort,
                onOptionSelected = { sort = it },
                optionLabel = {
                    when (it) {
                        FunnelSort.Descending -> "降序 (常规漏斗)"
                        FunnelSort.Ascending -> "升序 (倒置金字塔)"
                        else -> "降序 (常规漏斗)"
                    }
                }
            )

            ControlSlider(
                label = "漏斗阶梯梯形间距 (gap)",
                value = gapValue,
                valueRange = 0f..15f,
                onValueChange = { gapValue = it },
                valueFormatter = { String.format("%.0f dp", it) }
            )
        }
    }
}

/**
 * Demo 15: 箱线统计图（盒须图）
 */
@Composable
fun DemoBoxplot(style: ChartStyle) {
    var boxWidthRatio by remember { mutableFloatStateOf(0.45f) }
    var whiskerWidthRatio by remember { mutableFloatStateOf(0.22f) }

    val labels = listOf("工序A", "工序B", "工序C", "工序D", "工序E")

    // 一号生产线：五数概括 + 异常值
    val line1Points = listOf(
        BoxplotPoint(10f, 22f, 30f, 38f, 50f),
        BoxplotPoint(15f, 25f, 32f, 40f, 55f),
        BoxplotPoint(8f, 18f, 26f, 35f, 48f),
        BoxplotPoint(12f, 28f, 36f, 44f, 58f),
        BoxplotPoint(18f, 30f, 42f, 50f, 65f)
    )
    val line1Outliers = listOf(
        BoxplotOutlier(0, 62f, "异常大尺寸"),
        BoxplotOutlier(0, 3f, "异常小尺寸"),
        BoxplotOutlier(2, 58f, "超差"),
        BoxplotOutlier(4, 78f, "严重溢出")
    )

    // 二号生产线：五数概括 + 异常值
    val line2Points = listOf(
        BoxplotPoint(12f, 20f, 28f, 36f, 46f),
        BoxplotPoint(10f, 22f, 30f, 38f, 50f),
        BoxplotPoint(14f, 24f, 34f, 42f, 52f),
        BoxplotPoint(16f, 26f, 35f, 43f, 56f),
        BoxplotPoint(15f, 28f, 38f, 47f, 60f)
    )
    val line2Outliers = listOf(
        BoxplotOutlier(1, 65f, "溢出点"),
        BoxplotOutlier(3, 8f, "极小异常")
    )

    val data = BoxplotChartData(
        xLabels = labels,
        series = listOf(
            BoxplotSeries(
                name = "一号生产线",
                points = line1Points,
                color = Color(0xFF5470C6),
                boxWidthRatio = boxWidthRatio,
                whiskerWidthRatio = whiskerWidthRatio,
                outliers = line1Outliers
            ),
            BoxplotSeries(
                name = "二号生产线",
                points = line2Points,
                color = Color(0xFF91CC75),
                boxWidthRatio = boxWidthRatio,
                whiskerWidthRatio = whiskerWidthRatio,
                outliers = line2Outliers
            )
        )
    )

    val customStyle = style.copy(
        titleOptions = TitleOptions(
            text = "生产线工艺缺陷尺寸箱线统计",
            subtext = "多系列并列排布，支持五数大观概括、触须短盖及异常值散点",
            textStyle = style.titleOptions.textStyle.copy(fontSize = 15.sp)
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
        ) {
            BoxplotChart(
                data = data,
                style = customStyle,
                tooltipOptions = TooltipOptions(enabled = true),
                modifier = Modifier.fillMaxSize()
            )
        }

        ControlPanel {
            ControlSlider(
                label = "系列箱体横向宽度比 (boxWidthRatio)",
                value = boxWidthRatio,
                valueRange = 0.2f..0.8f,
                onValueChange = { boxWidthRatio = it },
                valueFormatter = { String.format("%.2f", it) }
            )

            ControlSlider(
                label = "最大/最小影线端盖宽度比 (whiskerWidthRatio)",
                value = whiskerWidthRatio,
                valueRange = 0.1f..0.5f,
                onValueChange = { whiskerWidthRatio = it },
                valueFormatter = { String.format("%.2f", it) }
            )
        }
    }
}
