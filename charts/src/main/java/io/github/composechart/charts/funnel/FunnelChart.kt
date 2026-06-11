package io.github.composechart.charts.funnel

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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import io.github.composechart.core.style.TriggerType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 极美级联倒角漏斗分析与金字塔图组件 (FunnelChart)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FunnelChart(
    data: FunnelChartData,
    modifier: Modifier = Modifier,
    style: ChartStyle = ChartStyle.Default,
    options: FunnelOptions = FunnelOptions.Default,
    viewportState: ViewportState = rememberViewportState(),
    interactionState: InteractionState = rememberInteractionState(),
    tooltipOptions: TooltipOptions = TooltipOptions.Default.copy(trigger = TriggerType.Item),
    legendOptions: LegendOptions = LegendOptions.Default,
    onSliceSelected: ((slice: FunnelSlice) -> Unit)? = null
) {
    val textMeasurer = rememberTextMeasurer()
    val hapticFeedback = LocalHapticFeedback.current
    val density = LocalDensity.current

    // 图例过滤隐藏状态
    val hiddenSliceNames = remember { mutableStateListOf<String>() }

    // 1. 数据重排与过滤
    val sortedSlices = remember(data.slices, options.sort, hiddenSliceNames) {
        val filtered = data.slices.filter { it.name !in hiddenSliceNames }
        when (options.sort) {
            FunnelSort.Descending -> filtered.sortedByDescending { it.value }
            FunnelSort.Ascending -> filtered.sortedBy { it.value }
            FunnelSort.None -> filtered
        }
    }

    val n = sortedSlices.size

    // 2. 瀑布流雨点般依次渐显坠落的级联动画时钟
    val alphaAnims = remember(sortedSlices) { List(n) { Animatable(0f) } }
    val offsetAnims = remember(sortedSlices) { List(n) { Animatable(0f) } }

    LaunchedEffect(sortedSlices) {
        sortedSlices.indices.forEach { i ->
            launch {
                // 每层坠落依次延迟 120ms
                delay(i * 120L)
                launch {
                    alphaAnims[i].animateTo(
                        targetValue = 1f,
                        animationSpec = tween(450, easing = LinearOutSlowInEasing)
                    )
                }
                launch {
                    offsetAnims[i].animateTo(
                        targetValue = 1f,
                        animationSpec = tween(650, easing = FastOutSlowInEasing)
                    )
                }
            }
        }
    }

    val isDark = style.backgroundColor != Color.Transparent && style.backgroundColor.red < 0.5f

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

        // ================= 2. 顶置图例 =================
        if (legendOptions.show && legendOptions.position == LegendPosition.Top) {
            RenderLegend(
                slices = data.slices,
                hiddenList = hiddenSliceNames,
                options = legendOptions,
                colorPalette = style.colorPalette,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ================= 3. 核心 Canvas 漏斗绘制 =================
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val widthPx = constraints.maxWidth.toFloat()
            val heightPx = constraints.maxHeight.toFloat()

            val gridRect = with(density) {
                Rect(
                    left = 40.dp.toPx(),
                    top = 48.dp.toPx(),
                    right = widthPx - 40.dp.toPx(),
                    bottom = heightPx - 40.dp.toPx()
                )
            }

            // 计算垂直等分
            val gapPx = with(density) { options.gap.toPx() }
            val totalHeight = gridRect.height - (n - 1) * gapPx
            val sliceHeight = totalHeight / n

            // 手势区域 Y 轴区间碰撞检测
            var hoveredIndex: Int? = null
            var touchOffset: Offset? = null

            if (interactionState.isTooltipActive && interactionState.tooltipScreenOffset != null && n > 0) {
                val tOffset = interactionState.tooltipScreenOffset!!
                if (tOffset.y in gridRect.top..gridRect.bottom) {
                    val idx = ((tOffset.y - gridRect.top) / (sliceHeight + gapPx)).toInt()
                    if (idx in 0 until n) {
                        hoveredIndex = idx
                        touchOffset = tOffset
                    }
                }
            }

            // 虚拟的映射器，仅用于满足 TooltipRenderer 对 CoordinateMapper 的位置反算及裁剪校验
            val dummyViewport = ViewportState(0f, 100f, 0f, 100f)
            val mapper = CoordinateMapper(gridRect = gridRect, viewportState = dummyViewport)

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
                val renderer = FunnelChartRenderer(
                    slices = sortedSlices,
                    options = options,
                    style = style,
                    textMeasurer = textMeasurer,
                    sliceAlphaProgresses = alphaAnims.map { it.value },
                    sliceYOffsetProgresses = offsetAnims.map { it.value },
                    hoveredSliceIndex = hoveredIndex,
                    isDark = isDark
                )

                // 3.1 绘制漏斗梯形及引线转化率
                renderer.draw(this)

                // 3.2 绘制触碰高亮 Tooltip 气泡
                if (hoveredIndex != null && touchOffset != null) {
                    val slice = sortedSlices[hoveredIndex]
                    onSliceSelected?.invoke(slice)

                    val tooltipInfo = TooltipInfo(
                        title = slice.name,
                        items = listOf(
                            TooltipItem(
                                seriesName = "阶段数值",
                                value = String.format("%.1f", slice.value).removeSuffix(".0"),
                                color = slice.color ?: style.colorPalette[hoveredIndex % style.colorPalette.size]
                            )
                        )
                    )

                    // 确定气泡磁吸指示的垂直位置（即当前漏斗块的中心高度）
                    val indicatorY = gridRect.top + hoveredIndex * (sliceHeight + gapPx) + sliceHeight / 2f
                    val indicatorOffset = Offset(widthPx / 2f, indicatorY)

                    TooltipRenderer(
                        textMeasurer = textMeasurer,
                        style = style,
                        tooltipOptions = tooltipOptions
                    ).draw(
                        drawScope = this,
                        mapper = mapper,
                        tooltipInfo = tooltipInfo,
                        indicatorPoints = listOf(indicatorOffset),
                        touchOffset = touchOffset
                    )
                }
            }
        }

        // ================= 4. 底置图例 =================
        if (legendOptions.show && legendOptions.position == LegendPosition.Bottom) {
            RenderLegend(
                slices = data.slices,
                hiddenList = hiddenSliceNames,
                options = legendOptions,
                colorPalette = style.colorPalette,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * 渲染漏斗图图例
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RenderLegend(
    slices: List<FunnelSlice>,
    hiddenList: MutableList<String>,
    options: LegendOptions,
    colorPalette: List<Color>,
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
        slices.forEachIndexed { index, slice ->
            val isHidden = slice.name in hiddenList
            Row(
                modifier = Modifier
                    .padding(horizontal = options.itemGap / 2, vertical = 2.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(enabled = options.selectMode != LegendSelectMode.None) {
                        if (options.selectMode == LegendSelectMode.Multiple) {
                            if (isHidden) {
                                hiddenList.remove(slice.name)
                            } else {
                                hiddenList.add(slice.name)
                            }
                        } else if (options.selectMode == LegendSelectMode.Single) {
                            hiddenList.clear()
                            slices.forEach {
                                if (it.name != slice.name) {
                                    hiddenList.add(it.name)
                                }
                            }
                        }
                    }
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val indicatorColor = if (isHidden) Color.LightGray else (slice.color ?: colorPalette[index % colorPalette.size])
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
                    text = slice.name,
                    style = options.textStyle.copy(
                        color = options.textStyle.color.copy(alpha = textAlpha)
                    )
                )
            }
        }
    }
}
