package io.github.composechart.charts.line

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
import androidx.compose.ui.text.drawText
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlin.math.roundToInt

/**
 * 极致直角折线与面积渐变图组件 (LineChart)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LineChart(
    data: LineChartData,
    modifier: Modifier = Modifier,
    style: ChartStyle = ChartStyle.Default,
    viewportState: ViewportState = rememberViewportState(),
    interactionState: InteractionState = rememberInteractionState(),
    tooltipOptions: TooltipOptions = TooltipOptions.Default,
    legendOptions: LegendOptions = LegendOptions.Default,
    dataZoomOptions: DataZoomOptions? = null,
    visualMap: List<VisualMapRange>? = null,
    rangeAreaOptions: RangeAreaOptions? = null,
    rangeAreaOptionsList: List<RangeAreaOptions> = emptyList(),
    x2Labels: List<String>? = null,
    xAxisTicks: List<Float>? = null,
    x2AxisTicks: List<Float>? = null,
    markAreas: List<MarkArea> = emptyList(),
    onValueSelected: ((point: LinePoint, series: LineSeries) -> Unit)? = null
) {
    val textMeasurer = rememberTextMeasurer()
    val hapticFeedback = LocalHapticFeedback.current
    val density = LocalDensity.current

    // 管理哪些折线系列处于激活/显示状态，实现类似 ECharts 图例点击显示/隐藏联动过滤
    val hiddenSeriesNames = remember { mutableStateListOf<String>() }

    // 根据视口显示模式初始化 X 轴范围（若数据不为空）
    LaunchedEffect(data.xLabels, xAxisTicks, viewportState.viewportMode) {
        if (xAxisTicks != null && xAxisTicks.isNotEmpty()) {
            viewportState.initializeRange(
                isCategory = false,
                categorySize = 0,
                numericMin = xAxisTicks.minOrNull() ?: 0f,
                numericMax = xAxisTicks.maxOrNull() ?: 100f
            )
        } else {
            viewportState.initializeRange(
                isCategory = true,
                categorySize = data.xLabels.size
            )
        }
    }

    val visibleSeries = data.series.filter { it.name !in hiddenSeriesNames }

    Column(
        modifier = modifier
            .background(style.backgroundColor)
            .padding(8.dp)
    ) {
        // ================= 1. 渲染主标题及副标题 =================
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

        // ================= 2. 渲染顶部 Legend (图例列表) =================
        if (legendOptions.show && legendOptions.position == LegendPosition.Top) {
            RenderLegend(
                seriesList = data.series,
                hiddenList = hiddenSeriesNames,
                options = legendOptions,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ================= 3. 渲染核心 Canvas 图表主体 =================
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val widthPx = constraints.maxWidth.toFloat()
            val heightPx = constraints.maxHeight.toFloat()

            // 转化 GridOptions (margins) 到物理像素
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

            // 限制视口越界与过度缩放，防止滑动时前后出现大面积空白
            if (xAxisTicks != null && xAxisTicks.isNotEmpty()) {
                val minLim = xAxisTicks.minOrNull() ?: 0f
                val maxLim = xAxisTicks.maxOrNull() ?: 100f
                viewportState.constrain(minLimitX = minLim, maxLimitX = maxLim)
            } else {
                val maxLimitX = (data.xLabels.size - 1).coerceAtLeast(0).toFloat()
                viewportState.constrain(minLimitX = 0f, maxLimitX = maxLimitX)
            }

            // 计算当前屏幕可见范围内的 Y 轴物理极致 (Auto Y Scaling)
            var visibleMinY = Float.MAX_VALUE
            var visibleMaxY = -Float.MAX_VALUE
            var hasVisibleData = false

            if (data.xLabels.isEmpty()) {
                // 数值轴 (Numeric Axis) 逻辑：直接遍历所有可见系列中的点，查找 X 落在视口内的 Y 极值
                for (series in visibleSeries) {
                    val (leftIdx, rightIdx) = series.points.getVisibleRange(viewportState.minX, viewportState.maxX)
                    for (i in leftIdx..rightIdx) {
                        val pt = series.points.getOrNull(i) ?: continue
                        val y = pt.y ?: continue
                        hasVisibleData = true
                        if (y < visibleMinY) visibleMinY = y
                        if (y > visibleMaxY) visibleMaxY = y
                    }
                }
            } else {
                // 类目轴 (Category Axis) 逻辑：依索引循环，支持堆叠计算
                val leftIndex = viewportState.minX.roundToInt().coerceIn(0, data.xLabels.lastIndex)
                val rightIndex = viewportState.maxX.roundToInt().coerceIn(0, data.xLabels.lastIndex)

                for (i in leftIndex..rightIndex) {
                    val posSums = mutableMapOf<String, Float>()
                    val negSums = mutableMapOf<String, Float>()

                    for (series in visibleSeries) {
                        val pt = series.points.getOrNull(i) ?: continue
                        val y = pt.y ?: continue
                        hasVisibleData = true

                        val stackKey = series.stack
                        if (stackKey != null) {
                            if (y >= 0f) {
                                val cur = posSums[stackKey] ?: 0f
                                val accumulated = cur + y
                                posSums[stackKey] = accumulated
                                if (accumulated > visibleMaxY) visibleMaxY = accumulated
                            } else {
                                val cur = negSums[stackKey] ?: 0f
                                val accumulated = cur + y
                                negSums[stackKey] = accumulated
                                if (accumulated < visibleMinY) visibleMinY = accumulated
                            }
                        } else {
                            if (y < visibleMinY) visibleMinY = y
                            if (y > visibleMaxY) visibleMaxY = y
                        }
                    }
                }
            }

            if (!hasVisibleData) {
                visibleMinY = 0f
                visibleMaxY = 100f
            } else {
                if (visibleMinY == Float.MAX_VALUE) {
                    visibleMinY = 0f
                }
                if (visibleMaxY == -Float.MAX_VALUE) {
                    visibleMaxY = 0f
                }
            }

            // Nice Numbers 算法计算规整刻度
            val yInterval = io.github.composechart.core.util.AxisIntervalCalculator.calculate(
                min = visibleMinY,
                max = visibleMaxY,
                splitNumber = 5
            )

            // 依据 Nice 算出的极值构建用于 Canvas 绘制的 CoordinateMapper
            val currentViewport = ViewportState(
                initialMinX = viewportState.minX,
                initialMaxX = viewportState.maxX,
                initialMinY = yInterval.min,
                initialMaxY = yInterval.max
            )
            val mapper = CoordinateMapper(
                gridRect = gridRect,
                viewportState = currentViewport,
                isYInversed = style.yAxisOptions.inverse
            )

            // 创建直角坐标网格和标签绘制器
            val coordinatePlotter = RectangularCoordinate(
                mapper = mapper,
                style = style,
                textMeasurer = textMeasurer,
                xLabels = data.xLabels,
                yTicks = yInterval.ticks,
                xTicks = xAxisTicks,
                x2Labels = x2Labels,
                x2Ticks = x2AxisTicks
            )

            // 预备置信区间带列表
            val activeRangeAreas = remember(rangeAreaOptions, rangeAreaOptionsList) {
                val list = mutableListOf<RangeAreaOptions>()
                rangeAreaOptions?.let { list.add(it) }
                list.addAll(rangeAreaOptionsList)
                list
            }

            // 缓存每个 stack分组当前已累加的高程列表 (大小为 data.xLabels.size)
            val stackHeights = mutableMapOf<String, FloatArray>()

            // 预备折线图渲染器列表
            val renderers = visibleSeries.map { series ->
                val stackKey = series.stack
                val baseline = if (stackKey != null) {
                    val heights = stackHeights.getOrPut(stackKey) { FloatArray(data.xLabels.size) { 0f } }
                    val currentBaseline = heights.toList()
                    
                    for (i in series.points.indices) {
                        val y = series.points[i].y ?: 0f
                        heights[i] += y
                    }
                    currentBaseline
                } else {
                    null
                }

                LineChartRenderer(
                    mapper = mapper,
                    series = series,
                    style = style,
                    textMeasurer = textMeasurer,
                    baselinePoints = baseline
                )
            }

            // ================= 4. 组装交互与 Canvas 渲染 =================
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .chartGestureDetector(
                        mapper = mapper,
                        viewportState = viewportState,
                        interactionState = interactionState,
                        hapticFeedback = hapticFeedback
                    )
            ) {
                // 4.1 绘制坐标系背景分割区与分割线
                coordinatePlotter.drawBackground(this)

                // 4.1.2 绘制 MarkArea 警戒色带标注区域
                for (area in markAreas) {
                    val rectLeft = if (area.startX != null) mapper.toScreenX(area.startX).coerceIn(gridRect.left, gridRect.right) else gridRect.left
                    val rectRight = if (area.endX != null) mapper.toScreenX(area.endX).coerceIn(gridRect.left, gridRect.right) else gridRect.right

                    val y1 = if (area.startY != null) mapper.toScreenY(area.startY) else gridRect.bottom
                    val y2 = if (area.endY != null) mapper.toScreenY(area.endY) else gridRect.top

                    val rectTop = kotlin.math.min(y1, y2).coerceIn(gridRect.top, gridRect.bottom)
                    val rectBottom = kotlin.math.max(y1, y2).coerceIn(gridRect.top, gridRect.bottom)

                    if (rectLeft < rectRight && rectTop < rectBottom) {
                        drawRect(
                            color = area.color,
                            topLeft = Offset(rectLeft, rectTop),
                            size = Size(rectRight - rectLeft, rectBottom - rectTop)
                        )

                        if (area.label != null) {
                            val textLayout = textMeasurer.measure(
                                text = area.label,
                                style = style.xAxisOptions.labelTextStyle.copy(
                                    color = area.labelColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            val tw = textLayout.size.width
                            val th = textLayout.size.height
                            val labelX = (rectRight - tw - 6f).coerceAtLeast(rectLeft + 6f)
                            val labelY = rectTop + 6f
                            drawText(
                                textLayoutResult = textLayout,
                                topLeft = Offset(labelX, labelY)
                            )
                        }
                    }
                }

                // 4.1.5 绘制置信度区间阴影带 (Confidence Bands)
                clipRect(
                    left = gridRect.left,
                    top = gridRect.top,
                    right = gridRect.right,
                    bottom = gridRect.bottom
                ) {
                    for (rangeOption in activeRangeAreas) {
                        val upperIdx = rangeOption.upperSeriesIndex
                        val lowerIdx = rangeOption.lowerSeriesIndex
                        val upperRenderer = renderers.getOrNull(upperIdx)
                        val lowerRenderer = renderers.getOrNull(lowerIdx)
                        
                        if (upperRenderer != null && lowerRenderer != null) {
                            val upperPath = upperRenderer.generateAreaPath()
                            val lowerPath = lowerRenderer.generateAreaPath()
                            val confidenceBandPath = Path.combine(
                                operation = androidx.compose.ui.graphics.PathOperation.Difference,
                                path1 = upperPath,
                                path2 = lowerPath
                            )
                            drawPath(path = confidenceBandPath, color = rangeOption.fillColor)
                        }
                    }
                }

                // 4.2 绘制折线与面积渐变层
                for (renderer in renderers) {
                    renderer.draw(this)
                }

                // 4.3 绘制前置轴线与标签
                coordinatePlotter.drawAxesAndLabels(this)

                // 4.4 绘制极致标注：MarkLine (均线) 和 MarkPoint (水滴极值)
                for (renderer in renderers) {
                    renderer.drawMarkLines(this)
                    renderer.drawMarkPoints(this)
                }

                // 4.4.5 绘制 Line Race 折线末端贴附标签 (带重叠避让算法)
                val raceLabels = mutableListOf<RaceLabelInfo>()
                for (s in visibleSeries) {
                    if (s.showEndLabel) {
                        val points = s.points
                        if (points.isNotEmpty()) {
                            val (leftIdx, rightIdx) = points.getVisibleRange(mapper.toDataX(gridRect.left) - 1f, mapper.toDataX(gridRect.right) + 1f)
                            val visiblePoints = if (leftIdx <= rightIdx) points.subList(leftIdx, rightIdx + 1) else emptyList()
                            val lastValidPoint = visiblePoints.lastOrNull { it.y != null }
                            if (lastValidPoint != null) {
                                val lastX = mapper.toScreenX(lastValidPoint.x)
                                val lastY = mapper.toScreenY(lastValidPoint.y ?: 0f)
                                if (lastX in gridRect.left..gridRect.right && lastY in gridRect.top..gridRect.bottom) {
                                    val yVal = lastValidPoint.y ?: 0f
                                    val text = "${s.name}: ${String.format("%.0f", yVal)}"
                                    raceLabels.add(
                                        RaceLabelInfo(
                                            series = s,
                                            lastX = lastX,
                                            lastY = lastY,
                                            text = text,
                                            color = s.color
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                if (raceLabels.isNotEmpty()) {
                    raceLabels.sortBy { it.lastY }

                    val itemHeightPx = with(density) { 16.dp.toPx() }
                    val marginLabelPx = with(density) { 6.dp.toPx() }

                    val adjustedLabels = raceLabels.map { it.copy() }
                    for (i in 1 until adjustedLabels.size) {
                        val prev = adjustedLabels[i - 1]
                        val curr = adjustedLabels[i]
                        if (curr.lastY < prev.lastY + itemHeightPx) {
                            curr.lastY = prev.lastY + itemHeightPx
                        }
                    }

                    val maxAllowedY = gridRect.bottom - itemHeightPx / 2f
                    if (adjustedLabels.isNotEmpty() && adjustedLabels.last().lastY > maxAllowedY) {
                        adjustedLabels.last().lastY = maxAllowedY
                        for (i in adjustedLabels.size - 2 downTo 0) {
                            val next = adjustedLabels[i + 1]
                            val curr = adjustedLabels[i]
                            if (curr.lastY > next.lastY - itemHeightPx) {
                                curr.lastY = next.lastY - itemHeightPx
                            }
                        }
                    }

                    for (i in adjustedLabels.indices) {
                        val orig = raceLabels[i]
                        val adj = adjustedLabels[i]
                        val layout = textMeasurer.measure(
                            text = adj.text,
                            style = style.xAxisOptions.labelTextStyle.copy(
                                color = adj.color,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )

                        val textWidth = layout.size.width
                        val textHeight = layout.size.height

                        val drawX = adj.lastX + marginLabelPx
                        val drawY = adj.lastY - textHeight / 2f
                        drawText(
                            textLayoutResult = layout,
                            topLeft = Offset(drawX, drawY)
                        )

                        if (kotlin.math.abs(adj.lastY - orig.lastY) > 2f) {
                            drawLine(
                                color = adj.color.copy(alpha = 0.5f),
                                start = Offset(orig.lastX, orig.lastY),
                                end = Offset(drawX - 2f, adj.lastY),
                                strokeWidth = with(density) { 1.dp.toPx() },
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                            )
                        }
                    }
                }

                // 4.5 绘制长按锁定扫掠 Tooltip 浮窗
                if (interactionState.isTooltipActive && interactionState.tooltipDataX != null && interactionState.tooltipScreenOffset != null && data.xLabels.isNotEmpty()) {
                    val sweepDataX = interactionState.tooltipDataX!!
                    val closestIndex = sweepDataX.roundToInt().coerceIn(data.xLabels.indices)

                    val tooltipTitle = if (x2Labels != null && closestIndex in x2Labels.indices && closestIndex in data.xLabels.indices) {
                        "${data.xLabels[closestIndex]} / ${x2Labels[closestIndex]}"
                    } else if (closestIndex in data.xLabels.indices) {
                        data.xLabels[closestIndex]
                    } else {
                        ""
                    }

                    // 组织浮窗呈现的数据信息
                    val tooltipInfo = TooltipInfo(
                        title = tooltipTitle,
                        items = visibleSeries.map { s ->
                            val pt = s.points.getOrNull(closestIndex)
                            val valText = if (pt?.y != null) String.format("%.1f", pt.y).removeSuffix(".0") else "无"
                            TooltipItem(
                                seriesName = s.name,
                                value = valText,
                                color = s.color
                            )
                        }
                    )

                    // 获取各个线系列在这个 X 位置的物理圆点物理像素坐标
                    val stackSumsForTooltip = mutableMapOf<String, Float>()
                    val indicatorPoints = visibleSeries.mapNotNull { s ->
                        val pt = s.points.getOrNull(closestIndex)
                        if (pt?.y != null) {
                            val stackKey = s.stack
                            val displayedY = if (stackKey != null) {
                                val curSum = stackSumsForTooltip[stackKey] ?: 0f
                                val accum = curSum + pt.y
                                stackSumsForTooltip[stackKey] = accum
                                accum
                            } else {
                                pt.y
                            }
                            Offset(mapper.toScreenX(closestIndex.toFloat()), mapper.toScreenY(displayedY))
                        } else null
                    }

                    // 实例化 Tooltip 渲染器并绘制气泡卡片
                    TooltipRenderer(
                        textMeasurer = textMeasurer,
                        style = style,
                        tooltipOptions = tooltipOptions
                    ).draw(
                        drawScope = this,
                        mapper = mapper,
                        tooltipInfo = tooltipInfo,
                        indicatorPoints = indicatorPoints,
                        touchOffset = interactionState.tooltipScreenOffset!!
                    )
                }
            }
        }

        // ================= 5. 渲染底部 Legend (如果设置在底部) =================
        if (legendOptions.show && legendOptions.position == LegendPosition.Bottom) {
            RenderLegend(
                seriesList = data.series,
                hiddenList = hiddenSeriesNames,
                options = legendOptions,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * 渲染图例项 FlowRow
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RenderLegend(
    seriesList: List<LineSeries>,
    hiddenList: MutableList<String>,
    options: LegendOptions,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.padding(vertical = 4.dp, horizontal = 12.dp),
        horizontalArrangement = when (options.alignment) {
            Alignment.Start -> Arrangement.Start
            Alignment.End -> Arrangement.End
            else -> Arrangement.Center
        }
    ) {
        for (series in seriesList) {
            val isHidden = series.name in hiddenList
            Row(
                modifier = Modifier
                    .padding(horizontal = options.itemGap / 2, vertical = 2.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(enabled = options.selectMode != LegendSelectMode.None) {
                        if (options.selectMode == LegendSelectMode.Multiple) {
                            if (isHidden) {
                                hiddenList.remove(series.name)
                            } else {
                                hiddenList.add(series.name)
                            }
                        } else if (options.selectMode == LegendSelectMode.Single) {
                            hiddenList.clear()
                            seriesList.forEach {
                                if (it.name != series.name) {
                                    hiddenList.add(it.name)
                                }
                            }
                        }
                    }
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 图例色彩图形指示器
                val indicatorColor = if (isHidden) Color.LightGray else series.color
                val textAlpha = if (isHidden) 0.4f else 1.0f

                Box(
                    modifier = Modifier
                        .size(options.itemWidth, options.itemHeight)
                        .clip(
                            if (options.iconShape == LegendIconShape.Circle) RoundedCornerShape(50)
                            else RoundedCornerShape(2.dp)
                        )
                        .background(indicatorColor)
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = series.name,
                    style = options.textStyle.copy(
                        color = options.textStyle.color.copy(alpha = textAlpha)
                    )
                )
            }
        }
    }
}

private data class RaceLabelInfo(
    val series: LineSeries,
    val lastX: Float,
    var lastY: Float,
    val text: String,
    val color: Color
)
