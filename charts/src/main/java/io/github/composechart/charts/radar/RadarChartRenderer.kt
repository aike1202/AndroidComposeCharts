package io.github.composechart.charts.radar

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.composechart.core.data.CoordinateMapper
import io.github.composechart.core.style.ChartStyle
import io.github.composechart.core.style.RadarShape
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * 纯 Compose Canvas 绘制的雷达图渲染器
 */
class RadarChartRenderer(
    private val mapper: CoordinateMapper,
    private val style: ChartStyle,
    private val textMeasurer: TextMeasurer,
    private val density: Density,
    private val animationProgress: Float = 1.0f
) {
    /**
     * 执行雷达图绘制。
     *
     * @param drawScope 绘图域
     * @param indicators 指标维度配置
     * @param visibleSeries 过滤后待显示的覆盖数据系列
     */
    fun draw(
        drawScope: DrawScope,
        indicators: List<RadarIndicator>,
        visibleSeries: List<RadarSeries>,
        animProgress: Float = 1.0f
    ) = with(drawScope) {
        val gridRect = Rect(
            left = with(density) { style.gridOptions.left.toPx() },
            top = with(density) { style.gridOptions.top.toPx() },
            right = size.width - with(density) { style.gridOptions.right.toPx() },
            bottom = size.height - with(density) { style.gridOptions.bottom.toPx() }
        )
        val centerX = gridRect.left + gridRect.width / 2f
        val centerY = gridRect.top + gridRect.height / 2f
        val maxRadius = min(gridRect.width, gridRect.height) / 2f - with(density) { 44.dp.toPx() }



        val dimension = indicators.size
        if (dimension < 3) return@with // 多边形雷达至少需要 3 个维度

        val splitNumber = style.radarOptions.splitNumber
        val gridLineWidthPx = with(density) { style.radarOptions.gridLineWidth.toPx() }
        val axisLineWidthPx = with(density) { style.radarOptions.axisLineWidth.toPx() }

        // 计算各个维度的射线弧度
        val angles = DoubleArray(dimension) { i ->
            -Math.PI / 2.0 + i * (2.0 * Math.PI / dimension)
        }

        // ================= 1. 绘制蛛网格同心背景线 =================
        for (k in 1..splitNumber) {
            val rCurrent = maxRadius * (k.toFloat() / splitNumber)
            if (style.radarOptions.shape == RadarShape.Polygon) {
                // 绘制同心多边形
                val netPath = Path()
                for (i in 0 until dimension) {
                    val theta = angles[i]
                    val px = centerX + rCurrent * cos(theta).toFloat()
                    val py = centerY + rCurrent * sin(theta).toFloat()
                    if (i == 0) netPath.moveTo(px, py) else netPath.lineTo(px, py)
                }
                netPath.close()
                drawPath(
                    path = netPath,
                    color = style.radarOptions.gridColor,
                    style = Stroke(width = gridLineWidthPx)
                )
            } else {
                // 绘制同心圆
                drawCircle(
                    color = style.radarOptions.gridColor,
                    radius = rCurrent,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = gridLineWidthPx)
                )
            }
        }

        // ================= 2. 绘制发散骨架射线 =================
        for (i in 0 until dimension) {
            val theta = angles[i]
            val outerX = centerX + maxRadius * cos(theta).toFloat()
            val outerY = centerY + maxRadius * sin(theta).toFloat()
            drawLine(
                color = style.radarOptions.axisLineColor,
                start = Offset(centerX, centerY),
                end = Offset(outerX, outerY),
                strokeWidth = axisLineWidthPx
            )
        }

        // ================= 3. 绘制 12 点钟主轴标尺刻度值 =================
        if (style.radarOptions.drawScaleLabel && indicators.isNotEmpty()) {
            val baseColor = style.legendOptions.textStyle.color
            val labelColor = if (baseColor == Color.Unspecified) {
                if (style.backgroundColor == Color.Transparent || style.backgroundColor.red > 0.5f) Color.Black else Color.White
            } else baseColor
            val scaleStyle = style.legendOptions.textStyle.copy(fontSize = 9.sp, color = labelColor.copy(alpha = 0.5f))

            val ind0 = indicators[0]
            val range = ind0.max - ind0.min
            for (k in 1..splitNumber) {
                val scaleVal = ind0.min + (k.toFloat() / splitNumber) * range
                val text = String.format("%.1f", scaleVal).removeSuffix(".0")
                val textLayout = textMeasurer.measure(text = text, style = scaleStyle)

                val yOffset = centerY - maxRadius * (k.toFloat() / splitNumber)
                drawText(
                    textLayoutResult = textLayout,
                    topLeft = Offset(centerX + 4.dp.toPx(), yOffset - textLayout.size.height / 2f)
                )
            }
        }

        // ================= 4. 绘制多系列面积覆盖多边形及发光顶点 =================
        val symbolSizePx = with(density) { style.radarOptions.symbolSize.toPx() }

        visibleSeries.forEach { series ->
            val areaPath = Path()
            val vertices = mutableListOf<Offset>()

            for (i in 0 until dimension) {
                val ind = indicators.getOrNull(i) ?: continue
                val rawVal = series.values.getOrNull(i) ?: ind.min
                val theta = angles[i]

                // 对独立轴极值作百分比转换映射
                val range = ind.max - ind.min
                val ratio = if (range > 0f) (rawVal - ind.min) / range else 0f
                val lengthRadius = maxRadius * ratio.coerceIn(0f, 1f) * animProgress

                val vx = centerX + lengthRadius * cos(theta).toFloat()
                val vy = centerY + lengthRadius * sin(theta).toFloat()
                vertices.add(Offset(vx, vy))

                if (i == 0) areaPath.moveTo(vx, vy) else areaPath.lineTo(vx, vy)
            }
            areaPath.close()

            // 4.1 绘制半透明填充背景区
            val fillClr = series.fillColor ?: series.color.copy(alpha = 0.25f)
            drawPath(path = areaPath, color = fillClr)

            // 4.2 绘制边界色描边线
            val strokeWidthPx = with(density) { series.strokeWidth.toPx() }
            drawPath(
                path = areaPath,
                color = series.color,
                style = Stroke(width = strokeWidthPx)
            )

            // 4.3 绘制各折点发光装饰霓虹圈 (Rich Aesthetics)
            vertices.forEach { pt ->
                // 内圈实心原色小点
                drawCircle(
                    color = series.color,
                    radius = symbolSizePx,
                    center = pt
                )
                // 外圈半透明霓虹光晕环
                drawCircle(
                    color = series.color.copy(alpha = 0.3f),
                    radius = symbolSizePx + 3.dp.toPx(),
                    center = pt,
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }
        }

        // ================= 5. 自适应象限对齐绘制外边缘指标文本 =================
        val baseColor = style.legendOptions.textStyle.color
        val labelColor = if (baseColor == Color.Unspecified) {
            if (style.backgroundColor == Color.Transparent || style.backgroundColor.red > 0.5f) Color.Black else Color.White
        } else baseColor
        val labelStyle = style.legendOptions.textStyle.copy(fontSize = 11.sp, color = labelColor)
        val labelGap = 6.dp.toPx()

        for (i in 0 until dimension) {
            val ind = indicators.getOrNull(i) ?: continue
            val theta = angles[i]

            val cosVal = cos(theta).toFloat()
            val sinVal = sin(theta).toFloat()

            // 文本组合格式：“指标名称 (最大值)”
            val labelText = "${ind.name} (${String.format("%.1f", ind.max).removeSuffix(".0")})"
            val textLayout = textMeasurer.measure(text = labelText, style = labelStyle)
            val tw = textLayout.size.width.toFloat()
            val th = textLayout.size.height.toFloat()

            // 最外环稍偏外侧点位
            val vx = centerX + (maxRadius + labelGap) * cosVal
            val vy = centerY + (maxRadius + labelGap) * sinVal

            // 自适应象限对齐计算，杜绝外文字溢出或挤压
            val textX = when {
                cosVal > 0.15f -> vx                // 右侧：向右延伸
                cosVal < -0.15f -> vx - tw          // 左侧：向左对齐
                else -> vx - tw / 2f                // 居中轴：水平居中
            }
            val textY = when {
                sinVal > 0.8f -> vy                 // 底部：顶对齐
                sinVal < -0.8f -> vy - th           // 顶部：底对齐
                else -> vy - th / 2f                // 侧边：垂直居中
            }

            drawText(
                textLayoutResult = textLayout,
                topLeft = Offset(textX, textY)
            )
        }
    }
}
