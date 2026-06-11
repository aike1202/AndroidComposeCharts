package io.github.composechart.charts.polar

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.composechart.core.data.PolarCoordinateMapper
import io.github.composechart.core.style.ChartStyle
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * 极坐标系柱状图渲染器，支持 Radial 径向扇区绘制和 Tangential 切向圆环段绘制，以及自适应标签旋转排版。
 */
class PolarBarChartRenderer(
    private val mapper: PolarCoordinateMapper,
    private val style: ChartStyle,
    private val textMeasurer: TextMeasurer,
    private val density: Density,
    private val allSeries: List<PolarBarSeries>,
    private val horizontal: Boolean, // false = Radial 模式 (分类在角度，数值在半径)；true = Tangential 模式 (分类在半径，数值在角度)
    private val animationProgress: Float = 1.0f
) {
    private val opts = mapper.polarOptions
    private val grid = mapper.gridRect

    /**
     * 执行极坐标柱体与标签绘制。
     */
    fun draw(drawScope: DrawScope) = with(drawScope) {
        if (allSeries.isEmpty()) return@with

        val categoryCount = allSeries.maxOf { it.values.size }
        if (categoryCount == 0) return@with

        val totalSweep = opts.endAngle - opts.startAngle
        val labelStyle = style.legendOptions.textStyle.copy(
            fontSize = 9.sp,
            color = if (style.legendOptions.textStyle.color == Color.Unspecified) {
                if (style.backgroundColor == Color.Transparent || style.backgroundColor.red > 0.5f) Color.Black else Color.White
            } else style.legendOptions.textStyle.color
        )

        val barWidthRatio = 0.6f // 柱体宽度占比

        clipRect(
            left = grid.left,
            top = grid.top,
            right = grid.right,
            bottom = grid.bottom
        ) {
            if (!horizontal) {
                // ================= 1. Radial 模式 (角度类目轴 / 扇形柱状图) =================
                val angleSpan = totalSweep / categoryCount
                val sweepBar = angleSpan * barWidthRatio
                val seriesSize = allSeries.size
                val singleBarAngle = sweepBar / seriesSize

                for (categoryIdx in 0 until categoryCount) {
                    val startAngleBase = opts.startAngle + categoryIdx * angleSpan
                    val centerAngle = startAngleBase + angleSpan / 2f
                    val barStartAngle = centerAngle - sweepBar / 2f

                    allSeries.forEachIndexed { sIdx, series ->
                        val barValObj = series.values.getOrNull(categoryIdx) ?: return@forEachIndexed
                        val rawValue = barValObj.value

                        // 角度区间
                        val aStart = barStartAngle + sIdx * singleBarAngle
                        val aSweep = singleBarAngle

                        // 映射半径物理大小
                        val rStartPx = mapper.getPhysicalRadius(0f)
                        val rEndPx = mapper.getPhysicalRadius(rawValue * animationProgress)

                        if (abs(rEndPx - rStartPx) > 1f) {
                            // 组装扇区路径
                            val path = Path()
                            val rectOuter = Rect(
                                left = mapper.centerX - rEndPx,
                                top = mapper.centerY - rEndPx,
                                right = mapper.centerX + rEndPx,
                                bottom = mapper.centerY + rEndPx
                            )
                            path.arcTo(rect = rectOuter, startAngleDegrees = aStart, sweepAngleDegrees = aSweep, forceMoveTo = true)
                            if (rStartPx > 0f) {
                                val rectInner = Rect(
                                    left = mapper.centerX - rStartPx,
                                    top = mapper.centerY - rStartPx,
                                    right = mapper.centerX + rStartPx,
                                    bottom = mapper.centerY + rStartPx
                                )
                                path.arcTo(rect = rectInner, startAngleDegrees = aStart + aSweep, sweepAngleDegrees = -aSweep, forceMoveTo = false)
                            } else {
                                path.lineTo(mapper.centerX, mapper.centerY)
                            }
                            path.close()

                            // 绘制扇面实体
                            drawPath(path = path, color = series.color)

                            // 绘制径向标签文本
                            if (series.labelPosition != PolarLabelPosition.None) {
                                val labelAngle = aStart + aSweep / 2f
                                val rLabel = when (series.labelPosition) {
                                    PolarLabelPosition.Middle -> (rStartPx + rEndPx) / 2f
                                    else -> rEndPx + 6.dp.toPx()
                                }

                                val rad = Math.toRadians(labelAngle.toDouble())
                                val px = mapper.centerX + rLabel * cos(rad).toFloat()
                                val py = mapper.centerY + rLabel * sin(rad).toFloat()

                                val text = String.format("%.1f", rawValue).removeSuffix(".0")
                                val textLayout = textMeasurer.measure(text = text, style = labelStyle)
                                val tw = textLayout.size.width
                                val th = textLayout.size.height

                                // 调整文本旋转以易于阅读（避免字倒过来）
                                var drawAngle = labelAngle
                                if (drawAngle in 90f..270f || drawAngle in -270f..-90f) {
                                    drawAngle += 180f
                                }

                                withTransform({
                                    rotate(degrees = drawAngle, pivot = Offset(px, py))
                                }) {
                                    drawText(
                                        textLayoutResult = textLayout,
                                        topLeft = Offset(px - tw / 2f, py - th / 2f)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // ================= 2. Tangential 模式 (半径类目轴 / 圆环柱状图) =================
                val trackWidth = mapper.maxRadiusPx / categoryCount
                val barWidth = trackWidth * barWidthRatio / 2f

                for (categoryIdx in 0 until categoryCount) {
                    val rCenter = trackWidth * (categoryIdx + 0.5f)
                    val rStartPx = rCenter - barWidth
                    val rEndPx = rCenter + barWidth

                    allSeries.forEach { series ->
                        val barValObj = series.values.getOrNull(categoryIdx) ?: return@forEach
                        val rawValue = barValObj.value

                        // 扫掠角度大小
                        val sweepAngle = (rawValue / mapper.maxRadiusVal).coerceIn(0f, 1f) * totalSweep * animationProgress

                        if (abs(sweepAngle) > 0.5f) {
                            // 组装圆弧圆环路径
                            val path = Path()
                            val rectOuter = Rect(
                                left = mapper.centerX - rEndPx,
                                top = mapper.centerY - rEndPx,
                                right = mapper.centerX + rEndPx,
                                bottom = mapper.centerY + rEndPx
                            )
                            path.arcTo(rect = rectOuter, startAngleDegrees = opts.startAngle, sweepAngleDegrees = sweepAngle, forceMoveTo = true)

                            val rectInner = Rect(
                                left = mapper.centerX - rStartPx,
                                top = mapper.centerY - rStartPx,
                                right = mapper.centerX + rStartPx,
                                bottom = mapper.centerY + rStartPx
                            )
                            path.arcTo(rect = rectInner, startAngleDegrees = opts.startAngle + sweepAngle, sweepAngleDegrees = -sweepAngle, forceMoveTo = false)
                            path.close()

                            // 绘制同心圆弧柱子
                            drawPath(path = path, color = series.color)

                            // 绘制切向标签文本
                            if (series.labelPosition != PolarLabelPosition.None) {
                                val labelAngle = opts.startAngle + when (series.labelPosition) {
                                    PolarLabelPosition.Middle -> sweepAngle / 2f
                                    else -> sweepAngle
                                }

                                val rad = Math.toRadians(labelAngle.toDouble())
                                val px = mapper.centerX + rCenter * cos(rad).toFloat()
                                val py = mapper.centerY + rCenter * sin(rad).toFloat()

                                val text = String.format("%.1f", rawValue).removeSuffix(".0")
                                val textLayout = textMeasurer.measure(text = text, style = labelStyle)
                                val tw = textLayout.size.width
                                val th = textLayout.size.height

                                // 切线旋转角度为 labelAngle + 90f
                                var drawAngle = labelAngle + 90f
                                if (drawAngle in 90f..270f || drawAngle in -270f..-90f || drawAngle in 450f..630f) {
                                    drawAngle += 180f
                                }

                                withTransform({
                                    rotate(degrees = drawAngle, pivot = Offset(px, py))
                                }) {
                                    drawText(
                                        textLayoutResult = textLayout,
                                        topLeft = Offset(px - tw / 2f, py - th / 2f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
