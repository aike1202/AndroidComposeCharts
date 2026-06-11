package io.github.composechart.charts.bar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
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
 * 原生直角坐标系柱状图/条形图组件 (BarChart)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BarChart(
    data: BarChartData,
    modifier: Modifier = Modifier,
    style: ChartStyle = ChartStyle.Default,
    viewportState: ViewportState = rememberViewportState(),
    interactionState: InteractionState = rememberInteractionState(),
    tooltipOptions: TooltipOptions = TooltipOptions.Default,
    legendOptions: LegendOptions = LegendOptions.Default,
    dataZoomOptions: DataZoomOptions? = null,
    horizontal: Boolean = false, // 是否将图表旋转为水平条形图
    clickToZoom: Boolean = false, // 点击类目轴是否平滑缩放聚焦
    onBarSelected: ((bar: BarValue, series: BarSeries) -> Unit)? = null
) {
    val textMeasurer = rememberTextMeasurer()
    val hapticFeedback = LocalHapticFeedback.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // 管理系列点击过滤
    val hiddenSeriesNames = remember { mutableStateListOf<String>() }

    // 柱子入场高度生长动画
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animationProgress.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }

    // 根据视口显示模式初始化 X 轴范围（若数据不为空）
    LaunchedEffect(data.xLabels, viewportState.viewportMode) {
        viewportState.initializeRange(
            isCategory = true,
            categorySize = data.xLabels.size,
            categoryPadding = 0.5f
        )
    }

    val visibleSeries = data.series.filter { it.name !in hiddenSeriesNames }

    Column(
        modifier = modifier
            .background(style.backgroundColor)
            .padding(8.dp)
    ) {
        // ================= 1. 渲染标题 =================
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

        // ================= 2. 渲染顶部图例 =================
        if (legendOptions.show && legendOptions.position == LegendPosition.Top) {
            RenderLegend(
                seriesList = data.series,
                hiddenList = hiddenSeriesNames,
                options = legendOptions,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ================= 3. 渲染核心 Canvas 图表 =================
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
            val maxLimitX = (data.xLabels.size - 1).coerceAtLeast(0).toFloat()
            viewportState.constrain(minLimitX = -0.5f, maxLimitX = maxLimitX + 0.5f)

            // 计算视口极值
            val maxValuesCount = data.xLabels.size
            val leftIndex = if (maxValuesCount == 0) 0 else viewportState.minX.roundToInt().coerceIn(0, maxValuesCount - 1)
            val rightIndex = if (maxValuesCount == 0) -1 else viewportState.maxX.roundToInt().coerceIn(0, maxValuesCount - 1)

            var visibleMinVal = 0f
            var visibleMaxVal = 0f

            // 依据并列或堆叠，求取当前屏幕可视段柱子的正负高度极值
            for (categoryIndex in leftIndex..rightIndex) {
                val posAccumMap = mutableMapOf<String, Float>()
                val negAccumMap = mutableMapOf<String, Float>()

                for (series in visibleSeries) {
                    val valObj = series.values.getOrNull(categoryIndex) ?: continue
                    val rawValue = valObj.value
                    val stackKey = series.stack

                    if (stackKey != null) {
                        if (rawValue >= 0f) {
                            val acc = posAccumMap[stackKey] ?: 0f
                            val next = acc + rawValue
                            posAccumMap[stackKey] = next
                            if (next > visibleMaxVal) visibleMaxVal = next
                        } else {
                            val acc = negAccumMap[stackKey] ?: 0f
                            val next = acc + rawValue
                            negAccumMap[stackKey] = next
                            if (next < visibleMinVal) visibleMinVal = next
                        }
                    } else {
                        if (rawValue >= 0f) {
                            if (rawValue > visibleMaxVal) visibleMaxVal = rawValue
                        } else {
                            if (rawValue < visibleMinVal) visibleMinVal = rawValue
                        }
                    }
                }
            }

            if (visibleMinVal == 0f && visibleMaxVal == 0f) {
                visibleMaxVal = 100f
            }

            // Nice Numbers 算法计算规整极值
            val niceInterval = io.github.composechart.core.util.AxisIntervalCalculator.calculate(
                min = visibleMinVal,
                max = visibleMaxVal,
                splitNumber = 5
            )

            // 倒装映射视口以适应水平条形图旋转
            val currentViewport = if (!horizontal) {
                ViewportState(
                    initialMinX = viewportState.minX,
                    initialMaxX = viewportState.maxX,
                    initialMinY = niceInterval.min,
                    initialMaxY = niceInterval.max
                )
            } else {
                ViewportState(
                    initialMinX = niceInterval.min,
                    initialMaxX = niceInterval.max,
                    initialMinY = viewportState.minX,
                    initialMaxY = viewportState.maxX
                )
            }

            val mapper = CoordinateMapper(gridRect = gridRect, viewportState = currentViewport)

            // 倒装底座轴属性以支持水平条形图
            val coordinatePlotter = if (!horizontal) {
                RectangularCoordinate(
                    mapper = mapper,
                    style = style,
                    textMeasurer = textMeasurer,
                    xLabels = data.xLabels,
                    yTicks = niceInterval.ticks,
                    categoryValueProvider = { idx -> visibleSeries.mapNotNull { it.values.getOrNull(idx)?.value }.sum() }
                )
            } else {
                val niceTicksStr = niceInterval.ticks.map {
                    String.format("%.1f", it).removeSuffix(".0")
                }
                RectangularCoordinate(
                    mapper = mapper,
                    style = style.copy(
                        xAxisOptions = style.yAxisOptions.copy(showGridLines = style.xAxisOptions.showGridLines),
                        yAxisOptions = style.xAxisOptions.copy(showGridLines = style.yAxisOptions.showGridLines)
                    ),
                    textMeasurer = textMeasurer,
                    xLabels = niceTicksStr,
                    yTicks = (0 until data.xLabels.size).map { it.toFloat() }.filter {
                        it in (viewportState.minX - 0.5f)..(viewportState.maxX + 0.5f)
                    },
                    categoryValueProvider = { idx -> visibleSeries.mapNotNull { it.values.getOrNull(idx)?.value }.sum() }
                )
            }

            // 柱体渲染器
            val barRenderer = BarChartRenderer(
                mapper = mapper,
                allSeries = visibleSeries,
                style = style,
                horizontal = horizontal,
                animationProgress = animationProgress.value
            )

            // 计算扫掠时单个类目的物理大小 (用于 Tooltip 块状阴影高亮背景绘制)
            val categoryLength = if (!horizontal) gridRect.width else gridRect.height
            val totalLabelsCount = maxValuesCount.toFloat()
            val itemSpacing = if (totalLabelsCount > 1) categoryLength / (totalLabelsCount - 1) else categoryLength

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(clickToZoom, horizontal) {
                        if (clickToZoom) {
                            detectTapGestures { offset ->
                                val dataVal = if (!horizontal) mapper.toDataX(offset.x) else mapper.toDataY(offset.y)
                                val clickIdx = dataVal.roundToInt()
                                if (clickIdx in data.xLabels.indices) {
                                    coroutineScope.launch {
                                        val targetMin = (clickIdx - 1.2f).coerceAtLeast(-0.5f)
                                        val targetMax = (clickIdx + 1.2f).coerceAtMost(data.xLabels.lastIndex + 0.5f)
                                        val anim = Animatable(0f)
                                        val startMin = viewportState.minX
                                        val startMax = viewportState.maxX
                                        anim.animateTo(
                                            targetValue = 1.0f,
                                            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                                        ) {
                                            viewportState.minX = startMin + (targetMin - startMin) * value
                                            viewportState.maxX = startMax + (targetMax - startMax) * value
                                        }
                                    }
                                }
                            }
                        }
                    }
                    .chartGestureDetector(
                        mapper = mapper,
                        viewportState = viewportState,
                        interactionState = interactionState,
                        hapticFeedback = hapticFeedback,
                        horizontal = horizontal
                    )
            ) {
                // 4.1 绘制网格背景
                coordinatePlotter.drawBackground(this)

                // 4.2 绘制柱状图柱体
                barRenderer.draw(this)

                // 4.3 绘制网格前置坐标轴与标签
                coordinatePlotter.drawAxesAndLabels(this)

                // 4.4 绘制扫掠阴影背景与 Tooltip 卡片
                if (interactionState.isTooltipActive && interactionState.tooltipDataX != null && interactionState.tooltipScreenOffset != null && data.xLabels.isNotEmpty()) {
                    val sweepVal = if (!horizontal) interactionState.tooltipDataX!! else mapper.toDataY(interactionState.tooltipScreenOffset!!.y)
                    val closestIndex = sweepVal.roundToInt().coerceIn(data.xLabels.indices)

                    val tooltipInfo = TooltipInfo(
                        title = data.xLabels[closestIndex],
                        items = visibleSeries.map { s ->
                            val ptVal = s.values.getOrNull(closestIndex)
                            val valText = if (ptVal != null) String.format("%.1f", ptVal.value).removeSuffix(".0") else "无"
                            TooltipItem(
                                seriesName = s.name,
                                value = valText,
                                color = s.color
                            )
                        }
                    )

                    // 气泡发光指示灯位置：柱子顶端
                    val indicatorPoints = visibleSeries.mapNotNull { s ->
                        val ptVal = s.values.getOrNull(closestIndex) ?: return@mapNotNull null
                        if (!horizontal) {
                            Offset(mapper.toScreenX(closestIndex.toFloat()), mapper.toScreenY(ptVal.value))
                        } else {
                            Offset(mapper.toScreenX(ptVal.value), mapper.toScreenY(closestIndex.toFloat()))
                        }
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
                        horizontal = horizontal
                    )
                }
            }
        }

        // ================= 5. 渲染底部图例 =================
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
    seriesList: List<BarSeries>,
    hiddenList: List<String>,
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
                                (hiddenList as MutableList<String>).remove(series.name)
                            } else {
                                (hiddenList as MutableList<String>).add(series.name)
                            }
                        } else if (options.selectMode == LegendSelectMode.Single) {
                            (hiddenList as MutableList<String>).clear()
                            seriesList.forEach {
                                if (it.name != series.name) {
                                    (hiddenList as MutableList<String>).add(it.name)
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
