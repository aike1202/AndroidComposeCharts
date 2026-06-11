package io.github.composechart.charts.gauge

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.composechart.core.style.ChartStyle
import kotlin.math.cos
import kotlin.math.sin

/**
 * 仪表盘系列渲染引擎，负责绘制背景环、彩色段、刻度线、刻度文字和旋转指针等核心图层。
 */
class GaugeChartRenderer(
    private val data: GaugeChartData,
    private val options: GaugeOptions,
    private val animatedFraction: Float, // 动画进度 0.0f..1.0f，代表当前数值的占比
    private val style: ChartStyle,
    private val textMeasurer: TextMeasurer,
    private val isDark: Boolean
) {

    /**
     * 根据当前占比 fraction 获取对应色彩区间的颜色
     */
    fun getCurrentColor(fraction: Float): Color {
        for (pair in options.axisLineColors) {
            if (fraction <= pair.first) {
                return pair.second
            }
        }
        return options.axisLineColors.lastOrNull()?.second ?: Color.Gray
    }

    /**
     * 绘制仪表盘主体
     */
    fun draw(drawScope: DrawScope) = with(drawScope) {
        val density = drawScope.density

        // 1. 确定原点及大半径
        val widthPx = size.width
        val heightPx = size.height
        val center = Offset(widthPx / 2f, heightPx * 0.48f)
        val radius = minOf(widthPx, heightPx) * 0.42f

        val axisLineWidthPx = options.axisLineWidth.toPx()
        val innerEdgeRadius = radius - axisLineWidthPx / 2f // 色环内侧边缘半径

        // ================= 2. 绘制圆弧刻度色环 =================
        // 2.1 绘制灰色背景底环
        val rect = Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius)
        drawArc(
            color = options.axisBgColor,
            startAngle = options.startAngle,
            sweepAngle = options.sweepAngle,
            useCenter = false,
            topLeft = rect.topLeft,
            size = rect.size,
            style = Stroke(width = axisLineWidthPx, cap = StrokeCap.Round)
        )

        // 2.2 顺时针分段绘制彩色进度弧
        val endFraction = if (options.isProgress) animatedFraction else 1.0f
        var lastFraction = 0f
        for (pair in options.axisLineColors) {
            val currentFraction = pair.first.coerceIn(0f, 1f)
            if (currentFraction > lastFraction) {
                val startF = lastFraction
                val endF = minOf(currentFraction, endFraction)
                if (endF > startF) {
                    val sectionStartAngle = options.startAngle + startF * options.sweepAngle
                    val sectionSweepAngle = (endF - startF) * options.sweepAngle
                    val cap = if (options.isProgress && (startF == 0f || endF == endFraction)) {
                        StrokeCap.Round
                    } else {
                        StrokeCap.Butt
                    }
                    drawArc(
                        color = pair.second,
                        startAngle = sectionStartAngle,
                        sweepAngle = sectionSweepAngle,
                        useCenter = false,
                        topLeft = rect.topLeft,
                        size = rect.size,
                        style = Stroke(width = axisLineWidthPx, cap = cap)
                    )
                }
                lastFraction = currentFraction
            }
        }

        // ================= 3. 绘制刻度短线与数值数字 =================
        val totalTicks = options.tickCount
        val subTicks = options.subTickCount

        val tickLengthPx = options.tickLength.toPx()
        val tickWidthPx = options.tickWidth.toPx()
        val subTickLengthPx = options.subTickLength.toPx()
        val subTickWidthPx = options.subTickWidth.toPx()

        val startMarginPx = 3.dp.toPx() // 刻度与色环内侧边缘留白

        for (i in 0..totalTicks) {
            // 大刻度角度
            val angleDeg = options.startAngle + (i.toFloat() / totalTicks) * options.sweepAngle
            val angleRad = Math.toRadians(angleDeg.toDouble())

            val cosA = cos(angleRad).toFloat()
            val sinA = sin(angleRad).toFloat()

            // 3.1 绘制主大刻度线
            if (options.showTicks) {
                val tickStartR = innerEdgeRadius - startMarginPx
                val tickEndR = tickStartR - tickLengthPx

                drawLine(
                    color = options.tickColor,
                    start = Offset(center.x + tickStartR * cosA, center.y + tickStartR * sinA),
                    end = Offset(center.x + tickEndR * cosA, center.y + tickEndR * sinA),
                    strokeWidth = tickWidthPx
                )
            }

            // 3.2 绘制大刻度上的数字
            if (options.showTickLabels) {
                val tickVal = data.min + (i.toFloat() / totalTicks) * (data.max - data.min)
                val labelText = options.customLabels?.getOrNull(i)
                    ?: String.format("%.1f", tickVal).removeSuffix(".0")

                val labelTextStyle = options.tickLabelTextStyle ?: TextStyle(
                    color = if (isDark) Color(0xFFB0B3C0) else Color(0xFF666666),
                    fontSize = 10.sp
                )
                val labelLayout = textMeasurer.measure(labelText, style = labelTextStyle)

                // 文字排在刻度线内侧 8.dp 处
                val textR = innerEdgeRadius - startMarginPx - tickLengthPx - 8.dp.toPx()
                val textCenter = Offset(center.x + textR * cosA, center.y + textR * sinA)

                drawText(
                    textLayoutResult = labelLayout,
                    topLeft = Offset(
                        textCenter.x - labelLayout.size.width / 2f,
                        textCenter.y - labelLayout.size.height / 2f
                    )
                )
            }

            // 3.3 绘制主刻度之间的细分小刻度
            if (options.showTicks && i < totalTicks) {
                for (j in 1 until subTicks) {
                    val subAngleDeg = options.startAngle +
                            ((i.toFloat() + j.toFloat() / subTicks) / totalTicks) * options.sweepAngle
                    val subAngleRad = Math.toRadians(subAngleDeg.toDouble())

                    val cosS = cos(subAngleRad).toFloat()
                    val sinS = sin(subAngleRad).toFloat()

                    val subStartR = innerEdgeRadius - startMarginPx
                    val subEndR = subStartR - subTickLengthPx

                    drawLine(
                        color = options.subTickColor,
                        start = Offset(center.x + subStartR * cosS, center.y + subStartR * sinS),
                        end = Offset(center.x + subEndR * cosS, center.y + subEndR * sinS),
                        strokeWidth = subTickWidthPx
                    )
                }
            }
        }

        if (options.showPointer) {
            // ================= 4. 绘制三角形指针 =================
            // 指针指向位置（以动画占比 animatedFraction 为基准）
            val pointerAngleDeg = options.startAngle + animatedFraction * options.sweepAngle
            val pointerAngleRad = Math.toRadians(pointerAngleDeg.toDouble())

            val cosP = cos(pointerAngleRad).toFloat()
            val sinP = sin(pointerAngleRad).toFloat()

            val pointerColor = options.pointerColor ?: getCurrentColor(animatedFraction)
            val halfW = options.pointerWidth.toPx() / 2f

            if (options.pointerType == PointerType.ShortTriangle) {
                // 悬空并贴附在内圈轨道的短三角形指针
                val tipR = innerEdgeRadius - startMarginPx - 4.dp.toPx()
                val baseR = tipR - 12.dp.toPx() // 高度约 12.dp

                val tip = Offset(center.x + tipR * cosP, center.y + tipR * sinP)
                val basePt = Offset(center.x + baseR * cosP, center.y + baseR * sinP)

                // 垂直于 cosP, sinP 向量的法向量是 -sinP, cosP
                val p1 = Offset(basePt.x + halfW * (-sinP), basePt.y + halfW * cosP)
                val p2 = Offset(basePt.x - halfW * (-sinP), basePt.y - halfW * cosP)

                val pointerPath = Path().apply {
                    moveTo(p1.x, p1.y)
                    lineTo(tip.x, tip.y)
                    lineTo(p2.x, p2.y)
                    close()
                }
                drawPath(pointerPath, color = pointerColor, style = Fill)
            } else {
                // 常规从圆心延伸至轨道的长角指针
                val pointerLength = innerEdgeRadius * options.pointerLengthRatio
                val tip = Offset(center.x + pointerLength * cosP, center.y + pointerLength * sinP)

                val p1 = Offset(center.x + halfW * sinP, center.y - halfW * cosP)
                val p2 = Offset(center.x - halfW * sinP, center.y + halfW * cosP)

                val pointerPath = Path().apply {
                    moveTo(p1.x, p1.y)
                    lineTo(tip.x, tip.y)
                    lineTo(p2.x, p2.y)
                    close()
                }
                drawPath(pointerPath, color = pointerColor, style = Fill)

                // 绘制指针上的立体高亮线（细折线）
                drawLine(
                    color = Color.White.copy(alpha = 0.45f),
                    start = center,
                    end = tip,
                    strokeWidth = 1.dp.toPx()
                )
            }

            // ================= 5. 绘制中心装饰轴承盖 (Layered Mechanical Cap) =================
            if (options.pointerType == PointerType.Triangle) {
                val capRadius = options.centerCircleRadius.toPx()
                val capColor = options.centerCircleColor ?: getCurrentColor(animatedFraction)

                // 5.1 第一层：外底白色圆圈（带细灰边）
                drawCircle(
                    color = if (isDark) Color(0xFF2C2C2E) else Color.White,
                    radius = capRadius + 2.dp.toPx(),
                    center = center
                )
                drawCircle(
                    color = if (isDark) Color(0xFF48484A) else Color(0xFFD1D1D6),
                    radius = capRadius + 2.dp.toPx(),
                    center = center,
                    style = Stroke(width = 0.8.dp.toPx())
                )

                // 5.2 第二层：中心实色进度圆
                drawCircle(
                    color = capColor,
                    radius = capRadius,
                    center = center
                )

                // 5.3 第三层：最里层金属小圆扣
                drawCircle(
                    color = if (isDark) Color(0xFF1C1C1E) else Color.White.copy(alpha = 0.9f),
                    radius = 3.dp.toPx(),
                    center = center
                )
            }
        }
    }
}
