package io.github.composechart.charts.mixed

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.composechart.charts.bar.BarChartRenderer
import io.github.composechart.charts.bar.BarSeries
import io.github.composechart.charts.line.LineChartRenderer
import io.github.composechart.charts.line.LineSeries
import io.github.composechart.core.data.CoordinateMapper
import io.github.composechart.core.gesture.chartGestureDetector
import io.github.composechart.core.plot.RectangularCoordinate
import io.github.composechart.core.plot.TooltipInfo
import io.github.composechart.core.plot.TooltipItem
import io.github.composechart.core.plot.TooltipRenderer
import io.github.composechart.core.state.InteractionState
import io.github.composechart.core.state.ViewportState
import io.github.composechart.core.state.rememberInteractionState
import io.github.composechart.core.state.rememberViewportState
import io.github.composechart.core.style.ChartStyle
import io.github.composechart.core.style.DataZoomOptions
import io.github.composechart.core.style.LegendIconShape
import io.github.composechart.core.style.LegendOptions
import io.github.composechart.core.style.LegendPosition
import io.github.composechart.core.style.LegendSelectMode
import io.github.composechart.core.style.TooltipOptions
import io.github.composechart.core.util.AxisIntervalCalculator
import kotlin.math.roundToInt

/**
 * 原生直角坐标系折线-柱状双 Y 轴混合图表组件 (MixedChart)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MixedChart(
    data: MixedChartData,
    modifier: Modifier = Modifier,
    style: ChartStyle = ChartStyle.Default,
    viewportState: ViewportState = rememberViewportState(),
    interactionState: InteractionState = rememberInteractionState(),
    tooltipOptions: TooltipOptions = TooltipOptions.Default,
    legendOptions: LegendOptions = LegendOptions.Default,
    dataZoomOptions: DataZoomOptions? = null,
    onValueSelected: ((index: Int, seriesName: String, value: Float) -> Unit)? = null
) {
    val textMeasurer = rememberTextMeasurer()
    val hapticFeedback = LocalHapticFeedback.current
    val density = LocalDensity.current

    // 图例过滤系列
    val hiddenSeriesNames = remember { mutableStateListOf<String>() }

    // 入场动效
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animationProgress.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )
    }

    // 视口初始化 (混合图含有柱体，安全外拓 0.5f)
    LaunchedEffect(data.xLabels, viewportState.viewportMode) {
        viewportState.initializeRange(
            isCategory = true,
            categorySize = data.xLabels.size,
            categoryPadding = 0.5f
        )
    }

    val visibleLineSeries = data.lineSeries.filter { it.name !in hiddenSeriesNames }
    val visibleBarSeries = data.barSeries.filter { it.name !in hiddenSeriesNames }
    val allSeriesNames = (data.lineSeries.map { it.name } + data.barSeries.map { it.name })

    Column(
        modifier = modifier
            .background(style.backgroundColor)
            .padding(8.dp)
    ) {
        // ================= 1. 标题 =================
        if (style.titleOptions.show && style.titleOptions.text.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalAlignment = when (style.titleOptions.alignment) {
                    Alignment.CenterHorizontally -> Alignment.CenterHorizontally
                    Alignment.End -> Alignment.End
                    else -> Alignment.Start
                }
            ) {
                Text(
                    text = style.titleOptions.text,
                    style = style.titleOptions.textStyle
                )
                if (style.titleOptions.subtext.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(style.titleOptions.itemGap))
                    Text(
                        text = style.titleOptions.subtext,
                        style = style.titleOptions.subtextStyle
                    )
                }
            }
        }

        // ================= 2. 置顶图例 =================
        if (legendOptions.show && legendOptions.position == LegendPosition.Top) {
            RenderMixedLegend(
                lineSeries = data.lineSeries,
                barSeries = data.barSeries,
                hiddenList = hiddenSeriesNames,
                options = legendOptions,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ================= 3. 核心 Canvas 区域 =================
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val widthPx = constraints.maxWidth.toFloat()
            val heightPx = constraints.maxHeight.toFloat()

            val leftMarginPx = with(density) { style.gridOptions.left.toPx() }
            val rightMarginPx = with(density) { style.gridOptions.right.toPx() }
            val topMarginPx = with(density) { style.gridOptions.top.toPx() }
            val bottomMarginPx = with(density) { style.gridOptions.bottom.toPx() }

            val gridRect = Rect(
                left = leftMarginPx,
                top = topMarginPx,
                right = widthPx - rightMarginPx,
                bottom = heightPx - bottomMarginPx
            )

            // 视口平移缩放硬拦截限制
            val maxLimitX = (data.xLabels.size - 1).coerceAtLeast(0).toFloat()
            viewportState.constrain(minLimitX = -0.5f, maxLimitX = maxLimitX + 0.5f)

            // 根据可见视口计算当前 Y1(左) 和 Y2(右) 轴的局部极值
            val leftIndex = if (data.xLabels.isEmpty()) 0 else viewportState.minX.roundToInt().coerceIn(data.xLabels.indices)
            val rightIndex = if (data.xLabels.isEmpty()) -1 else viewportState.maxX.roundToInt().coerceIn(data.xLabels.indices)

            var visibleMinY1 = Float.MAX_VALUE
            var visibleMaxY1 = -Float.MAX_VALUE
            var hasY1 = false

            var visibleMinY2 = Float.MAX_VALUE
            var visibleMaxY2 = -Float.MAX_VALUE
            var hasY2 = false

            // 遍历评估左/右轴的极值
            for (idx in leftIndex..rightIndex) {
                // 评估折线
                for (s in visibleLineSeries) {
                    val valY = s.points.getOrNull(idx)?.y ?: continue
                    if (s.yAxisIndex == 1) {
                        if (valY < visibleMinY2) visibleMinY2 = valY
                        if (valY > visibleMaxY2) visibleMaxY2 = valY
                        hasY2 = true
                    } else {
                        if (valY < visibleMinY1) visibleMinY1 = valY
                        if (valY > visibleMaxY1) visibleMaxY1 = valY
                        hasY1 = true
                    }
                }
                // 评估柱状
                for (s in visibleBarSeries) {
                    val valY = s.values.getOrNull(idx)?.value ?: continue
                    if (s.yAxisIndex == 1) {
                        if (valY < visibleMinY2) visibleMinY2 = valY
                        if (valY > visibleMaxY2) visibleMaxY2 = valY
                        hasY2 = true
                    } else {
                        if (valY < visibleMinY1) visibleMinY1 = valY
                        if (valY > visibleMaxY1) visibleMaxY1 = valY
                        hasY1 = true
                    }
                }
            }

            // 防空兜底
            if (!hasY1) {
                visibleMinY1 = 0f
                visibleMaxY1 = 100f
            }
            if (!hasY2) {
                visibleMinY2 = 0f
                visibleMaxY2 = 100f
            }

            // Nice Numbers 刻度同步对齐计算
            val niceY1 = AxisIntervalCalculator.calculate(
                min = visibleMinY1,
                max = visibleMaxY1,
                splitNumber = 5
            )
            val segmentCount = (niceY1.ticks.size - 1).coerceAtLeast(1)
            val niceY2 = AxisIntervalCalculator.calculateWithSegmentCount(
                min = visibleMinY2,
                max = visibleMaxY2,
                targetSegmentCount = segmentCount
            )

            // 创建统一的映射底盘
            val currentViewport = ViewportState(
                initialMinX = viewportState.minX,
                initialMaxX = viewportState.maxX,
                initialMinY = niceY1.min,
                initialMaxY = niceY1.max
            )
            val mapper = CoordinateMapper(gridRect = gridRect, viewportState = currentViewport)

            // 实例化坐标轴网格绘制器
            val coordinatePlotter = RectangularCoordinate(
                mapper = mapper,
                style = style,
                textMeasurer = textMeasurer,
                xLabels = data.xLabels,
                yTicks = niceY1.ticks,
                y2Ticks = niceY2.ticks
            )

            // 实例化柱状渲染器
            val barRenderer = BarChartRenderer(
                mapper = mapper,
                allSeries = visibleBarSeries,
                style = style,
                horizontal = false,
                animationProgress = animationProgress.value,
                y2Min = niceY2.min,
                y2Max = niceY2.max
            )

            val totalLabelsCount = data.xLabels.size.toFloat()
            val categoryLength = gridRect.width
            val itemSpacing = if (totalLabelsCount > 1) categoryLength / (totalLabelsCount - 1) else categoryLength

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .chartGestureDetector(
                        mapper = mapper,
                        viewportState = viewportState,
                        interactionState = interactionState,
                        hapticFeedback = hapticFeedback,
                        horizontal = false
                    )
            ) {
                // 1. 绘制网格背景及交替颜色
                coordinatePlotter.drawBackground(this)

                // 2. 绘制柱体
                barRenderer.draw(this)

                // 3. 绘制多组折线系列
                for (lineSeries in visibleLineSeries) {
                    val lineRenderer = LineChartRenderer(
                        mapper = mapper,
                        series = lineSeries,
                        style = style,
                        textMeasurer = textMeasurer,
                        y2Min = niceY2.min,
                        y2Max = niceY2.max
                    )
                    lineRenderer.draw(this)
                    lineRenderer.drawMarkPoints(this)
                    lineRenderer.drawMarkLines(this)
                }

                // 4. 绘制前置坐标轴与双 Y 刻度
                coordinatePlotter.drawAxesAndLabels(this)

                // 5. 绘制 Hover 十字扫掠 Tooltip 卡片
                if (interactionState.isTooltipActive && interactionState.tooltipDataX != null && interactionState.tooltipScreenOffset != null && data.xLabels.isNotEmpty()) {
                    val sweepVal = interactionState.tooltipDataX!!
                    val closestIndex = sweepVal.roundToInt().coerceIn(data.xLabels.indices)

                    val tooltipItems = mutableListOf<TooltipItem>()
                    // 收集柱子数值
                    visibleBarSeries.forEach { s ->
                        val pt = s.values.getOrNull(closestIndex)
                        val text = if (pt != null) String.format("%.1f", pt.value).removeSuffix(".0") else "无"
                        tooltipItems.add(TooltipItem(s.name, text, s.color))
                    }
                    // 收集折线数值
                    visibleLineSeries.forEach { s ->
                        val pt = s.points.getOrNull(closestIndex)
                        val text = if (pt?.y != null) String.format("%.1f", pt.y).removeSuffix(".0") else "无"
                        tooltipItems.add(TooltipItem(s.name, text, s.color))
                    }

                    val tooltipInfo = TooltipInfo(
                        title = data.xLabels[closestIndex],
                        items = tooltipItems
                    )

                    // 计算全部可见拐点发光指示器的物理坐标
                    val indicatorPoints = mutableListOf<Offset>()
                    // 柱状指示点在顶部
                    visibleBarSeries.forEach { s ->
                        val pt = s.values.getOrNull(closestIndex) ?: return@forEach
                        val ptY = if (s.yAxisIndex == 1) {
                            mapper.toScreenY2(pt.value, niceY2.min, niceY2.max)
                        } else {
                            mapper.toScreenY(pt.value)
                        }
                        indicatorPoints.add(Offset(mapper.toScreenX(closestIndex.toFloat()), ptY))
                    }
                    // 折线指示点
                    visibleLineSeries.forEach { s ->
                        val pt = s.points.getOrNull(closestIndex) ?: return@forEach
                        val valY = pt.y ?: return@forEach
                        val ptY = if (s.yAxisIndex == 1) {
                            mapper.toScreenY2(valY, niceY2.min, niceY2.max)
                        } else {
                            mapper.toScreenY(valY)
                        }
                        indicatorPoints.add(Offset(mapper.toScreenX(closestIndex.toFloat()), ptY))
                    }

                    TooltipRenderer(
                        textMeasurer = textMeasurer,
                        style = style,
                        tooltipOptions = tooltipOptions
                    ).draw(
                        drawScope = this,
                        mapper = mapper,
                        tooltipInfo = tooltipInfo,
                        indicatorPoints = indicatorPoints,
                        touchOffset = interactionState.tooltipScreenOffset!!,
                        itemSpacing = itemSpacing,
                        horizontal = false
                    )
                }
            }
        }

        // ================= 4. 置底图例 =================
        if (legendOptions.show && legendOptions.position == LegendPosition.Bottom) {
            RenderMixedLegend(
                lineSeries = data.lineSeries,
                barSeries = data.barSeries,
                hiddenList = hiddenSeriesNames,
                options = legendOptions,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * 混合系列图例渲染
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RenderMixedLegend(
    lineSeries: List<LineSeries>,
    barSeries: List<BarSeries>,
    hiddenList: List<String>,
    options: LegendOptions,
    modifier: Modifier = Modifier
) {
    val allSeries = lineSeries.map { it.name to it.color } + barSeries.map { it.name to it.color }

    FlowRow(
        modifier = modifier.padding(vertical = 4.dp, horizontal = 12.dp),
        horizontalArrangement = when (options.alignment) {
            Alignment.Start -> Arrangement.Start
            Alignment.End -> Arrangement.End
            else -> Arrangement.Center
        }
    ) {
        for ((name, color) in allSeries) {
            val isHidden = name in hiddenList
            Row(
                modifier = Modifier
                    .padding(horizontal = options.itemGap / 2, vertical = 2.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(enabled = options.selectMode != LegendSelectMode.None) {
                        if (options.selectMode == LegendSelectMode.Multiple) {
                            if (isHidden) {
                                (hiddenList as MutableList<String>).remove(name)
                            } else {
                                (hiddenList as MutableList<String>).add(name)
                            }
                        } else if (options.selectMode == LegendSelectMode.Single) {
                            (hiddenList as MutableList<String>).clear()
                            allSeries.forEach {
                                if (it.first != name) {
                                    (hiddenList as MutableList<String>).add(it.first)
                                }
                            }
                        }
                    }
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val indicatorColor = if (isHidden) Color.LightGray else color
                val textAlpha = if (isHidden) 0.4f else 1.0f

                Box(
                    modifier = Modifier
                        .size(width = options.itemWidth, height = options.itemHeight)
                        .background(
                            color = indicatorColor,
                            shape = if (options.iconShape == LegendIconShape.Circle) RoundedCornerShape(100) else RoundedCornerShape(2.dp)
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = name,
                    style = options.textStyle.copy(color = options.textStyle.color.copy(alpha = textAlpha))
                )
            }
        }
    }
}
