package io.github.composechart.charts.kline

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.composechart.core.data.CoordinateMapper
import io.github.composechart.core.style.ChartStyle
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 金融 K 线柱体与移动平均线、极值 MarkPoint 具体 Canvas 绘制渲染器
 */
class KLineChartRenderer(
    private val mapper: CoordinateMapper,
    private val entries: List<KLineEntry>,
    private val style: ChartStyle,
    private val kLineStyle: KLineStyle,
    private val textMeasurer: TextMeasurer,
    private val density: Density
) {
    private val grid = mapper.gridRect

    // 预计算 MA 数据
    private val ma5Values = FloatArray(entries.size) { -1f }
    private val ma10Values = FloatArray(entries.size) { -1f }
    private val ma20Values = FloatArray(entries.size) { -1f }

    init {
        for (i in entries.indices) {
            if (i >= 4) {
                var sum = 0f
                for (k in 0..4) sum += entries[i - k].close
                ma5Values[i] = sum / 5f
            }
            if (i >= 9) {
                var sum = 0f
                for (k in 0..9) sum += entries[i - k].close
                ma10Values[i] = sum / 10f
            }
            if (i >= 19) {
                var sum = 0f
                for (k in 0..19) sum += entries[i - k].close
                ma20Values[i] = sum / 20f
            }
        }
    }

    /**
     * 执行具体绘制。
     */
    fun draw(drawScope: DrawScope, leftIndex: Int, rightIndex: Int) = with(drawScope) {
        if (entries.isEmpty() || leftIndex > rightIndex) return@with

        // 1. 获取蜡烛柱大小
        val totalCount = entries.size
        val categoryLength = grid.width
        val itemSpacing = if (totalCount > 1) categoryLength / (totalCount - 1) else categoryLength
        val candleWidth = itemSpacing * kLineStyle.candleWidthRatio
        val shadowWidthPx = with(density) { kLineStyle.shadowLineWidth.toPx() }

        // 2. 绘制开盘收盘蜡烛实体及上下影线
        clipRect(
            left = grid.left,
            top = grid.top,
            right = grid.right,
            bottom = grid.bottom
        ) {
            for (i in leftIndex..rightIndex) {
                val entry = entries.getOrNull(i) ?: continue

            val xCenter = mapper.toScreenX(i.toFloat())
            val yOpen = mapper.toScreenY(entry.open)
            val yClose = mapper.toScreenY(entry.close)
            val yHigh = mapper.toScreenY(entry.high)
            val yLow = mapper.toScreenY(entry.low)

            val isUp = entry.close >= entry.open
            val color = if (isUp) kLineStyle.upColor else kLineStyle.downColor
            val filled = if (isUp) kLineStyle.upFilled else kLineStyle.downFilled

            val candleTop = min(yOpen, yClose)
            val candleBottom = max(yOpen, yClose)
            val candleHeight = abs(yOpen - yClose).coerceAtLeast(1f)

            // 2.1 影线绘制
            drawLine(
                color = color,
                start = Offset(xCenter, yHigh),
                end = Offset(xCenter, yLow),
                strokeWidth = shadowWidthPx
            )

            // 2.2 烛体绘制
            val candleLeft = xCenter - candleWidth / 2f
            if (filled) {
                drawRect(
                    color = color,
                    topLeft = Offset(candleLeft, candleTop),
                    size = Size(candleWidth, candleHeight)
                )
            } else {
                val strokeWidth = with(density) { 1.5.dp.toPx() }
                drawRect(
                    color = color,
                    topLeft = Offset(candleLeft, candleTop),
                    size = Size(candleWidth, candleHeight),
                    style = Stroke(width = strokeWidth)
                )
            }
        }

        // 3. 绘制均线层
        val maWidthPx = with(density) { kLineStyle.maLineWidth.toPx() }
        drawMaLine(this, ma5Values, kLineStyle.ma5Color, maWidthPx, leftIndex, rightIndex)
        drawMaLine(this, ma10Values, kLineStyle.ma10Color, maWidthPx, leftIndex, rightIndex)
        drawMaLine(this, ma20Values, kLineStyle.ma20Color, maWidthPx, leftIndex, rightIndex)
        }

        // 4. 动态极值价格 MarkPoint
        drawExtremesMark(this, leftIndex, rightIndex)
    }

    private fun drawMaLine(
        drawScope: DrawScope,
        maValues: FloatArray,
        color: Color,
        lineWidth: Float,
        leftIndex: Int,
        rightIndex: Int
    ) = with(drawScope) {
        val startIdx = leftIndex.coerceIn(0, maValues.size - 1)
        val endIdx = (rightIndex - 1).coerceIn(0, maValues.size - 1)

        for (i in startIdx..endIdx) {
            val y1 = maValues[i]
            val y2 = maValues.getOrNull(i + 1) ?: -1f
            if (y1 > 0f && y2 > 0f) {
                drawLine(
                    color = color,
                    start = Offset(mapper.toScreenX(i.toFloat()), mapper.toScreenY(y1)),
                    end = Offset(mapper.toScreenX((i + 1).toFloat()), mapper.toScreenY(y2)),
                    strokeWidth = lineWidth
                )
            }
        }
    }

    private fun drawExtremesMark(drawScope: DrawScope, leftIndex: Int, rightIndex: Int) = with(drawScope) {
        var highestVal = Float.MIN_VALUE
        var highestIdx = -1
        var lowestVal = Float.MAX_VALUE
        var lowestIdx = -1

        for (i in leftIndex..rightIndex) {
            val entry = entries.getOrNull(i) ?: continue
            if (entry.high > highestVal) {
                highestVal = entry.high
                highestIdx = i
            }
            if (entry.low < lowestVal) {
                lowestVal = entry.low
                lowestIdx = i
            }
        }

        val baseColor = style.legendOptions.textStyle.color
        val labelColor = if (baseColor == Color.Unspecified) {
            if (style.backgroundColor == Color.Transparent || style.backgroundColor.red > 0.5f) Color.Black else Color.White
        } else baseColor
        val textStyle = style.legendOptions.textStyle.copy(fontSize = 10.sp, color = labelColor)
        val lineEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)

        // 1. 最高价标注横向虚线及标签
        if (highestIdx != -1) {
            val hx = mapper.toScreenX(highestIdx.toFloat())
            if (hx in grid.left..grid.right) {
                val hy = mapper.toScreenY(highestVal)
                val text = "最高: ${String.format("%.2f", highestVal)}"
                val layout = textMeasurer.measure(text = text, style = textStyle)

                val isLeft = highestIdx > (leftIndex + rightIndex) / 2
                val lineLength = with(density) { 15.dp.toPx() }
                val dir = if (isLeft) -1f else 1f

                drawLine(
                    color = labelColor.copy(alpha = 0.5f),
                    start = Offset(hx, hy),
                    end = Offset(hx + lineLength * dir, hy),
                    strokeWidth = with(density) { 0.8.dp.toPx() },
                    pathEffect = lineEffect
                )

                val tx = if (isLeft) hx - lineLength - layout.size.width - 2.dp.toPx() else hx + lineLength + 2.dp.toPx()
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(tx, hy - layout.size.height / 2f)
                )
            }
        }

        // 2. 最低价标注横向虚线及标签
        if (lowestIdx != -1) {
            val lx = mapper.toScreenX(lowestIdx.toFloat())
            if (lx in grid.left..grid.right) {
                val ly = mapper.toScreenY(lowestVal)
                val text = "最低: ${String.format("%.2f", lowestVal)}"
                val layout = textMeasurer.measure(text = text, style = textStyle)

                val isLeft = lowestIdx > (leftIndex + rightIndex) / 2
                val lineLength = with(density) { 15.dp.toPx() }
                val dir = if (isLeft) -1f else 1f

                drawLine(
                    color = labelColor.copy(alpha = 0.5f),
                    start = Offset(lx, ly),
                    end = Offset(lx + lineLength * dir, ly),
                    strokeWidth = with(density) { 0.8.dp.toPx() },
                    pathEffect = lineEffect
                )

                val tx = if (isLeft) lx - lineLength - layout.size.width - 2.dp.toPx() else lx + lineLength + 2.dp.toPx()
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(tx, ly - layout.size.height / 2f)
                )
            }
        }
    }
}
