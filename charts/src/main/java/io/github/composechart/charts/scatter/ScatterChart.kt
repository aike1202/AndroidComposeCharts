package io.github.composechart.charts.scatter

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.runtime.getValue
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
import io.github.composechart.core.style.IndicatorStyle
import io.github.composechart.core.style.LegendIconShape
import io.github.composechart.core.style.LegendOptions
import io.github.composechart.core.style.LegendPosition
import io.github.composechart.core.style.LegendSelectMode
import io.github.composechart.core.style.TooltipOptions
import io.github.composechart.core.style.TriggerType
import kotlin.math.roundToInt

/**
 * 霓虹发光交互式散点图与气泡图组件 (ScatterChart)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScatterChart(
    data: ScatterChartData,
    modifier: Modifier = Modifier,
    style: ChartStyle = ChartStyle.Default,
    viewportState: ViewportState = rememberViewportState(),
    interactionState: InteractionState = rememberInteractionState(),
    tooltipOptions: TooltipOptions = TooltipOptions.Default.copy(
        trigger = TriggerType.Item,
        indicatorStyle = IndicatorStyle.Cross
    ),
    legendOptions: LegendOptions = LegendOptions.Default,
    visualMap: ScatterVisualMap? = null,
    onValueSelected: ((point: ScatterPoint, series: ScatterSeries) -> Unit)? = null
) {
    val textMeasurer = rememberTextMeasurer()
    val hapticFeedback = LocalHapticFeedback.current
    val density = LocalDensity.current

    // 管理隐藏图例系列
    val hiddenSeriesNames = remember { mutableStateListOf<String>() }

    // 根据视口显示模式初始化 X 轴范围（若数据不为空）
    LaunchedEffect(data.series, data.xLabels, viewportState.viewportMode) {
        val isCategory = !data.xLabels.isNullOrEmpty()
        if (isCategory) {
            viewportState.initializeRange(
                isCategory = true,
                categorySize = data.xLabels!!.size
            )
        } else {
            var minX = Float.MAX_VALUE
            var maxX = -Float.MAX_VALUE
            var hasData = false
            val allXList = mutableListOf<Float>()
            for (s in data.series) {
                for (pt in s.points) {
                    if (pt.x < minX) minX = pt.x
                    if (pt.x > maxX) maxX = pt.x
                    allXList.add(pt.x)
                    hasData = true
                }
            }
            allXList.sort()
            val pad = if (hasData && maxX > minX) (maxX - minX) * 0.05f else 1f
            viewportState.initializeRange(
                isCategory = false,
                categorySize = 0,
                numericMin = if (hasData) minX - pad else 0f,
                numericMax = if (hasData) maxX + pad else 100f,
                allSortedXValues = allXList
            )
        }
    }

    // 2. 动效时间轴：涟漪扩散动画因子
    val transition = rememberInfiniteTransition(label = "scatter_ripples")
    val rippleProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_progress"
    )

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
            var minLimitX = 0f
            var maxLimitX = 100f
            if (!data.xLabels.isNullOrEmpty()) {
                maxLimitX = (data.xLabels.size - 1).toFloat()
            } else {
                var minXVal = Float.MAX_VALUE
                var maxXVal = -Float.MAX_VALUE
                var hasData = false
                for (s in data.series) {
                    for (pt in s.points) {
                        if (pt.x < minXVal) minXVal = pt.x
                        if (pt.x > maxXVal) maxXVal = pt.x
                        hasData = true
                    }
                }
                if (hasData) {
                    val pad = if (maxXVal > minXVal) (maxXVal - minXVal) * 0.05f else 1f
                    minLimitX = minXVal - pad
                    maxLimitX = maxXVal + pad
                }
            }
            viewportState.constrain(minLimitX = minLimitX, maxLimitX = maxLimitX)

            // 计算当前屏幕可见范围内的 Y 轴与 X 轴(若为数值轴)的 Nice Range 极致
            var visibleMinY = Float.MAX_VALUE
            var visibleMaxY = -Float.MAX_VALUE
            var visibleMinX = Float.MAX_VALUE
            var visibleMaxX = -Float.MAX_VALUE
            var hasVisibleData = false

            for (s in visibleSeries) {
                for (pt in s.points) {
                    // 仅统计处于当前 X 轴视口范围内的散点
                    if (pt.x in viewportState.minX..viewportState.maxX) {
                        if (pt.y < visibleMinY) visibleMinY = pt.y
                        if (pt.y > visibleMaxY) visibleMaxY = pt.y
                        if (pt.x < visibleMinX) visibleMinX = pt.x
                        if (pt.x > visibleMaxX) visibleMaxX = pt.x
                        hasVisibleData = true
                    }
                }
            }

            if (!hasVisibleData) {
                visibleMinY = 0f
                visibleMaxY = 100f
                visibleMinX = viewportState.minX
                visibleMaxX = viewportState.maxX
            }

            // Nice Numbers 计算
            val yInterval = io.github.composechart.core.util.AxisIntervalCalculator.calculate(
                min = visibleMinY,
                max = visibleMaxY,
                splitNumber = 5
            )

            val xInterval = if (data.xLabels.isNullOrEmpty()) {
                io.github.composechart.core.util.AxisIntervalCalculator.calculate(
                    min = visibleMinX,
                    max = visibleMaxX,
                    splitNumber = 5
                )
            } else null

            // 数据映射核心引擎
            val currentViewport = ViewportState(
                initialMinX = viewportState.minX,
                initialMaxX = viewportState.maxX,
                initialMinY = yInterval.min,
                initialMaxY = yInterval.max
            )
            val mapper = CoordinateMapper(gridRect = gridRect, viewportState = currentViewport)

            // 碰撞检测与交互数据查找
            var hoveredPoint: ScatterPoint? = null
            var hoveredSeries: ScatterSeries? = null
            var hoveredPointOffset: Offset? = null
            var hoveredPIdx: Int? = null
            var hoveredSIdx: Int? = null

            if (interactionState.isTooltipActive && interactionState.tooltipScreenOffset != null) {
                val touchOffset = interactionState.tooltipScreenOffset!!
                var minDistance = Float.MAX_VALUE
                val searchRadiusPx = with(density) { 36.dp.toPx() } // 感应检测半径限制

                for (sIdx in visibleSeries.indices) {
                    val s = visibleSeries[sIdx]
                    for (pIdx in s.points.indices) {
                        val pt = s.points[pIdx]
                        val px = mapper.toScreenX(pt.x)
                        val py = mapper.toScreenY(pt.y)
                        val offset = Offset(px, py)
                        val dist = (offset - touchOffset).getDistance()
                        if (dist < minDistance && dist <= searchRadiusPx) {
                            minDistance = dist
                            hoveredPoint = pt
                            hoveredSeries = s
                            hoveredPointOffset = offset
                            hoveredPIdx = pIdx
                            hoveredSIdx = sIdx
                        }
                    }
                }
            }

            // 直角坐标轴绘制器
            val coordinatePlotter = RectangularCoordinate(
                mapper = mapper,
                style = style,
                textMeasurer = textMeasurer,
                xLabels = data.xLabels ?: emptyList(),
                yTicks = yInterval.ticks,
                xTicks = xInterval?.ticks
            )

            // 构建系列渲染器
            val renderers = visibleSeries.mapIndexed { sIdx, s ->
                ScatterChartRenderer(
                    mapper = mapper,
                    series = s,
                    style = style,
                    textMeasurer = textMeasurer,
                    visualMap = visualMap,
                    animationProgress = rippleProgress,
                    hoveredPointIndex = if (hoveredSIdx == sIdx) hoveredPIdx else null
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
                // 4.1 绘制坐标网格背景
                coordinatePlotter.drawBackground(this)

                // 4.2 绘制散点系列数据层
                for (renderer in renderers) {
                    renderer.draw(this)
                }

                // 4.3 绘制坐标轴主线与标签
                coordinatePlotter.drawAxesAndLabels(this)

                // 4.4 绘制 Tooltip 十字辅助定位线与气泡卡片
                if (hoveredPoint != null && hoveredSeries != null && hoveredPointOffset != null) {
                    onValueSelected?.invoke(hoveredPoint, hoveredSeries)

                    val titleText = hoveredPoint.name ?: hoveredSeries.name
                    val items = mutableListOf<TooltipItem>()

                    if (!data.xLabels.isNullOrEmpty()) {
                        val labelX = data.xLabels.getOrNull(hoveredPoint.x.roundToInt())
                            ?: String.format("%.1f", hoveredPoint.x).removeSuffix(".0")
                        items.add(
                            TooltipItem(
                                seriesName = "类目",
                                value = labelX,
                                color = hoveredSeries.color
                            )
                        )
                    } else {
                        items.add(
                            TooltipItem(
                                seriesName = "X 轴",
                                value = String.format("%.1f", hoveredPoint.x).removeSuffix(".0"),
                                color = hoveredSeries.color
                            )
                        )
                    }

                    items.add(
                        TooltipItem(
                            seriesName = "Y 轴",
                            value = String.format("%.1f", hoveredPoint.y).removeSuffix(".0"),
                            color = hoveredSeries.color
                        )
                    )

                    if (hoveredPoint.value != null) {
                        items.add(
                            TooltipItem(
                                seriesName = "数值",
                                value = String.format("%.1f", hoveredPoint.value).removeSuffix(".0"),
                                color = hoveredSeries.color
                            )
                        )
                    }

                    val tooltipInfo = TooltipInfo(
                        title = titleText,
                        items = items
                    )

                    TooltipRenderer(
                        textMeasurer = textMeasurer,
                        style = style,
                        tooltipOptions = tooltipOptions.copy(trigger = TriggerType.Axis) // 强制使用 Axis 以触发十字线
                    ).draw(
                        drawScope = this,
                        mapper = mapper,
                        tooltipInfo = tooltipInfo,
                        indicatorPoints = listOf(hoveredPointOffset),
                        touchOffset = interactionState.tooltipScreenOffset!!
                    )
                }
            }
        }

        // ================= 5. 渲染底部 Legend =================
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
 * 渲染散点图图例 FlowRow
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RenderLegend(
    seriesList: List<ScatterSeries>,
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
