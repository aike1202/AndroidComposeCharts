package io.github.composechart.core.plot

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.composechart.core.data.PolarCoordinateMapper
import io.github.composechart.core.style.ChartStyle
import kotlin.math.cos
import kotlin.math.sin

/**
 * 极坐标系下的轴网格及标签绘制器。
 */
class PolarCoordinate(
    private val mapper: PolarCoordinateMapper,
    private val style: ChartStyle,
    private val textMeasurer: TextMeasurer,
    private val density: Density,
    private val xLabels: List<String>, // 角度轴的类目标签
    private val rTicks: List<Float>     // 径向轴的刻度数值
) {
    private val opts = mapper.polarOptions
    private val grid = mapper.gridRect

    /**
     * 绘制极坐标网格背景：同心圆弧段与放射角度射线线。
     */
    fun drawBackground(drawScope: DrawScope) = with(drawScope) {
        if (!opts.show) return@with

        val totalSweep = opts.endAngle - opts.startAngle
        val isClosedCircle = totalSweep >= 360f

        val gridLineWidthPx = with(density) { opts.gridLineWidth.toPx() }
        val axisLineWidthPx = with(density) { opts.axisLineWidth.toPx() }

        // 1. 绘制同心圆环（或圆弧段）网格线
        val splitCount = opts.radiusStepNumber
        for (k in 1..splitCount) {
            val rPx = mapper.maxRadiusPx * (k.toFloat() / splitCount)
            if (isClosedCircle) {
                // 闭合极坐标，绘制同心圆
                drawCircle(
                    color = opts.gridColor,
                    radius = rPx,
                    center = Offset(mapper.centerX, mapper.centerY),
                    style = Stroke(width = gridLineWidthPx)
                )
            } else {
                // 非闭合，绘制同心扇形弧段
                drawArc(
                    color = opts.gridColor,
                    startAngle = opts.startAngle,
                    sweepAngle = totalSweep,
                    useCenter = false,
                    topLeft = Offset(mapper.centerX - rPx, mapper.centerY - rPx),
                    size = Size(rPx * 2f, rPx * 2f),
                    style = Stroke(width = gridLineWidthPx)
                )
            }
        }

        // 2. 绘制放射状角度线
        val categoryCount = xLabels.size
        if (categoryCount > 0) {
            val endIdx = if (isClosedCircle) categoryCount else categoryCount + 1
            for (i in 0 until endIdx) {
                // 在每个分类边界绘制射线线
                val anglePct = i.toFloat() / categoryCount
                val outerPt = mapper.toScreenPoint(mapper.maxRadiusVal, anglePct)
                drawLine(
                    color = opts.axisLineColor,
                    start = Offset(mapper.centerX, mapper.centerY),
                    end = outerPt,
                    strokeWidth = axisLineWidthPx
                )
            }
        }
    }

    /**
     * 绘制极轴上的刻度数值以及最外环的角度分类标签。
     */
    fun drawAxesAndLabels(drawScope: DrawScope) = with(drawScope) {
        if (!opts.show) return@with

        val labelStyle = style.legendOptions.textStyle.copy(
            fontSize = 10.sp,
            color = if (style.legendOptions.textStyle.color == Color.Unspecified) {
                if (style.backgroundColor == Color.Transparent || style.backgroundColor.red > 0.5f) Color.Black else Color.White
            } else style.legendOptions.textStyle.color
        )

        // 1. 绘制径向轴标签文本（从中心沿着起始射线由内向外书写）
        if (opts.drawRadiusLabel && rTicks.isNotEmpty()) {
            val anglePct = 0f
            val totalSweep = opts.endAngle - opts.startAngle
            val startRad = Math.toRadians(opts.startAngle.toDouble())
            
            // 稍稍垂直于射线方向做微调，以防数字贴线
            val labelOffsetAngle = Math.toRadians((opts.startAngle + 8f).toDouble())

            for (tick in rTicks) {
                val text = String.format("%.1f", tick).removeSuffix(".0")
                val textLayout = textMeasurer.measure(text = text, style = labelStyle.copy(fontSize = 9.sp))
                
                val dist = mapper.getPhysicalRadius(tick)
                if (dist > 10f) {
                    val px = mapper.centerX + dist * cos(labelOffsetAngle).toFloat()
                    val py = mapper.centerY + dist * sin(labelOffsetAngle).toFloat()
                    
                    drawText(
                        textLayoutResult = textLayout,
                        topLeft = Offset(px - textLayout.size.width / 2f, py - textLayout.size.height / 2f)
                    )
                }
            }
        }

        // 2. 绘制最外环的角度分类标签文本（象限对齐算法）
        if (opts.drawAngleLabel && xLabels.isNotEmpty()) {
            val totalSweep = opts.endAngle - opts.startAngle
            val categoryCount = xLabels.size
            val labelGapPx = with(density) { 8.dp.toPx() }

            for (i in xLabels.indices) {
                // 每个分类的中心点物理角度
                val anglePct = (i.toFloat() + 0.5f) / categoryCount
                val angleDegree = mapper.getPhysicalAngle(anglePct)
                val angleRad = Math.toRadians(angleDegree.toDouble())

                val cosVal = cos(angleRad).toFloat()
                val sinVal = sin(angleRad).toFloat()

                // 最外层边界再往外偏移一段距离作为文字点
                val vx = mapper.centerX + (mapper.maxRadiusPx + labelGapPx) * cosVal
                val vy = mapper.centerY + (mapper.maxRadiusPx + labelGapPx) * sinVal

                val text = xLabels[i]
                val textLayout = textMeasurer.measure(text = text, style = labelStyle)
                val tw = textLayout.size.width.toFloat()
                val th = textLayout.size.height.toFloat()

                // 象限自适应排版
                val textX = when {
                    cosVal > 0.15f -> vx
                    cosVal < -0.15f -> vx - tw
                    else -> vx - tw / 2f
                }
                val textY = when {
                    sinVal > 0.8f -> vy
                    sinVal < -0.8f -> vy - th
                    else -> vy - th / 2f
                }

                drawText(
                    textLayoutResult = textLayout,
                    topLeft = Offset(textX, textY)
                )
            }
        }
    }
}
