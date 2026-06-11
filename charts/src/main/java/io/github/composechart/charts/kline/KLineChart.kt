package io.github.composechart.charts.kline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.composechart.core.data.CoordinateMapper
import io.github.composechart.core.gesture.chartGestureDetector
import io.github.composechart.core.plot.RectangularCoordinate
import io.github.composechart.core.state.InteractionState
import io.github.composechart.core.state.ViewportState
import io.github.composechart.core.state.rememberInteractionState
import io.github.composechart.core.state.rememberViewportState
import io.github.composechart.core.style.ChartStyle
import io.github.composechart.core.util.AxisIntervalCalculator
import kotlin.math.roundToInt

/**
 * 原生金融 K 线蜡烛图组件 (KLineChart)
 */
@Composable
fun KLineChart(
    data: KLineChartData,
    modifier: Modifier = Modifier,
    style: ChartStyle = ChartStyle.Default,
    kLineStyle: KLineStyle = KLineStyle.Default,
    viewportState: ViewportState = rememberViewportState(),
    interactionState: InteractionState = rememberInteractionState()
) {
    val textMeasurer = rememberTextMeasurer()
    val hapticFeedback = LocalHapticFeedback.current
    val density = LocalDensity.current

    // 根据视口显示模式初始化 X 轴范围（若数据不为空）
    LaunchedEffect(data.entries, viewportState.viewportMode) {
        viewportState.initializeRange(
            isCategory = true,
            categorySize = data.entries.size,
            categoryPadding = 0.5f
        )
    }

    Column(
        modifier = modifier
            .background(style.backgroundColor)
            .padding(vertical = 4.dp)
    ) {
        // ================= 1. 渲染标题 =================
        if (style.titleOptions.show && style.titleOptions.text.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
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

        // ================= 2. 渲染顶部看盘指标看板 =================
        val activeEntry = if (interactionState.isTooltipActive && interactionState.tooltipDataX != null && data.entries.isNotEmpty()) {
            val idx = interactionState.tooltipDataX!!.roundToInt().coerceIn(data.entries.indices)
            data.entries.getOrNull(idx)
        } else {
            data.entries.lastOrNull()
        }

        activeEntry?.let { entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isUp = entry.close >= entry.open
                val statusColor = if (isUp) kLineStyle.upColor else kLineStyle.downColor
                val labelStyle = style.legendOptions.textStyle.copy(fontSize = 11.sp)
                val valueStyle = labelStyle.copy(color = statusColor, fontWeight = FontWeight.Bold)

                Text(text = "开: ", style = labelStyle)
                Text(text = String.format("%.2f", entry.open), style = valueStyle)
                Spacer(modifier = Modifier.width(8.dp))

                Text(text = "收: ", style = labelStyle)
                Text(text = String.format("%.2f", entry.close), style = valueStyle)
                Spacer(modifier = Modifier.width(8.dp))

                Text(text = "高: ", style = labelStyle)
                Text(text = String.format("%.2f", entry.high), style = valueStyle)
                Spacer(modifier = Modifier.width(8.dp))

                Text(text = "低: ", style = labelStyle)
                Text(text = String.format("%.2f", entry.low), style = valueStyle)
                Spacer(modifier = Modifier.width(12.dp))

                // 同时绘制当前可见周期的成交量数据
                Text(text = "量: ", style = labelStyle)
                Text(
                    text = String.format("%,.0f", entry.volume),
                    style = labelStyle.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        // ================= 3. 核心 Canvas =================
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
            val maxLimitX = (data.entries.size - 1).coerceAtLeast(0).toFloat()
            viewportState.constrain(minLimitX = -0.5f, maxLimitX = maxLimitX + 0.5f)

            val maxEntriesCount = data.entries.size
            val leftIndex = if (maxEntriesCount == 0) 0 else viewportState.minX.roundToInt().coerceIn(0, maxEntriesCount - 1)
            val rightIndex = if (maxEntriesCount == 0) -1 else viewportState.maxX.roundToInt().coerceIn(0, maxEntriesCount - 1)

            // 依据当前可见区间 [leftIndex, rightIndex] 自适应计算价格 Y 轴 Nice numbers
            var visibleMinY = Float.MAX_VALUE
            var visibleMaxY = Float.MIN_VALUE

            for (i in leftIndex..rightIndex) {
                val entry = data.entries[i]
                if (entry.low < visibleMinY) visibleMinY = entry.low
                if (entry.high > visibleMaxY) visibleMaxY = entry.high
            }

            if (visibleMinY == Float.MAX_VALUE || visibleMaxY == Float.MIN_VALUE) {
                visibleMinY = 0f
                visibleMaxY = 100f
            }

            // 计算价格刻度
            val niceInterval = AxisIntervalCalculator.calculate(
                min = visibleMinY,
                max = visibleMaxY,
                splitNumber = 5
            )

            val currentViewport = ViewportState(
                initialMinX = viewportState.minX,
                initialMaxX = viewportState.maxX,
                initialMinY = niceInterval.min,
                initialMaxY = niceInterval.max
            )

            val mapper = CoordinateMapper(gridRect = gridRect, viewportState = currentViewport)

            val coordinatePlotter = RectangularCoordinate(
                mapper = mapper,
                style = style,
                textMeasurer = textMeasurer,
                xLabels = data.entries.map { it.time },
                yTicks = niceInterval.ticks
            )

            val renderer = remember(mapper, data.entries, style, kLineStyle) {
                KLineChartRenderer(
                    mapper = mapper,
                    entries = data.entries,
                    style = style,
                    kLineStyle = kLineStyle,
                    textMeasurer = textMeasurer,
                    density = density
                )
            }

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
                // 3.1 绘制网格背景
                coordinatePlotter.drawBackground(this)

                // 3.2 绘制蜡烛图及均线、极值标注
                renderer.draw(this, leftIndex, rightIndex)

                // 3.3 绘制坐标轴与刻度文字
                coordinatePlotter.drawAxesAndLabels(this)

                // 3.4 绘制吸附十字光标气泡线
                if (interactionState.isTooltipActive && interactionState.tooltipDataX != null && interactionState.tooltipScreenOffset != null && data.entries.isNotEmpty()) {
                    val closestIndex = interactionState.tooltipDataX!!.roundToInt().coerceIn(data.entries.indices)
                    val entry = data.entries[closestIndex]

                    val sweepX = mapper.toScreenX(closestIndex.toFloat())
                    val sweepY = interactionState.tooltipScreenOffset!!.y.coerceIn(gridRect.top, gridRect.bottom)

                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    val baseColor = style.legendOptions.textStyle.color
                    val labelColor = if (baseColor == Color.Unspecified) {
                        if (style.backgroundColor == Color.Transparent || style.backgroundColor.red > 0.5f) Color.Black else Color.White
                    } else baseColor
                    
                    val cursorStyle = labelColor.copy(alpha = 0.5f)

                    // 1) 绘制垂直、水平十字虚线
                    drawLine(
                        color = cursorStyle,
                        start = Offset(sweepX, gridRect.top),
                        end = Offset(sweepX, gridRect.bottom),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dashEffect
                    )
                    drawLine(
                        color = cursorStyle,
                        start = Offset(gridRect.left, sweepY),
                        end = Offset(gridRect.right, sweepY),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dashEffect
                    )

                    // 2) 绘制时间浮动黑色气泡 (X轴边缘)
                    val timeLayout = textMeasurer.measure(
                        text = entry.time,
                        style = style.xAxisOptions.labelTextStyle.copy(color = Color.White)
                    )
                    val timeWidth = timeLayout.size.width
                    val timeHeight = timeLayout.size.height
                    val timeBubbleLeft = (sweepX - timeWidth / 2f).coerceIn(gridRect.left, gridRect.right - timeWidth)
                    val timeBubbleTop = gridRect.bottom + 4.dp.toPx()

                    drawRoundRect(
                        color = Color(0xFF333333),
                        topLeft = Offset(timeBubbleLeft - 6.dp.toPx(), timeBubbleTop - 2.dp.toPx()),
                        size = Size(timeWidth + 12.dp.toPx(), timeHeight + 4.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                    drawText(
                        textLayoutResult = timeLayout,
                        topLeft = Offset(timeBubbleLeft, timeBubbleTop)
                    )

                    // 3) 绘制价格浮动黑色气泡 (左侧 Y 轴边缘)
                    val priceVal = mapper.toDataY(sweepY)
                    val priceText = String.format("%.2f", priceVal)
                    val priceLayout = textMeasurer.measure(
                        text = priceText,
                        style = style.yAxisOptions.labelTextStyle.copy(color = Color.White)
                    )
                    val priceWidth = priceLayout.size.width
                    val priceHeight = priceLayout.size.height
                    val priceBubbleLeft = gridRect.left - priceWidth - 12.dp.toPx()
                    val priceBubbleTop = sweepY - priceHeight / 2f

                    drawRoundRect(
                        color = Color(0xFF333333),
                        topLeft = Offset(priceBubbleLeft - 6.dp.toPx(), priceBubbleTop - 2.dp.toPx()),
                        size = Size(priceWidth + 12.dp.toPx(), priceHeight + 4.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                    drawText(
                        textLayoutResult = priceLayout,
                        topLeft = Offset(priceBubbleLeft, priceBubbleTop)
                    )
                }
            }
        }
    }
}
