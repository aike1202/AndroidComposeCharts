package io.github.composechart.showroom.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.composechart.charts.line.LineChart
import io.github.composechart.charts.line.LineChartData
import io.github.composechart.charts.line.LinePoint
import io.github.composechart.charts.line.LineSeries
import io.github.composechart.charts.line.RangeAreaOptions
import io.github.composechart.charts.line.LineStyleType
import io.github.composechart.charts.line.MarkLine
import io.github.composechart.charts.line.MarkLineType
import io.github.composechart.charts.line.MarkPoint
import io.github.composechart.charts.line.MarkPointType
import io.github.composechart.charts.line.SymbolType
import io.github.composechart.core.state.ViewportMode
import io.github.composechart.core.state.rememberViewportState
import io.github.composechart.core.style.ChartStyle
import io.github.composechart.core.style.TitleOptions
import io.github.composechart.core.style.TooltipOptions
import kotlin.math.sin

/**
 * Demo 1: 24小时温度渐变面积图
 */
@Composable
fun DemoTemperature(style: ChartStyle) {
    // 1. 定义交互配置台的状态
    var isSmooth by remember { mutableStateOf(true) }
    var drawArea by remember { mutableStateOf(true) }
    var symbolType by remember { mutableStateOf(SymbolType.Circle) }
    var lineWidth by remember { mutableFloatStateOf(2.5f) }
    var showXGridLines by remember { mutableStateOf(true) }
    var showYGridLines by remember { mutableStateOf(true) }

    // 2. 生成模拟温度数据
    val labels = (0..23).map { String.format("%02d:00", it) }
    val temperatures = listOf(
        15f, 14.5f, 14f, 13.5f, 13f, 12.5f,
        14f, 16f, 18f, 20f, 22f, 24f,
        25.5f, 26.5f, 27f, 26f, 24.5f, 23f,
        21f, 19.5f, 18f, 17f, 16f, 15.5f
    )
    val points = temperatures.mapIndexed { index, temp ->
        LinePoint(index.toFloat(), temp)
    }

    val data = LineChartData(
        xLabels = labels,
        series = listOf(
            LineSeries(
                name = "温度 (℃)",
                points = points,
                color = Color(0xFFFAC858), // ECharts 暖橙色
                isSmooth = isSmooth,
                drawArea = drawArea,
                symbol = symbolType,
                symbolSize = 5.dp,
                lineWidth = lineWidth.dp
            )
        )
    )

    val customStyle = style.copy(
        titleOptions = TitleOptions(
            text = "今日温度变化趋势",
            subtext = "24小时气温连续平滑监测 (贝塞尔曲线面积图)",
            textStyle = style.titleOptions.textStyle.copy(fontSize = 16.sp)
        ),
        xAxisOptions = style.xAxisOptions.copy(
            showGridLines = showXGridLines
        ),
        yAxisOptions = style.yAxisOptions.copy(
            showGridLines = showYGridLines
        )
    )

    val viewportState = rememberViewportState(
        initialMinX = 0f,
        initialMaxX = 23f
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 图表展示区
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
        ) {
            LineChart(
                data = data,
                style = customStyle,
                viewportState = viewportState,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 参数控制台
        ControlPanel {
            ControlSwitch(label = "贝塞尔平滑曲线 (Smooth)", checked = isSmooth, onCheckedChange = { isSmooth = it })
            ControlSwitch(label = "渲染渐变面积 (drawArea)", checked = drawArea, onCheckedChange = { drawArea = it })
            ControlSwitch(label = "X 轴分割线 (showGridLines)", checked = showXGridLines, onCheckedChange = { showXGridLines = it })
            ControlSwitch(label = "Y 轴分割线 (showGridLines)", checked = showYGridLines, onCheckedChange = { showYGridLines = it })
            
            ControlSlider(
                label = "折线线条粗细 (lineWidth)",
                value = lineWidth,
                valueRange = 1f..6f,
                onValueChange = { lineWidth = it },
                valueFormatter = { String.format("%.1f dp", it) }
            )

            ControlSelector(
                label = "数据点符号样式 (SymbolType)",
                options = listOf(SymbolType.None, SymbolType.Circle, SymbolType.Square, SymbolType.Diamond),
                selectedOption = symbolType,
                onOptionSelected = { symbolType = it },
                optionLabel = {
                    when (it) {
                        SymbolType.None -> "无"
                        SymbolType.Circle -> "小圆点"
                        SymbolType.Square -> "方块"
                        SymbolType.Diamond -> "菱形"
                    }
                }
            )
        }
    }
}

/**
 * Demo 2: 多线对比与高级特性标注
 */
@Composable
fun DemoMultiSeries(style: ChartStyle) {
    var isSmooth by remember { mutableStateOf(false) }
    var showMarkPoints by remember { mutableStateOf(true) }
    var showMarkLines by remember { mutableStateOf(true) }
    var showYGridLines by remember { mutableStateOf(true) }

    val labels = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    
    val thisWeekPoints = listOf(150f, 230f, 224f, 218f, 135f, 147f, 260f).mapIndexed { i, v -> LinePoint(i.toFloat(), v) }
    val lastWeekPoints = listOf(220f, 182f, 191f, 234f, 290f, 330f, 310f).mapIndexed { i, v -> LinePoint(i.toFloat(), v) }

    val data = LineChartData(
        xLabels = labels,
        series = listOf(
            LineSeries(
                name = "本周浏览量",
                points = thisWeekPoints,
                color = Color(0xFF5470C6),
                isSmooth = isSmooth,
                symbol = SymbolType.Circle,
                symbolSize = 6.dp,
                markPoints = if (showMarkPoints) listOf(
                    MarkPoint(MarkPointType.Max, "最高值"),
                    MarkPoint(MarkPointType.Min, "最低值")
                ) else emptyList(),
                markLines = if (showMarkLines) listOf(
                    MarkLine(MarkLineType.Average, label = "本周均值")
                ) else emptyList()
            ),
            LineSeries(
                name = "上周同期",
                points = lastWeekPoints,
                color = Color(0xFFEE6666),
                lineStyle = LineStyleType.Dashed,
                isSmooth = isSmooth,
                symbol = SymbolType.Diamond,
                symbolSize = 5.dp
            )
        )
    )

    val customStyle = style.copy(
        titleOptions = TitleOptions(
            text = "流量趋势多线对比",
            subtext = "支持平均均线 (MarkLine) 与极值小水滴 (MarkPoint) 标识",
            textStyle = style.titleOptions.textStyle.copy(fontSize = 16.sp)
        ),
        yAxisOptions = style.yAxisOptions.copy(
            showGridLines = showYGridLines
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
            LineChart(
                data = data,
                style = customStyle,
                modifier = Modifier.fillMaxSize()
            )
        }

        ControlPanel {
            ControlSwitch(label = "两周数据曲线整体平滑 (Smooth)", checked = isSmooth, onCheckedChange = { isSmooth = it })
            ControlSwitch(label = "显示极值气泡标定 (MarkPoint)", checked = showMarkPoints, onCheckedChange = { showMarkPoints = it })
            ControlSwitch(label = "显示本周平均值辅助虚线 (MarkLine)", checked = showMarkLines, onCheckedChange = { showMarkLines = it })
            ControlSwitch(label = "Y 轴横向网格线 (showGridLines)", checked = showYGridLines, onCheckedChange = { showYGridLines = it })
        }
    }
}

/**
 * Demo 3: 密集点大数据量手势测试
 */
@Composable
fun DemoBigData(style: ChartStyle) {
    var pointsCount by remember { mutableIntStateOf(1000) }
    var connectNulls by remember { mutableStateOf(false) }

    // 动态生成正弦波密集数据
    val data = remember(pointsCount, connectNulls) {
        val labels = (0 until pointsCount).map { it.toString() }
        val points = (0 until pointsCount).map { i ->
            val sineValue = 100f + sin(i / 15f) * 45f + (Math.random().toFloat() - 0.5f) * 8f
            // 周期性空值点
            val yVal = if (i > 0 && i % 220 == 0) null else sineValue
            LinePoint(i.toFloat(), yVal)
        }

        LineChartData(
            xLabels = labels,
            series = listOf(
                LineSeries(
                    name = "实时波动指标",
                    points = points,
                    color = Color(0xFF73C0DE),
                    lineWidth = 1.5.dp,
                    symbol = SymbolType.None, // 大数据下关闭 Symbol 增强绘制速度
                    connectNulls = connectNulls
                )
            )
        )
    }

    val customStyle = style.copy(
        titleOptions = TitleOptions(
            text = "${pointsCount}点大数据量滑移测试",
            subtext = "二分裁剪仅渲染可视区以保帧率，支持双指缩放",
            textStyle = style.titleOptions.textStyle.copy(fontSize = 16.sp)
        ),
        xAxisOptions = style.xAxisOptions.copy(
            showLabels = true
        )
    )

    val viewportState = rememberViewportState(
        initialMinX = 100f,
        initialMaxX = 300f
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
            LineChart(
                data = data,
                style = customStyle,
                viewportState = viewportState,
                modifier = Modifier.fillMaxSize()
            )
        }

        ControlPanel {
            ControlSwitch(
                label = "空值自动跨段连接 (connectNulls)",
                checked = connectNulls,
                onCheckedChange = { connectNulls = it }
            )

            ControlSlider(
                label = "全局密集点总数 (pointsCount)",
                value = pointsCount.toFloat(),
                valueRange = 100f..1800f,
                onValueChange = { pointsCount = it.toInt() },
                valueFormatter = { "${it.toInt()} 个点" }
            )
        }
    }
}

/**
 * Demo 4: 折线图堆叠
 */
@Composable
fun DemoStackedLine(style: ChartStyle) {
    var isSmooth by remember { mutableStateOf(false) }
    var lineWidth by remember { mutableFloatStateOf(2f) }
    var showXGridLines by remember { mutableStateOf(true) }
    var showYGridLines by remember { mutableStateOf(true) }

    val labels = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    val stackKey = "Total"

    val seriesData = listOf(
        "邮件营销" to listOf(120f, 132f, 101f, 134f, 90f, 230f, 210f),
        "联盟广告" to listOf(220f, 182f, 191f, 234f, 290f, 330f, 310f),
        "视频广告" to listOf(150f, 232f, 201f, 154f, 190f, 330f, 410f),
        "直接访问" to listOf(320f, 332f, 301f, 334f, 390f, 330f, 320f),
        "搜索引擎" to listOf(820f, 932f, 901f, 934f, 1290f, 1330f, 1320f)
    )

    // ECharts 标准经典折线图配色
    val colors = listOf(
        Color(0xFF5470C6),
        Color(0xFF91CC75),
        Color(0xFFFAC858),
        Color(0xFFEE6666),
        Color(0xFF73C0DE)
    )

    val series = seriesData.mapIndexed { index, (name, values) ->
        LineSeries(
            name = name,
            points = values.mapIndexed { i, v -> LinePoint(i.toFloat(), v) },
            color = colors[index % colors.size],
            isSmooth = isSmooth,
            lineWidth = lineWidth.dp,
            drawArea = false,
            stack = stackKey,
            symbol = SymbolType.Circle,
            symbolSize = 5.dp
        )
    }

    val data = LineChartData(xLabels = labels, series = series)
    val customStyle = style.copy(
        titleOptions = TitleOptions(
            text = "折线图堆叠",
            subtext = "同组系列（stack）在各点的值依次向上累加对比",
            textStyle = style.titleOptions.textStyle.copy(fontSize = 16.sp)
        ),
        xAxisOptions = style.xAxisOptions.copy(showGridLines = showXGridLines),
        yAxisOptions = style.yAxisOptions.copy(showGridLines = showYGridLines)
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
            LineChart(
                data = data,
                style = customStyle,
                modifier = Modifier.fillMaxSize()
            )
        }

        ControlPanel {
            ControlSwitch(label = "贝塞尔平滑曲线 (Smooth)", checked = isSmooth, onCheckedChange = { isSmooth = it })
            ControlSwitch(label = "X 轴分割线 (showGridLines)", checked = showXGridLines, onCheckedChange = { showXGridLines = it })
            ControlSwitch(label = "Y 轴分割线 (showGridLines)", checked = showYGridLines, onCheckedChange = { showYGridLines = it })
            
            ControlSlider(
                label = "折线线条粗细 (lineWidth)",
                value = lineWidth,
                valueRange = 1f..6f,
                onValueChange = { lineWidth = it },
                valueFormatter = { String.format("%.1f dp", it) }
            )
        }
    }
}

/**
 * Demo 5: 堆叠面积图
 */
@Composable
fun DemoStackedArea(style: ChartStyle) {
    var isSmooth by remember { mutableStateOf(false) }
    var areaAlpha by remember { mutableFloatStateOf(0.5f) }
    var lineWidth by remember { mutableFloatStateOf(1.5f) }
    var showXGridLines by remember { mutableStateOf(true) }
    var showYGridLines by remember { mutableStateOf(true) }

    val labels = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    val stackKey = "Total"

    val seriesData = listOf(
        "邮件营销" to listOf(120f, 132f, 101f, 134f, 90f, 230f, 210f),
        "联盟广告" to listOf(220f, 182f, 191f, 234f, 290f, 330f, 310f),
        "视频广告" to listOf(150f, 232f, 201f, 154f, 190f, 330f, 410f),
        "直接访问" to listOf(320f, 332f, 301f, 334f, 390f, 330f, 320f),
        "搜索引擎" to listOf(820f, 932f, 901f, 934f, 1290f, 1330f, 1320f)
    )

    val colors = listOf(
        Color(0xFF5470C6),
        Color(0xFF91CC75),
        Color(0xFFFAC858),
        Color(0xFFEE6666),
        Color(0xFF73C0DE)
    )

    val series = seriesData.mapIndexed { index, (name, values) ->
        val color = colors[index % colors.size]
        LineSeries(
            name = name,
            points = values.mapIndexed { i, v -> LinePoint(i.toFloat(), v) },
            color = color,
            isSmooth = isSmooth,
            lineWidth = lineWidth.dp,
            drawArea = true,
            areaBrush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = areaAlpha), color.copy(alpha = areaAlpha * 0.2f))
            ),
            stack = stackKey,
            symbol = SymbolType.Circle,
            symbolSize = 4.dp
        )
    }

    val data = LineChartData(xLabels = labels, series = series)
    val customStyle = style.copy(
        titleOptions = TitleOptions(
            text = "堆叠面积图",
            subtext = "多层面积图堆叠填充，展现总量与占比关系",
            textStyle = style.titleOptions.textStyle.copy(fontSize = 16.sp)
        ),
        xAxisOptions = style.xAxisOptions.copy(showGridLines = showXGridLines),
        yAxisOptions = style.yAxisOptions.copy(showGridLines = showYGridLines)
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
            LineChart(
                data = data,
                style = customStyle,
                modifier = Modifier.fillMaxSize()
            )
        }

        ControlPanel {
            ControlSwitch(label = "贝塞尔平滑曲线 (Smooth)", checked = isSmooth, onCheckedChange = { isSmooth = it })
            ControlSwitch(label = "X 轴分割线 (showGridLines)", checked = showXGridLines, onCheckedChange = { showXGridLines = it })
            ControlSwitch(label = "Y 轴分割线 (showGridLines)", checked = showYGridLines, onCheckedChange = { showYGridLines = it })
            
            ControlSlider(
                label = "面积填充不透明度 (areaAlpha)",
                value = areaAlpha,
                valueRange = 0.1f..0.9f,
                onValueChange = { areaAlpha = it },
                valueFormatter = { String.format("%d%%", (it * 100).toInt()) }
            )

            ControlSlider(
                label = "折线线条粗细 (lineWidth)",
                value = lineWidth,
                valueRange = 0.5f..5f,
                onValueChange = { lineWidth = it },
                valueFormatter = { String.format("%.1f dp", it) }
            )
        }
    }
}

/**
 * Demo 6: 渐变堆叠面积图
 */
@Composable
fun DemoGradientStackedArea(style: ChartStyle) {
    var isSmooth by remember { mutableStateOf(true) }
    var areaAlpha by remember { mutableFloatStateOf(0.8f) }
    var showXGridLines by remember { mutableStateOf(true) }
    var showYGridLines by remember { mutableStateOf(true) }

    val labels = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    val stackKey = "GradientTotal"

    // 采用更具起伏的测试数据展现渐变的流线性
    val seriesData = listOf(
        "Line 1" to listOf(140f, 232f, 101f, 264f, 90f, 340f, 250f),
        "Line 2" to listOf(120f, 282f, 111f, 234f, 220f, 340f, 310f),
        "Line 3" to listOf(320f, 132f, 201f, 334f, 190f, 330f, 220f)
    )

    // 每一系列渐变的起点和终点色值
    val gradientColors = listOf(
        // Line 1: 绿到浅蓝
        listOf(Color(0xFF80FFA5), Color(0xFF00DDFF)),
        // Line 2: 浅蓝到深紫
        listOf(Color(0xFF37A2FF), Color(0xFF7756FF)),
        // Line 3: 玫红到大红
        listOf(Color(0xFFFF0087), Color(0xFFFF0059))
    )

    // 主线条对应的颜色（取渐变的终点色或特色主色，凸显对比度）
    val lineColors = listOf(
        Color(0xFF00DDFF),
        Color(0xFF37A2FF),
        Color(0xFFFF0087)
    )

    val series = seriesData.mapIndexed { index, (name, values) ->
        val gradPair = gradientColors[index]
        val lineColor = lineColors[index]
        
        LineSeries(
            name = name,
            points = values.mapIndexed { i, v -> LinePoint(i.toFloat(), v) },
            color = lineColor,
            isSmooth = isSmooth,
            lineWidth = 2.dp,
            drawArea = true,
            areaBrush = Brush.verticalGradient(
                colors = listOf(
                    gradPair[0].copy(alpha = areaAlpha),
                    gradPair[1].copy(alpha = areaAlpha * 0.1f)
                )
            ),
            stack = stackKey,
            symbol = SymbolType.Circle,
            symbolSize = 4.dp
        )
    }

    val data = LineChartData(xLabels = labels, series = series)
    val customStyle = style.copy(
        titleOptions = TitleOptions(
            text = "渐变堆叠面积图",
            subtext = "渐变色彩堆叠面积，对标 ECharts 高颜值视觉样式",
            textStyle = style.titleOptions.textStyle.copy(fontSize = 16.sp)
        ),
        xAxisOptions = style.xAxisOptions.copy(showGridLines = showXGridLines),
        yAxisOptions = style.yAxisOptions.copy(showGridLines = showYGridLines)
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
            LineChart(
                data = data,
                style = customStyle,
                modifier = Modifier.fillMaxSize()
            )
        }

        ControlPanel {
            ControlSwitch(label = "贝塞尔平滑曲线 (Smooth)", checked = isSmooth, onCheckedChange = { isSmooth = it })
            ControlSwitch(label = "X 轴分割线 (showGridLines)", checked = showXGridLines, onCheckedChange = { showXGridLines = it })
            ControlSwitch(label = "Y 轴分割线 (showGridLines)", checked = showYGridLines, onCheckedChange = { showYGridLines = it })
            
            ControlSlider(
                label = "渐变初始不透明度 (areaAlpha)",
                value = areaAlpha,
                valueRange = 0.1f..0.9f,
                onValueChange = { areaAlpha = it },
                valueFormatter = { String.format("%d%%", (it * 100).toInt()) }
            )
        }
    }
}

/**
 * Demo 7: 置信区间带图 (Confidence Band)
 */
@Composable
fun DemoConfidenceBand(style: ChartStyle) {
    var isSmooth by remember { mutableStateOf(true) }
    var bandAlpha by remember { mutableFloatStateOf(0.18f) }
    var lineWidth by remember { mutableFloatStateOf(2.5f) }
    var showXGridLines by remember { mutableStateOf(true) }
    var showYGridLines by remember { mutableStateOf(true) }

    var useScrollableMode by remember { mutableStateOf(false) }
    var visibleCount by remember { mutableFloatStateOf(5f) }
    var enablePan by remember { mutableStateOf(true) }
    var enableZoom by remember { mutableStateOf(true) }

    val viewportState = rememberViewportState()
    viewportState.viewportMode = if (useScrollableMode) ViewportMode.Scrollable(visibleCount.toInt()) else ViewportMode.Fit
    viewportState.enablePan = enablePan
    viewportState.enableZoom = enableZoom

    val labels = listOf("7-01", "7-02", "7-03", "7-04", "7-05", "7-06", "7-07", "7-08", "7-09", "7-10")
    
    // 主均线数据
    val valueMain = listOf(22f, 24f, 21f, 26f, 25f, 29f, 27f, 31f, 28f, 32f)
    
    // 偏差数据（上下非对称偏差）
    val upperOffset = listOf(2.5f, 3.1f, 2.2f, 3.5f, 2.8f, 4.0f, 3.2f, 3.8f, 3.0f, 4.2f)
    val lowerOffset = listOf(2.1f, 2.8f, 1.8f, 3.0f, 2.5f, 3.5f, 2.9f, 3.2f, 2.6f, 3.8f)

    val pointsMain = valueMain.mapIndexed { i, v -> LinePoint(i.toFloat(), v) }
    val pointsUpper = valueMain.mapIndexed { i, v -> LinePoint(i.toFloat(), v + upperOffset[i]) }
    val pointsLower = valueMain.mapIndexed { i, v -> LinePoint(i.toFloat(), v - lowerOffset[i]) }

    val series = listOf(
        // Series 0: 主测量线
        LineSeries(
            name = "模拟主测量值",
            points = pointsMain,
            color = Color(0xFF1890FF), // 经典科技蓝
            isSmooth = isSmooth,
            lineWidth = lineWidth.dp,
            drawArea = false,
            symbol = SymbolType.Circle,
            symbolSize = 5.dp
        ),
        // Series 1: 置信上限线
        LineSeries(
            name = "置信上限 (Upper)",
            points = pointsUpper,
            color = Color(0xFFB0C4DE).copy(alpha = 0.8f),
            isSmooth = isSmooth,
            lineWidth = 1.dp,
            lineStyle = LineStyleType.Dashed,
            drawArea = false,
            symbol = SymbolType.None
        ),
        // Series 2: 置信下限线
        LineSeries(
            name = "置信下限 (Lower)",
            points = pointsLower,
            color = Color(0xFFB0C4DE).copy(alpha = 0.8f),
            isSmooth = isSmooth,
            lineWidth = 1.dp,
            lineStyle = LineStyleType.Dashed,
            drawArea = false,
            symbol = SymbolType.None
        )
    )

    // 配置置信区间带
    val rangeArea = RangeAreaOptions(
        upperSeriesIndex = 1,
        lowerSeriesIndex = 2,
        fillColor = Color(0xFF73C0DE).copy(alpha = bandAlpha) // 浅彩色半透明包络
    )

    val data = LineChartData(xLabels = labels, series = series)
    val customStyle = style.copy(
        titleOptions = TitleOptions(
            text = "置信区间带阴影图",
            subtext = "主测量折线环绕半透明置信度区间带 (Confidence Band)",
            textStyle = style.titleOptions.textStyle.copy(fontSize = 16.sp)
        ),
        xAxisOptions = style.xAxisOptions.copy(showGridLines = showXGridLines),
        yAxisOptions = style.yAxisOptions.copy(showGridLines = showYGridLines)
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
            LineChart(
                data = data,
                style = customStyle,
                viewportState = viewportState,
                rangeAreaOptions = rangeArea,
                modifier = Modifier.fillMaxSize()
            )
        }

        ControlPanel {
            ControlSwitch(label = "贝塞尔平滑曲线 (Smooth)", checked = isSmooth, onCheckedChange = { isSmooth = it })
            ControlSwitch(label = "X 轴分割线 (showGridLines)", checked = showXGridLines, onCheckedChange = { showXGridLines = it })
            ControlSwitch(label = "Y 轴分割线 (showGridLines)", checked = showYGridLines, onCheckedChange = { showYGridLines = it })

            ControlSlider(
                label = "置信度阴影不透明度 (bandAlpha)",
                value = bandAlpha,
                valueRange = 0.05f..0.5f,
                onValueChange = { bandAlpha = it },
                valueFormatter = { String.format("%d%%", (it * 100).toInt()) }
            )

            ControlSlider(
                label = "主折线线条粗细 (lineWidth)",
                value = lineWidth,
                valueRange = 1f..6f,
                onValueChange = { lineWidth = it },
                valueFormatter = { String.format("%.1f dp", it) }
            )

            ControlSwitch(label = "只看一部分、滑动查看 (Scrollable Mode)", checked = useScrollableMode, onCheckedChange = { useScrollableMode = it })
            if (useScrollableMode) {
                ControlSlider(
                    label = "可视范围点数 (visibleCount)",
                    value = visibleCount,
                    valueRange = 3f..10f,
                    onValueChange = { visibleCount = it },
                    valueFormatter = { String.format("%d 点", it.toInt()) }
                )
            }
            ControlSwitch(label = "启用拖拽平移 (Enable Pan)", checked = enablePan, onCheckedChange = { enablePan = it })
            ControlSwitch(label = "启用捏合缩放 (Enable Zoom)", checked = enableZoom, onCheckedChange = { enableZoom = it })
        }
    }
}

