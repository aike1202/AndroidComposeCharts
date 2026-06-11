package io.github.composechart.charts.boxplot

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
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
import io.github.composechart.core.style.LegendIconShape
import io.github.composechart.core.style.LegendOptions
import io.github.composechart.core.style.LegendPosition
import io.github.composechart.core.style.LegendSelectMode
import io.github.composechart.core.style.TitleOptions
import io.github.composechart.core.style.TooltipOptions
import kotlin.math.roundToInt

/**
 * 极致高画质统计学盒须箱线图组件 (BoxplotChart)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BoxplotChart(
    data: BoxplotChartData,
    modifier: Modifier = Modifier,
    style: ChartStyle = ChartStyle.Default,
    viewportState: ViewportState = rememberViewportState(),
    interactionState: InteractionState = rememberInteractionState(),
    tooltipOptions: TooltipOptions = TooltipOptions.Default,
    legendOptions: LegendOptions = LegendOptions.Default,
    onValueSelected: ((point: BoxplotPoint, series: BoxplotSeries) -> Unit)? = null
) {
    val textMeasurer = rememberTextMeasurer()
    val hapticFeedback = LocalHapticFeedback.current
    val density = LocalDensity.current

    // 图例单选/多选过滤系列
    val hiddenSeriesNames = remember { mutableStateListOf<String>() }

    // 根据视口显示模式初始化 X 轴范围（若数据不为空）
    LaunchedEffect(data.xLabels, viewportState.viewportMode) {
        viewportState.initializeRange(
            isCategory = true,
            categorySize = data.xLabels.size,
            categoryPadding = 0.5f
        )
    }

    // 1. 驱动两阶段风琴生长动画 (大箱体拉伸 -> 触须长出)
    val boxGrowth = remember { Animatable(0f) }
    val whiskerGrowth = remember { Animatable(0f) }

    LaunchedEffect(data.series) {
        boxGrowth.snapTo(0f)
        whiskerGrowth.snapTo(0f)
        // 第一阶段：箱体中位拉伸
        boxGrowth.animateTo(
            targetValue = 1f,
            animationSpec = tween(500, easing = LinearOutSlowInEasing)
        )
        // 第二阶段：触须延伸
        whiskerGrowth.animateTo(
            targetValue = 1f,
            animationSpec = tween(400, easing = FastOutSlowInEasing)
        )
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

        // ================= 2. 置顶 Legend =================
        if (legendOptions.show && legendOptions.position == LegendPosition.Top) {
            RenderLegend(
                seriesList = data.series,
                hiddenList = hiddenSeriesNames,
                options = legendOptions,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ================= 3. 渲染核心 Canvas 箱线网格 =================
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

            // Nice Numbers Y 轴极值测算 (统计算入 min、max 触须以及 Outliers 散点的值，防止溢出)
            val leftIndex = if (data.xLabels.isEmpty()) 0 else viewportState.minX.roundToInt().coerceIn(0, data.xLabels.lastIndex)
            val rightIndex = if (data.xLabels.isEmpty()) -1 else viewportState.maxX.roundToInt().coerceIn(0, data.xLabels.lastIndex)

            var visibleMinY = Float.MAX_VALUE
            var visibleMaxY = -Float.MAX_VALUE
            var hasVisibleData = false

            for (series in visibleSeries) {
                val safeL = if (series.points.isEmpty()) 0 else leftIndex.coerceIn(series.points.indices)
                val safeR = if (series.points.isEmpty()) -1 else rightIndex.coerceIn(series.points.indices)
                for (i in safeL..safeR) {
                    val pt = series.points[i]
                    if (pt.min < visibleMinY) visibleMinY = pt.min
                    if (pt.max > visibleMaxY) visibleMaxY = pt.max

                    // 算入 outliers 散点极值
                    val outs = series.outliers.filter { it.xIndex == i }
                    for (out in outs) {
                        if (out.value < visibleMinY) visibleMinY = out.value
                        if (out.value > visibleMaxY) visibleMaxY = out.value
                    }
                    hasVisibleData = true
                }
            }

            if (!hasVisibleData) {
                visibleMinY = 0f
                visibleMaxY = 100f
            }

            val yInterval = io.github.composechart.core.util.AxisIntervalCalculator.calculate(
                min = visibleMinY,
                max = visibleMaxY,
                splitNumber = 5
            )

            val currentViewport = ViewportState(
                initialMinX = viewportState.minX,
                initialMaxX = viewportState.maxX,
                initialMinY = yInterval.min,
                initialMaxY = yInterval.max
            )
            val mapper = CoordinateMapper(gridRect = gridRect, viewportState = currentViewport)

            // 直角坐标轴绘制器
            val coordinatePlotter = RectangularCoordinate(
                mapper = mapper,
                style = style,
                textMeasurer = textMeasurer,
                xLabels = data.xLabels,
                yTicks = yInterval.ticks
            )

            // 选中锁定的类目索引
            var hoveredCategoryIdx: Int? = null
            if (interactionState.isTooltipActive && interactionState.tooltipDataX != null && data.xLabels.isNotEmpty()) {
                hoveredCategoryIdx = interactionState.tooltipDataX!!.roundToInt().coerceIn(data.xLabels.indices)
            }

            // 构建系列渲染器
            val renderers = visibleSeries.mapIndexed { sIdx, s ->
                BoxplotChartRenderer(
                    mapper = mapper,
                    series = s,
                    style = style,
                    textMeasurer = textMeasurer,
                    boxGrowth = boxGrowth.value,
                    whiskerGrowth = whiskerGrowth.value,
                    hoveredCategoryIndex = hoveredCategoryIdx,
                    seriesIndex = sIdx,
                    totalSeriesCount = visibleSeries.size,
                    totalLabelsCount = data.xLabels.size
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
                // 4.1 绘制网格线背景
                coordinatePlotter.drawBackground(this)

                // 4.2 绘制盒须系列数据层
                for (renderer in renderers) {
                    renderer.draw(this)
                }

                // 4.3 绘制直角坐标前置轴线与标签
                coordinatePlotter.drawAxesAndLabels(this)

                // 4.4 绘制长按锁定扫掠 Tooltip 五数大观
                if (hoveredCategoryIdx != null && interactionState.tooltipScreenOffset != null) {
                    val items = mutableListOf<TooltipItem>()

                    if (visibleSeries.size == 1) {
                        // 场景 A：单系列，罗列五数大观清单与异常值
                        val s = visibleSeries[0]
                        val pt = s.points.getOrNull(hoveredCategoryIdx)
                        if (pt != null) {
                            onValueSelected?.invoke(pt, s)

                            items.add(TooltipItem("最大值 (Max)", String.format("%.1f", pt.max).removeSuffix(".0"), s.color))
                            items.add(TooltipItem("上四分位 (Q3)", String.format("%.1f", pt.q3).removeSuffix(".0"), s.color))
                            items.add(TooltipItem("中位数 (Median)", String.format("%.1f", pt.median).removeSuffix(".0"), s.color))
                            items.add(TooltipItem("下四分位 (Q1)", String.format("%.1f", pt.q1).removeSuffix(".0"), s.color))
                            items.add(TooltipItem("最小值 (Min)", String.format("%.1f", pt.min).removeSuffix(".0"), s.color))

                            val outs = s.outliers.filter { it.xIndex == hoveredCategoryIdx }
                            if (outs.isNotEmpty()) {
                                val outStr = outs.map { String.format("%.1f", it.value).removeSuffix(".0") }.joinToString(", ")
                                items.add(TooltipItem("异常值", "${outs.size}个 ($outStr)", Color.Red))
                            }
                        }
                    } else {
                        // 场景 B：多系列并列，紧凑并排显示各自的中位数与 Q1-Q3 区间
                        for (s in visibleSeries) {
                            val pt = s.points.getOrNull(hoveredCategoryIdx) ?: continue
                            val valStr = "中位:${String.format("%.1f", pt.median).removeSuffix(".0")} (Q1:${String.format("%.1f", pt.q1).removeSuffix(".0")}-Q3:${String.format("%.1f", pt.q3).removeSuffix(".0")})"
                            items.add(TooltipItem(s.name, valStr, s.color))
                        }
                    }

                    val tooltipInfo = TooltipInfo(
                        title = data.xLabels[hoveredCategoryIdx],
                        items = items
                    )

                    // 磁吸对齐指示器 X 坐标为锁定的类目中心线
                    val sweepX = mapper.toScreenX(hoveredCategoryIdx.toFloat())
                    val indicatorPoints = listOf(Offset(sweepX, interactionState.tooltipScreenOffset!!.y))

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

        // ================= 5. 置底 Legend =================
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
 * 渲染图例
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RenderLegend(
    seriesList: List<BoxplotSeries>,
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
