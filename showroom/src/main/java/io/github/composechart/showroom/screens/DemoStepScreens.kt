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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.composechart.charts.line.LineChart
import io.github.composechart.charts.line.LineChartData
import io.github.composechart.charts.line.LinePoint
import io.github.composechart.charts.line.LineSeries
import io.github.composechart.charts.line.LineStyleType
import io.github.composechart.charts.line.StepType
import io.github.composechart.charts.line.SymbolType
import io.github.composechart.charts.line.MarkArea
import io.github.composechart.charts.bar.BarChart
import io.github.composechart.charts.bar.BarChartData
import io.github.composechart.charts.bar.BarSeries
import io.github.composechart.charts.bar.BarValue
import io.github.composechart.charts.pie.PieChart
import io.github.composechart.charts.pie.PieChartData
import io.github.composechart.charts.pie.PieSlice
import io.github.composechart.charts.polar.PolarBarChart
import io.github.composechart.charts.polar.PolarChartData
import io.github.composechart.charts.polar.PolarBarSeries
import io.github.composechart.charts.polar.PolarBarValue
import io.github.composechart.charts.polar.PolarLabelPosition
import io.github.composechart.core.state.ViewportMode
import io.github.composechart.core.state.ViewportState
import io.github.composechart.core.state.rememberViewportState
import io.github.composechart.core.style.ChartStyle
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import io.github.composechart.core.style.GridLineStyle
import io.github.composechart.core.style.IndicatorStyle
import io.github.composechart.core.style.TitleOptions
import io.github.composechart.core.style.TooltipOptions
import kotlinx.coroutines.delay
import kotlin.math.sin

/**
 * 演示：多 X 轴雨量对比 (Multiple X Axes)
 */
@Composable
fun DemoMultipleXAxes(style: ChartStyle) {
    val labels2016 = listOf("2016-1", "2016-3", "2016-5", "2016-7", "2016-9", "2016-11")
    val labels2015 = listOf("2015-1", "2015-3", "2015-5", "2015-7", "2015-9", "2015-11")

    val series = listOf(
        LineSeries(
            name = "Precipitation(2016)",
            points = listOf(2.6f, 9.0f, 28.7f, 175.6f, 48.7f, 6.0f).mapIndexed { i, v -> LinePoint(i.toFloat(), v) },
            color = Color(0xFF5470C6),
            isSmooth = true,
            xAxisIndex = 0 // 绑定底部 X 轴
        ),
        LineSeries(
            name = "Precipitation(2015)",
            points = listOf(2.0f, 7.0f, 25.6f, 135.6f, 32.6f, 6.4f).mapIndexed { i, v -> LinePoint(i.toFloat(), v) },
            color = Color(0xFFEE6666),
            isSmooth = true,
            xAxisIndex = 1 // 绑定顶部 X2 轴
        )
    )

    val customStyle = style.copy(
        titleOptions = TitleOptions(
            text = "双 X 轴雨量变化对比",
            subtext = "上下两套 X 类目轴，各自绑定 2015 和 2016 数据线",
            textStyle = style.titleOptions.textStyle.copy(fontSize = 16.sp)
        ),
        xAxisOptions = style.xAxisOptions.copy(show = true), // 底部 X 轴可见
        x2AxisOptions = style.xAxisOptions.copy(show = true) // 顶部 X2 轴可见
    )

    val data = LineChartData(xLabels = labels2016, series = series)

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
            LineChart(
                data = data,
                style = customStyle,
                x2Labels = labels2015,
                modifier = Modifier.fillMaxSize()
            )
        }

        ControlPanel {
            Text(
                text = "双 X 轴演示说明：\n1. 顶部的 2015 刻度轴与底部的 2016 刻度轴完全平行分布。\n2. 红色折线点对应顶轴刻度，蓝色点对应底轴刻度。\n3. 长按触发 Tooltip 十字扫掠时，可同时读取到上下两个对应月份的雨量。",
                fontSize = 12.sp,
                color = Color.Gray,
                lineHeight = 16.sp
            )
        }
    }
}

/**
 * 演示：阶梯折线图 (Step Line)
 */
@Composable
fun DemoStepLine(style: ChartStyle) {
    var lineStyle by remember { mutableStateOf(LineStyleType.Solid) }
    var lineWidth by remember { mutableFloatStateOf(2.5f) }

    val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    val series = listOf(
        LineSeries(
            name = "Step Start (起点拐弯)",
            points = listOf(120f, 132f, 101f, 134f, 90f, 230f, 210f).mapIndexed { i, v -> LinePoint(i.toFloat(), v) },
            color = Color(0xFF5470C6),
            lineWidth = lineWidth.dp,
            lineStyle = lineStyle,
            stepType = StepType.Start,
            symbol = SymbolType.Circle,
            symbolSize = 5.dp
        ),
        LineSeries(
            name = "Step Middle (中点拐弯)",
            points = listOf(220f, 282f, 201f, 234f, 290f, 430f, 410f).mapIndexed { i, v -> LinePoint(i.toFloat(), v) },
            color = Color(0xFF91CC75),
            lineWidth = lineWidth.dp,
            lineStyle = lineStyle,
            stepType = StepType.Middle,
            symbol = SymbolType.Circle,
            symbolSize = 5.dp
        ),
        LineSeries(
            name = "Step End (终点拐弯)",
            points = listOf(450f, 432f, 401f, 454f, 590f, 530f, 510f).mapIndexed { i, v -> LinePoint(i.toFloat(), v) },
            color = Color(0xFFFAC858),
            lineWidth = lineWidth.dp,
            lineStyle = lineStyle,
            stepType = StepType.End,
            symbol = SymbolType.Circle,
            symbolSize = 5.dp
        )
    )

    val data = LineChartData(xLabels = labels, series = series)
    val customStyle = style.copy(
        titleOptions = TitleOptions(
            text = "阶梯折线图 (Step Line)",
            subtext = "常用于展示非连续、阶段性剧烈跃变的数据走势",
            textStyle = style.titleOptions.textStyle.copy(fontSize = 16.sp)
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
            LineChart(
                data = data,
                style = customStyle,
                modifier = Modifier.fillMaxSize()
            )
        }

        ControlPanel {
            ControlSlider(
                label = "折线线条粗细 (lineWidth)",
                value = lineWidth,
                valueRange = 1f..6f,
                onValueChange = { lineWidth = it },
                valueFormatter = { String.format("%.1f dp", it) }
            )

            ControlSelector(
                label = "折线虚实样式 (lineStyle)",
                options = listOf(LineStyleType.Solid, LineStyleType.Dashed),
                selectedOption = lineStyle,
                onOptionSelected = { lineStyle = it },
                optionLabel = { if (it == LineStyleType.Solid) "实线 (Solid)" else "虚线 (Dashed)" }
            )
        }
    }
}

/**
 * 演示：动态排序赛跑折线图 (Line Race)
 */
@Composable
fun DemoLineRace(style: ChartStyle) {
    var isPlaying by remember { mutableStateOf(false) }
    var currentPointsCount by remember { mutableIntStateOf(3) }
    var speedMs by remember { mutableFloatStateOf(800f) }

    val years = listOf("1950", "1960", "1970", "1980", "1990", "2000", "2010", "2020")

    val fullGermany = listOf(1200f, 1800f, 2500f, 3200f, 3800f, 4100f, 4500f, 4900f)
    val fullFrance = listOf(900f, 1400f, 2100f, 2800f, 3300f, 3600f, 3900f, 4200f)
    val fullUK = listOf(1000f, 1500f, 2200f, 2900f, 3400f, 3700f, 4000f, 4400f)
    val fullItaly = listOf(800f, 1200f, 1800f, 2400f, 2900f, 3100f, 3300f, 3600f)
    val fullSpain = listOf(600f, 900f, 1400f, 1900f, 2400f, 2700f, 2900f, 3100f)

    // 定时步进协程
    LaunchedEffect(isPlaying, speedMs) {
        if (isPlaying) {
            while (true) {
                delay(speedMs.toLong())
                if (currentPointsCount < years.size) {
                    currentPointsCount++
                } else {
                    currentPointsCount = 3 // 循环重头播
                }
            }
        }
    }

    // 根据当前的展示长度截断数据点
    val series = listOf(
        LineSeries(
            name = "Germany",
            points = fullGermany.take(currentPointsCount).mapIndexed { i, v -> LinePoint(i.toFloat(), v) },
            color = Color(0xFF5470C6),
            isSmooth = true,
            showEndLabel = true
        ),
        LineSeries(
            name = "France",
            points = fullFrance.take(currentPointsCount).mapIndexed { i, v -> LinePoint(i.toFloat(), v) },
            color = Color(0xFF91CC75),
            isSmooth = true,
            showEndLabel = true
        ),
        LineSeries(
            name = "United Kingdom",
            points = fullUK.take(currentPointsCount).mapIndexed { i, v -> LinePoint(i.toFloat(), v) },
            color = Color(0xFFFAC858),
            isSmooth = true,
            showEndLabel = true
        ),
        LineSeries(
            name = "Italy",
            points = fullItaly.take(currentPointsCount).mapIndexed { i, v -> LinePoint(i.toFloat(), v) },
            color = Color(0xFFEE6666),
            isSmooth = true,
            showEndLabel = true
        ),
        LineSeries(
            name = "Spain",
            points = fullSpain.take(currentPointsCount).mapIndexed { i, v -> LinePoint(i.toFloat(), v) },
            color = Color(0xFF73C0DE),
            isSmooth = true,
            showEndLabel = true
        )
    )

    val data = LineChartData(xLabels = years, series = series)
    val customStyle = style.copy(
        titleOptions = TitleOptions(
            text = "动态国家收入排序赛跑",
            subtext = "折线伴随终点悬挂标签，重叠时自动触发物理间距避让",
            textStyle = style.titleOptions.textStyle.copy(fontSize = 16.sp)
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
            LineChart(
                data = data,
                style = customStyle,
                modifier = Modifier.fillMaxSize()
            )
        }

        ControlPanel {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { isPlaying = !isPlaying },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = if (isPlaying) "暂停播放" else "开始播放")
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = {
                        isPlaying = false
                        currentPointsCount = 3
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "重置")
                }
            }

            ControlSlider(
                label = "当前赛跑年份跨度 (1950 - 2020)",
                value = currentPointsCount.toFloat(),
                valueRange = 3f..8f,
                onValueChange = {
                    isPlaying = false
                    currentPointsCount = it.toInt()
                },
                valueFormatter = { years.getOrNull(it.toInt() - 1) ?: "无" }
            )

            ControlSlider(
                label = "动态推进刷新速率 (speedMs)",
                value = speedMs,
                valueRange = 200f..1500f,
                onValueChange = { speedMs = it },
                valueFormatter = { String.format("%.0f ms", it) }
            )
        }
    }
}

/**
 * 演示：数学函数绘图与中央十字相交 (Function Plot / Origin Cross)
 */
@Composable
fun DemoFunctionPlot(style: ChartStyle) {
    var functionType by remember { mutableStateOf(0) } // 0: ECharts 经典波动, 1: sin(x)*x, 2: 正弦衰减

    val points = remember(functionType) {
        when (functionType) {
            0 -> {
                val start = -50f
                val end = 50f
                val count = 1000
                val step = (end - start) / count
                (0..count).map { i ->
                    val x = start + i * step
                    val xDiv = x / 10f
                    val y = sin(xDiv) * kotlin.math.cos(xDiv * 2f + 1f) * sin(xDiv * 3f) * 50f
                    LinePoint(x, y)
                }
            }
            1 -> {
                val start = -15f
                val end = 15f
                val count = 300
                val step = (end - start) / count
                (0..count).map { i ->
                    val x = start + i * step
                    LinePoint(x, sin(x) * x)
                }
            }
            else -> {
                val start = 0f
                val end = 50f
                val count = 500
                val step = (end - start) / count
                (0..count).map { i ->
                    val x = start + i * step
                    val y = sin(x) * kotlin.math.exp(-0.1f * x) * 10f
                    LinePoint(x, y)
                }
            }
        }
    }

    val xAxisTicks = remember(functionType) {
        when (functionType) {
            0 -> listOf(-50f, -40f, -30f, -20f, -10f, 0f, 10f, 20f, 30f, 40f, 50f)
            1 -> listOf(-15f, -10f, -5f, 0f, 5f, 10f, 15f)
            else -> listOf(0f, 10f, 20f, 30f, 40f, 50f)
        }
    }

    val seriesName = remember(functionType) {
        when (functionType) {
            0 -> "y = sin(x/10) * cos(2x/10 + 1) * sin(3x/10) * 50"
            1 -> "y = sin(x) * x"
            else -> "y = sin(x) * e^(-0.1x) * 10"
        }
    }

    val series = listOf(
        LineSeries(
            name = seriesName,
            points = points,
            color = Color(0xFF5470C6),
            isSmooth = true,
            lineWidth = 3.dp,
            symbol = SymbolType.None
        )
    )

    val viewportState = remember(functionType) {
        when (functionType) {
            0 -> ViewportState(initialMinX = -20f, initialMaxX = 20f)
            1 -> ViewportState(initialMinX = -15f, initialMaxX = 15f)
            else -> ViewportState(initialMinX = 0f, initialMaxX = 25f)
        }
    }

    val customStyle = style.copy(
        titleOptions = TitleOptions(
            text = "数学公式函数曲线绘图",
            subtext = "X/Y轴锁定在零值 onZero 中央十字交叉，自适应手势视口降级",
            textStyle = style.titleOptions.textStyle.copy(fontSize = 16.sp)
        ),
        xAxisOptions = style.xAxisOptions.copy(
            show = true,
            onZero = true,
            showGridLines = true,
            gridLineStyle = GridLineStyle.Dashed
        ),
        yAxisOptions = style.yAxisOptions.copy(
            show = true,
            onZero = true,
            showGridLines = true,
            gridLineStyle = GridLineStyle.Dashed
        )
    )

    val data = LineChartData(xLabels = emptyList(), series = series)

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
            LineChart(
                data = data,
                style = customStyle,
                viewportState = viewportState,
                xAxisTicks = xAxisTicks,
                modifier = Modifier.fillMaxSize()
            )
        }

        ControlPanel {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { functionType = 0 },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "经典波动", fontSize = 11.sp)
                }
                Button(
                    onClick = { functionType = 1 },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "正弦自激", fontSize = 11.sp)
                }
                Button(
                    onClick = { functionType = 2 },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "正弦衰减", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "函数图表演示说明：\n1. X轴和Y轴均为连续数值轴（没有类目名称限制，全数值转换）。\n2. X轴主轴线精准穿过 Y = 0f 像素面；Y轴主轴线精准穿过 X = 0f 像素面，在中央实现十字轴交叉。\n3. 您可以尝试两指手势缩放或滑动，当中央原点 (0,0) 移出可见区域时，轴刻度与轴线会自动降级贴到边界，绝对不会脱离可视区，保证了绝佳的手势阻尼防断联体验。",
                fontSize = 12.sp,
                color = Color.Gray,
                lineHeight = 16.sp
            )
        }
    }
}

/**
 * 演示：品牌热度周排名消长凹凸图 (Bump Chart)
 */
@Composable
fun DemoBumpChart(style: ChartStyle) {
    val weeks = listOf("Week 1", "Week 2", "Week 3", "Week 4", "Week 5", "Week 6")

    val series = listOf(
        LineSeries(
            name = "品牌 A",
            points = listOf(1f, 2f, 2f, 1f, 3f, 2f).mapIndexed { i, v -> LinePoint(i.toFloat(), v) },
            color = Color(0xFF5470C6),
            isSmooth = true,
            lineWidth = 3.dp,
            symbol = SymbolType.Circle,
            symbolSize = 10.dp,
            showSymbolLabel = true
        ),
        LineSeries(
            name = "品牌 B",
            points = listOf(2f, 1f, 3f, 4f, 2f, 1f).mapIndexed { i, v -> LinePoint(i.toFloat(), v) },
            color = Color(0xFF91CC75),
            isSmooth = true,
            lineWidth = 3.dp,
            symbol = SymbolType.Circle,
            symbolSize = 10.dp,
            showSymbolLabel = true
        ),
        LineSeries(
            name = "品牌 C",
            points = listOf(3f, 4f, 1f, 2f, 1f, 3f).mapIndexed { i, v -> LinePoint(i.toFloat(), v) },
            color = Color(0xFFFAC858),
            isSmooth = true,
            lineWidth = 3.dp,
            symbol = SymbolType.Circle,
            symbolSize = 10.dp,
            showSymbolLabel = true
        ),
        LineSeries(
            name = "品牌 D",
            points = listOf(4f, 3f, 5f, 3f, 5f, 4f).mapIndexed { i, v -> LinePoint(i.toFloat(), v) },
            color = Color(0xFFEE6666),
            isSmooth = true,
            lineWidth = 3.dp,
            symbol = SymbolType.Circle,
            symbolSize = 10.dp,
            showSymbolLabel = true
        ),
        LineSeries(
            name = "品牌 E",
            points = listOf(5f, 5f, 4f, 5f, 4f, 5f).mapIndexed { i, v -> LinePoint(i.toFloat(), v) },
            color = Color(0xFF73C0DE),
            isSmooth = true,
            lineWidth = 3.dp,
            symbol = SymbolType.Circle,
            symbolSize = 10.dp,
            showSymbolLabel = true
        )
    )

    val customStyle = style.copy(
        titleOptions = TitleOptions(
            text = "品牌周热度排名变化 (Bump Chart)",
            subtext = "Y轴已反转（1位在顶，5位在底），圆点内置名次数字标定",
            textStyle = style.titleOptions.textStyle.copy(fontSize = 16.sp)
        ),
        yAxisOptions = style.yAxisOptions.copy(
            inverse = true, // 开启 Y 轴反向映射
            labelFormatter = { "${it.toInt()} 位" }
        )
    )

    val data = LineChartData(xLabels = weeks, series = series)

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
            LineChart(
                data = data,
                style = customStyle,
                modifier = Modifier.fillMaxSize()
            )
        }

        ControlPanel {
            Text(
                text = "凹凸图演示说明：\n1. Y 轴刻度呈倒向排列（1 位在最上方，5 位在最下方），完美契合排名趋势展示。\n2. 各品牌折线转折圆圈内部，直接动态居中渲染出当前周次的名次序号，信息读取极其直观。\n3. 支持点击顶部图例来单选/多选过滤对比品牌，折线可根据手势流畅平移与缩放。",
                fontSize = 12.sp,
                color = Color.Gray,
                lineHeight = 16.sp
            )
        }
    }
}

/**
 * 演示：警戒分带与高峰标注 (MarkArea)
 */
@Composable
fun DemoMarkArea(style: ChartStyle) {
    val times = listOf("00:00", "04:00", "08:00", "12:00", "16:00", "20:00", "24:00")

    // Chart 1: AQI 变化与横向警戒带
    val aqiSeries = listOf(
        LineSeries(
            name = "实时 AQI 监测",
            points = listOf(45f, 35f, 65f, 125f, 95f, 55f, 40f).mapIndexed { i, v -> LinePoint(i.toFloat(), v) },
            color = Color(0xFF5470C6),
            lineWidth = 3.dp,
            isSmooth = true
        )
    )

    val aqiMarkAreas = listOf(
        MarkArea(
            startY = 0f,
            endY = 50f,
            color = Color(0xFF91CC75).copy(alpha = 0.15f),
            label = "优 (0-50)"
        ),
        MarkArea(
            startY = 50f,
            endY = 100f,
            color = Color(0xFFFAC858).copy(alpha = 0.15f),
            label = "良 (50-100)"
        ),
        MarkArea(
            startY = 100f,
            endY = 150f,
            color = Color(0xFFEE6666).copy(alpha = 0.15f),
            label = "轻度污染 (100-150)"
        )
    )

    val aqiStyle = style.copy(
        titleOptions = TitleOptions(
            text = "北京 AQI 空气质量日变化",
            subtext = "背景横向分带填充警戒色（绿/黄/红），表示优良差级别",
            textStyle = style.titleOptions.textStyle.copy(fontSize = 16.sp)
        )
    )

    // Chart 2: 用电负荷走势与纵向高峰时段标注
    val loadSeries = listOf(
        LineSeries(
            name = "电网用电负荷 (MW)",
            points = listOf(1.8f, 1.3f, 3.2f, 6.8f, 7.2f, 4.5f, 2.2f).mapIndexed { i, v -> LinePoint(i.toFloat(), v) },
            color = Color(0xFF73C0DE),
            lineWidth = 3.dp,
            isSmooth = true
        )
    )

    val loadMarkAreas = listOf(
        MarkArea(
            startX = 3f, // 对应 12:00
            endX = 5f,   // 对应 20:00
            color = Color(0xFF73C0DE).copy(alpha = 0.15f),
            label = "用电高峰时段 (12:00 - 20:00)",
            labelColor = Color(0xFF5470C6)
        )
    )

    val loadStyle = style.copy(
        titleOptions = TitleOptions(
            text = "工业用电量与高峰时段标注",
            subtext = "背景纵向填充（12:00 - 20:00），直观定位高峰负荷区间",
            textStyle = style.titleOptions.textStyle.copy(fontSize = 16.sp)
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    ) {
        // 第一张图：AQI (横向分带)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
        ) {
            LineChart(
                data = LineChartData(xLabels = times, series = aqiSeries),
                style = aqiStyle,
                markAreas = aqiMarkAreas,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 第二张图：用电量 (纵向高峰)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
        ) {
            LineChart(
                data = LineChartData(xLabels = times, series = loadSeries),
                style = loadStyle,
                markAreas = loadMarkAreas,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        ControlPanel {
            Text(
                text = "警戒色带 (MarkArea) 演示说明：\n1. 支持横向标注（锁定 Y 值区间，如 AQI 优良等级色带，文字贴在网格右上方）。\n2. 支持纵向标注（锁定 X 值类目区间，如日间用电高峰，填充色块随 X 轴手势移动同步移动缩放）。\n3. 警戒带完全绘制于坐标线网格与折线图背景后层，文字清晰可读，完全还原 ECharts 警戒区特效。",
                fontSize = 12.sp,
                color = Color.Gray,
                lineHeight = 16.sp
            )
        }
    }
}

/**
 * 演示：交错正负轴条形图 (Bar Chart with Negative Value)
 */
@Composable
fun DemoBarNegativeValue(style: ChartStyle) {
    val labels = listOf("ten", "nine", "eight", "seven", "six", "five", "four", "three", "two", "one")
    
    val series = listOf(
        BarSeries(
            name = "Cost/Income",
            values = listOf(
                BarValue(-0.07f), BarValue(-0.09f), BarValue(0.2f), BarValue(0.44f), 
                BarValue(-0.23f), BarValue(0.08f), BarValue(-0.1f), BarValue(0.47f), 
                BarValue(-0.36f), BarValue(0.18f)
            ),
            color = Color(0xFF5470C6),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(0f, 0f)
        )
    )

    val customStyle = style.copy(
        titleOptions = TitleOptions(
            text = "交错正负轴标签条形图",
            subtext = "零轴上的分类标签自动偏置避让，防止与柱体物理重合",
            textStyle = style.titleOptions.textStyle.copy(fontSize = 16.sp)
        ),
        xAxisOptions = style.xAxisOptions.copy(
            show = true,
            onZero = true,
            showGridLines = true
        ),
        yAxisOptions = style.yAxisOptions.copy(
            show = true,
            onZero = true, // 开启 Y 轴 onZero
            labelOnZero = true, // 标签跟随零轴偏置避让
            showGridLines = false
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
            BarChart(
                data = BarChartData(xLabels = labels, series = series),
                style = customStyle,
                horizontal = true, // 水平条形图
                modifier = Modifier.fillMaxSize()
            )
        }

        ControlPanel {
            Text(
                text = "交错轴演示说明：\n1. 分类轴（Y 轴）轴线在 X = 0f 零线上相交。\n2. 分类标签在数值为正时偏左、在数值为负时偏右，自适应避开柱子的伸展方向，展现出极佳的可读性与美感。",
                fontSize = 12.sp,
                color = Color.Gray,
                lineHeight = 16.sp
            )
        }
    }
}

/**
 * 演示：瀑布图 (Waterfall Chart)
 */
@Composable
fun DemoBarWaterfall(style: ChartStyle) {
    val labels = listOf("Total", "Rent", "Utilities", "Transportation", "Meals", "Other")

    val series = listOf(
        BarSeries(
            name = "Living Expenses",
            values = listOf(
                BarValue(value = 2900f), // Total
                BarValue(value = 2900f, baseValue = 1700f), // Rent
                BarValue(value = 1700f, baseValue = 1400f), // Utilities
                BarValue(value = 1400f, baseValue = 1200f), // Transportation
                BarValue(value = 1200f, baseValue = 300f), // Meals
                BarValue(value = 300f) // Other
            ),
            color = Color(0xFF5470C6),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(0f, 0f)
        )
    )

    val customStyle = style.copy(
        titleOptions = TitleOptions(
            text = "深圳居民单月日常消费支出",
            subtext = "瀑布图模拟：通过 baseValue 支撑柱体悬空展现增减过程",
            textStyle = style.titleOptions.textStyle.copy(fontSize = 16.sp)
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
            BarChart(
                data = BarChartData(xLabels = labels, series = series),
                style = customStyle,
                modifier = Modifier.fillMaxSize()
            )
        }

        ControlPanel {
            Text(
                text = "瀑布图演示说明：\n1. 中段的 Rent, Utilities 等柱体底部并不在零线上，而是通过 baseValue 指定物理基准面悬空呈现。\n2. 生长动画与 Tooltip 完美匹配悬空范围，不需要设置复杂的透明占位柱，极其整洁。",
                fontSize = 12.sp,
                color = Color.Gray,
                lineHeight = 16.sp
            )
        }
    }
}

/**
 * 演示：渐变阴影与点击缩放 (Gradient, Shadow & Click-to-Zoom)
 */
@Composable
fun DemoBarGradientZoom(style: ChartStyle) {
    val labels = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    val series = listOf(
        BarSeries(
            name = "Sales",
            values = listOf(220f, 180f, 350f, 290f, 380f, 480f, 310f, 210f, 180f, 450f, 320f, 380f).map { BarValue(it) },
            color = Color(0xFFFAC858),
            gradientBrush = Brush.linearGradient(
                colors = listOf(Color(0xFF83FFF4), Color(0xFF3FD1FF), Color(0xFF188DF0))
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
            shadowColor = Color(0x88188DF0),
            shadowBlur = 16f,
            shadowOffset = Offset(0f, 4f)
        )
    )

    val customStyle = style.copy(
        titleOptions = TitleOptions(
            text = "销售数据渐变与点击聚焦",
            subtext = "柱体配有发光阴影与平滑渐变，点击任意柱子启动视口放大聚焦",
            textStyle = style.titleOptions.textStyle.copy(fontSize = 16.sp)
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
            BarChart(
                data = BarChartData(xLabels = labels, series = series),
                style = customStyle,
                clickToZoom = true, // 点击柱子放大聚焦
                modifier = Modifier.fillMaxSize()
            )
        }

        ControlPanel {
            Text(
                text = "渐变与聚焦演示说明：\n1. 柱体自上而下拥有高颜值的蓝色线性渐变，且周围渲染出一层半透明的霓虹发光阴影外壳，极具视觉冲击力。\n2. **点击柱子** 或网格内任意类目区域，视口将平滑运动缩放到该柱子周围，再次点击/拖拽可恢复，带来极佳的微交互体验。",
                fontSize = 12.sp,
                color = Color.Gray,
                lineHeight = 16.sp
            )
        }
    }
}

/**
 * 演示：极坐标径向扇形图 (Polar Radial Bar Chart)
 */
@Composable
fun DemoPolarRadialBar(style: ChartStyle) {
    val labels = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    val series = listOf(
        PolarBarSeries(
            name = "Attendance",
            values = listOf(12f, 18f, 25f, 15f, 32f, 28f, 10f).map { PolarBarValue(it) },
            color = Color(0xFF5470C6),
            labelPosition = PolarLabelPosition.Middle
        )
    )

    val customStyle = style.copy(
        titleOptions = TitleOptions(
            text = "极坐标周考勤率 (Radial Polar Bar)",
            subtext = "不闭合极轴 (270度)，径向中置文本对齐排版",
            textStyle = style.titleOptions.textStyle.copy(fontSize = 16.sp)
        ),
        polarOptions = style.polarOptions.copy(
            startAngle = -90f, // 12点钟方向
            endAngle = 180f,   // 扫掠270度
            radiusStepNumber = 4
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
            PolarBarChart(
                data = PolarChartData(xLabels = labels, series = series),
                style = customStyle,
                horizontal = false, // Radial 模式 (分类在角度，数值在半径)
                modifier = Modifier.fillMaxSize()
            )
        }

        ControlPanel {
            Text(
                text = "极坐标扇区演示说明：\n1. X轴（分类）是角度轴，从 12点钟顺时针延伸 270 度，Y轴（数值）是半径轴。\n2. 各分类绘制为圆心发散的扇区结构，文本沿着扇区射线的物理角度动态旋转并居中（Middle）显示，且左侧扇区的文本自动颠倒 180 度防止头悬空，可读性完美。",
                fontSize = 12.sp,
                color = Color.Gray,
                lineHeight = 16.sp
            )
        }
    }
}

/**
 * 演示：极坐标切向圆环图 (Polar Tangential Bar Chart)
 */
@Composable
fun DemoPolarTangentialBar(style: ChartStyle) {
    val labels = listOf("Question A", "Question B", "Question C", "Question D")

    val series = listOf(
        PolarBarSeries(
            name = "Correct Rate",
            values = listOf(85f, 62f, 95f, 45f).map { PolarBarValue(it) },
            color = Color(0xFF3BA272),
            labelPosition = PolarLabelPosition.Middle
        )
    )

    val customStyle = style.copy(
        titleOptions = TitleOptions(
            text = "极坐标答题正确率 (Tangential Bar)",
            subtext = "360度闭合网格，各分类位于不同半径轨道，切向排版标签",
            textStyle = style.titleOptions.textStyle.copy(fontSize = 16.sp)
        ),
        polarOptions = style.polarOptions.copy(
            startAngle = 90f,
            endAngle = 90f + 360f, // 闭合 360 度圆环
            radiusStepNumber = 4
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
            PolarBarChart(
                data = PolarChartData(xLabels = labels, series = series),
                style = customStyle,
                horizontal = true, // Tangential 模式 (分类在半径，数值在角度)
                modifier = Modifier.fillMaxSize()
            )
        }

        ControlPanel {
            Text(
                text = "极坐标圆环演示说明：\n1. X轴（分类）位于不同的半径轨道上，Y轴（数值）在角度轴上扫掠。\n2. 柱子呈同心圆圆弧状，文本顺着各自轨道的弧度切面进行旋转并居中展示，完全还原 ECharts 极坐标圆环图标签的 premium 排版。",
                fontSize = 12.sp,
                color = Color.Gray,
                lineHeight = 16.sp
            )
        }
    }
}

/**
 * 演示：某站点用户Access From (Basic Access Pie)
 */
@Composable
fun DemoBasicAccessPie(style: ChartStyle) {
    var selectedOffset by remember { mutableFloatStateOf(10f) }
    var showLabel by remember { mutableStateOf(true) }
    var borderWidth by remember { mutableFloatStateOf(0f) }

    val slices = listOf(
        PieSlice("Search Engine", 1048f, Color(0xFF5470C6)),
        PieSlice("Direct", 735f, Color(0xFF91CC75)),
        PieSlice("Email", 580f, Color(0xFFFAC858)),
        PieSlice("Union Ads", 484f, Color(0xFFEE6666)),
        PieSlice("Video Ads", 300f, Color(0xFF73C0DE))
    )

    val customStyle = style.copy(
        titleOptions = TitleOptions(
            text = "某站点用户 Access From",
            subtext = "经典实心饼图，带有防重叠重排的文字引导折线",
            textStyle = style.titleOptions.textStyle.copy(fontSize = 16.sp)
        ),
        pieOptions = style.pieOptions.copy(
            innerRadiusRatio = 0f, // 实心饼图
            cornerRadius = 0.dp,
            borderWidth = borderWidth.dp,
            selectedOffset = selectedOffset.dp,
            showLabel = showLabel,
            padAngle = 0f
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
            PieChart(
                data = PieChartData(slices = slices),
                style = customStyle,
                modifier = Modifier.fillMaxSize()
            )
        }

        ControlPanel {
            ControlSwitch(label = "显示边缘引线与占比文本 (showLabel)", checked = showLabel, onCheckedChange = { showLabel = it })

            ControlSlider(
                label = "选中扇区凸起偏移量 (selectedOffset)",
                value = selectedOffset,
                valueRange = 0f..25f,
                onValueChange = { selectedOffset = it },
                valueFormatter = { String.format("%.0f dp", it) }
            )

            ControlSlider(
                label = "背景分割描边宽度 (borderWidth)",
                value = borderWidth,
                valueRange = 0f..8f,
                onValueChange = { borderWidth = it },
                valueFormatter = { String.format("%.1f dp", it) }
            )
        }
    }
}

/**
 * 演示：圆角胶囊环形图 (Doughnut Chart with Rounded Corner)
 */
@Composable
fun DemoRoundedDoughnut(style: ChartStyle) {
    var innerRadiusRatio by remember { mutableFloatStateOf(0.6f) }
    var borderWidth by remember { mutableFloatStateOf(2f) }
    var cornerRadius by remember { mutableFloatStateOf(8f) }

    val slices = listOf(
        PieSlice("Search Engine", 1048f, Color(0xFF5470C6)),
        PieSlice("Direct", 735f, Color(0xFF91CC75)),
        PieSlice("Email", 580f, Color(0xFFFAC858)),
        PieSlice("Union Ads", 484f, Color(0xFFEE6666)),
        PieSlice("Video Ads", 300f, Color(0xFF73C0DE))
    )

    val customStyle = style.copy(
        titleOptions = TitleOptions(
            text = "圆角环形图",
            subtext = "Doughnut Chart with Rounded Corner",
            textStyle = style.titleOptions.textStyle.copy(fontSize = 16.sp)
        ),
        pieOptions = style.pieOptions.copy(
            innerRadiusRatio = innerRadiusRatio, // 环形图空心比例
            cornerRadius = cornerRadius.dp,
            borderWidth = borderWidth.dp,      // 扇区背景色描边宽度 (对标 ECharts borderWidth)
            roundCap = false,
            padAngle = 0f
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
            PieChart(
                data = PieChartData(slices = slices),
                style = customStyle,
                modifier = Modifier.fillMaxSize()
            )
        }

        ControlPanel {
            ControlSlider(
                label = "扇区圆角半径 (cornerRadius)",
                value = cornerRadius,
                valueRange = 0f..20f,
                onValueChange = { cornerRadius = it },
                valueFormatter = { String.format("%.0f dp", it) }
            )

            ControlSlider(
                label = "空心底座孔径比 (innerRadiusRatio)",
                value = innerRadiusRatio,
                valueRange = 0.2f..0.85f,
                onValueChange = { innerRadiusRatio = it },
                valueFormatter = { String.format("%.2f", it) }
            )

            ControlSlider(
                label = "背景分割描边宽度 (borderWidth)",
                value = borderWidth,
                valueRange = 0f..8f,
                onValueChange = { borderWidth = it },
                valueFormatter = { String.format("%.1f dp", it) }
            )
        }
    }
}

/**
 * 演示：半环形占比图 (Half Doughnut Chart)
 */
@Composable
fun DemoHalfDoughnut(style: ChartStyle) {
    var innerRadiusRatio by remember { mutableFloatStateOf(0.6f) }
    var startAngle by remember { mutableFloatStateOf(-180f) }
    var maxAngleSweep by remember { mutableFloatStateOf(180f) }

    val slices = listOf(
        PieSlice("Search Engine", 1048f, Color(0xFF5470C6)),
        PieSlice("Direct", 735f, Color(0xFF91CC75)),
        PieSlice("Email", 580f, Color(0xFFFAC858)),
        PieSlice("Union Ads", 484f, Color(0xFFEE6666)),
        PieSlice("Video Ads", 300f, Color(0xFF73C0DE))
    )

    val customStyle = style.copy(
        titleOptions = TitleOptions(
            text = "半环形占比图 (Half Doughnut)",
            subtext = "起始角为 -180度，扫掠角限制为 180度上半圆环",
            textStyle = style.titleOptions.textStyle.copy(fontSize = 16.sp)
        ),
        pieOptions = style.pieOptions.copy(
            innerRadiusRatio = innerRadiusRatio,
            startAngle = startAngle, // 9点钟方向
            maxAngleSweep = maxAngleSweep, // 扫掠180度半圆
            cornerRadius = 0.dp,
            padAngle = 0f
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
            PieChart(
                data = PieChartData(slices = slices),
                style = customStyle,
                modifier = Modifier.fillMaxSize()
            )
        }

        ControlPanel {
            ControlSlider(
                label = "旋转起始角度 (startAngle)",
                value = startAngle,
                valueRange = -360f..360f,
                onValueChange = { startAngle = it },
                valueFormatter = { String.format("%.0f 度", it) }
            )

            ControlSlider(
                label = "扫掠角度总跨度 (maxAngleSweep)",
                value = maxAngleSweep,
                valueRange = 90f..360f,
                onValueChange = { maxAngleSweep = it },
                valueFormatter = { String.format("%.0f 度", it) }
            )

            ControlSlider(
                label = "空心底座孔径比 (innerRadiusRatio)",
                value = innerRadiusRatio,
                valueRange = 0.2f..0.85f,
                onValueChange = { innerRadiusRatio = it },
                valueFormatter = { String.format("%.2f", it) }
            )
        }
    }
}

/**
 * 演示：饼图扇区间隙 (Pie with padAngle)
 */
@Composable
fun DemoPiePadAngle(style: ChartStyle) {
    var innerRadiusRatio by remember { mutableFloatStateOf(0.5f) }
    var padAngle by remember { mutableFloatStateOf(4f) }
    var borderWidth by remember { mutableFloatStateOf(0f) }

    val slices = listOf(
        PieSlice("Search Engine", 1048f, Color(0xFF5470C6)),
        PieSlice("Direct", 735f, Color(0xFF91CC75)),
        PieSlice("Email", 580f, Color(0xFFFAC858)),
        PieSlice("Union Ads", 484f, Color(0xFFEE6666)),
        PieSlice("Video Ads", 300f, Color(0xFF73C0DE))
    )

    val customStyle = style.copy(
        titleOptions = TitleOptions(
            text = "饼图扇区平行等宽隔离间隙",
            subtext = "设置 padAngle = 4f，射线隔离线覆盖裁剪产生等宽间隙",
            textStyle = style.titleOptions.textStyle.copy(fontSize = 16.sp)
        ),
        pieOptions = style.pieOptions.copy(
            innerRadiusRatio = innerRadiusRatio,
            cornerRadius = 0.dp,
            borderWidth = borderWidth.dp,
            padAngle = padAngle // 4度等宽隔离线
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
            PieChart(
                data = PieChartData(slices = slices),
                style = customStyle,
                modifier = Modifier.fillMaxSize()
            )
        }

        ControlPanel {
            ControlSlider(
                label = "扇区间隙夹角 (padAngle)",
                value = padAngle,
                valueRange = 0f..12f,
                onValueChange = { padAngle = it },
                valueFormatter = { String.format("%.1f 度", it) }
            )

            ControlSlider(
                label = "背景分割描边宽度 (borderWidth)",
                value = borderWidth,
                valueRange = 0f..8f,
                onValueChange = { borderWidth = it },
                valueFormatter = { String.format("%.1f dp", it) }
            )

            ControlSlider(
                label = "空心底座孔径比 (innerRadiusRatio)",
                value = innerRadiusRatio,
                valueRange = 0.2f..0.85f,
                onValueChange = { innerRadiusRatio = it },
                valueFormatter = { String.format("%.2f", it) }
            )
        }
    }
}

/**
 * 演示：普通环形图 (Doughnut Chart)
 */
@Composable
fun DemoDoughnutBasic(style: ChartStyle) {
    var innerRadiusRatio by remember { mutableFloatStateOf(0.6f) }
    var padAngle by remember { mutableFloatStateOf(0f) }
    var borderWidth by remember { mutableFloatStateOf(0f) }

    val slices = listOf(
        PieSlice("Search Engine", 1048f, Color(0xFF5470C6)),
        PieSlice("Direct", 735f, Color(0xFF91CC75)),
        PieSlice("Email", 580f, Color(0xFFFAC858)),
        PieSlice("Union Ads", 484f, Color(0xFFEE6666)),
        PieSlice("Video Ads", 300f, Color(0xFF73C0DE))
    )

    val customStyle = style.copy(
        titleOptions = TitleOptions(
            text = "环形图",
            subtext = "Doughnut Chart",
            textStyle = style.titleOptions.textStyle.copy(fontSize = 16.sp)
        ),
        pieOptions = style.pieOptions.copy(
            innerRadiusRatio = innerRadiusRatio, // 环形图空心比例
            roundCap = false, // 普通环形，非圆角
            cornerRadius = 0.dp,
            padAngle = padAngle,
            borderWidth = borderWidth.dp
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
            PieChart(
                data = PieChartData(slices = slices),
                style = customStyle,
                modifier = Modifier.fillMaxSize()
            )
        }

        ControlPanel {
            ControlSlider(
                label = "空心底座孔径比 (innerRadiusRatio)",
                value = innerRadiusRatio,
                valueRange = 0.2f..0.85f,
                onValueChange = { innerRadiusRatio = it },
                valueFormatter = { String.format("%.2f", it) }
            )

            ControlSlider(
                label = "扇区间隙夹角 (padAngle)",
                value = padAngle,
                valueRange = 0f..10f,
                onValueChange = { padAngle = it },
                valueFormatter = { String.format("%.1f 度", it) }
            )

            ControlSlider(
                label = "背景分割描边宽度 (borderWidth)",
                value = borderWidth,
                valueRange = 0f..8f,
                onValueChange = { borderWidth = it },
                valueFormatter = { String.format("%.1f dp", it) }
            )
        }
    }
}


