package io.github.composechart.charts.funnel

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.composechart.core.style.ChartStyle

/**
 * 漏斗图/金字塔图 Canvas 渲染器，负责倒角梯形块绘制、引线排布及相邻转化率气泡标定。
 */
class FunnelChartRenderer(
    private val slices: List<FunnelSlice>,
    private val options: FunnelOptions,
    private val style: ChartStyle,
    private val textMeasurer: TextMeasurer,
    private val sliceAlphaProgresses: List<Float>,       // 坠落透明度动画 (0f-1f)
    private val sliceYOffsetProgresses: List<Float>,     // 坠落垂直插值位移 (0f-1f)
    private val hoveredSliceIndex: Int?,                 // 触碰高亮的漏斗块索引
    private val isDark: Boolean
) {

    /**
     * 绘制整个漏斗图结构
     */
    fun draw(drawScope: DrawScope) = with(drawScope) {
        val density = drawScope.density
        val colorPalette = style.colorPalette

        val gridRect = Rect(
            left = 40.dp.toPx(),
            top = 48.dp.toPx(),
            right = size.width - 40.dp.toPx(),
            bottom = size.height - 40.dp.toPx()
        )

        val n = slices.size
        if (n == 0) return@with

        val gapPx = options.gap.toPx()
        val totalHeight = gridRect.height - (n - 1) * gapPx
        val sliceHeight = totalHeight / n

        // 1. 计算各层级归一化的宽度比例 (上底与下底)
        val maxValue = slices.maxOfOrNull { it.value } ?: 100f
        val minW = options.minWidthRatio
        val maxW = options.maxWidthRatio

        val widths = FloatArray(n) { i ->
            val ratio = if (maxValue > 0f) slices[i].value / maxValue else 0.5f
            minW + (maxW - minW) * ratio
        }

        // 保存每层的顶点 X/Y，用于后续绘制连接线及引线文本
        val topYs = FloatArray(n)
        val bottomYs = FloatArray(n)
        val topWidths = FloatArray(n)
        val bottomWidths = FloatArray(n)

        // 中轴 X 坐标
        val centerX = gridRect.left + gridRect.width / 2f

        for (i in 0 until n) {
            val progressY = sliceYOffsetProgresses.getOrElse(i) { 1.0f }
            
            // 坠落动画：从上方 grid.top - 80dp 坠落到目标位置
            val targetTopY = gridRect.top + i * (sliceHeight + gapPx)
            val initialTopY = gridRect.top - 80.dp.toPx()
            topYs[i] = initialTopY + (targetTopY - initialTopY) * progressY
            bottomYs[i] = topYs[i] + sliceHeight

            // 梯形连续拼接算法：上底宽度等于上一层的下底宽度
            topWidths[i] = if (i == 0) {
                if (options.sort == FunnelSort.Descending) maxW else widths[i] * 0.85f
            } else {
                bottomWidths[i - 1]
            }

            bottomWidths[i] = if (i == n - 1) {
                if (options.sort == FunnelSort.Descending) widths[i] * 0.22f else widths[i]
            } else {
                widths[i]
            }
        }

        // 2. 依次绘制每一个倒角梯形块
        for (i in 0 until n) {
            val alpha = sliceAlphaProgresses.getOrElse(i) { 1.0f }
            val isHovered = hoveredSliceIndex == i

            // 选中高亮膨胀 6% 宽度
            val wTopScale = if (isHovered) topWidths[i] * 1.06f else topWidths[i]
            val wBottomScale = if (isHovered) bottomWidths[i] * 1.06f else bottomWidths[i]

            val wTopPx = gridRect.width * wTopScale
            val wBottomPx = gridRect.width * wBottomScale

            val yTop = topYs[i]
            val yBottom = bottomYs[i]

            // 2.1 极坐标对齐矩阵算式
            val (xTL, xTR) = getAlignedX(centerX, wTopPx, gridRect, options.align)
            val (xBL, xBR) = getAlignedX(centerX, wBottomPx, gridRect, options.align)

            val baseColor = slices[i].color ?: colorPalette[i % colorPalette.size]
            val drawColor = baseColor.copy(alpha = baseColor.alpha * alpha)

            // 2.2 原生 Paint 接入 CornerPathEffect 渲染平滑倒角
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    color = drawColor.toArgb()
                    style = android.graphics.Paint.Style.FILL
                    if (options.cornerRadius.toPx() > 0f) {
                        pathEffect = android.graphics.CornerPathEffect(options.cornerRadius.toPx())
                    }
                }

                // 2.3 触碰 hover 高亮时，追加一层外发光投影效果
                if (isHovered) {
                    paint.setShadowLayer(
                        10.dp.toPx(),
                        0f, 0f,
                        drawColor.copy(alpha = 0.85f).toArgb()
                    )
                }

                val nativePath = android.graphics.Path().apply {
                    moveTo(xTL, yTop)
                    lineTo(xTR, yTop)
                    lineTo(xBR, yBottom)
                    lineTo(xBL, yBottom)
                    close()
                }
                canvas.nativeCanvas.drawPath(nativePath, paint)
            }

            // 2.4 若被 Hover，绘制一圈精细的白边框强化选中感
            if (isHovered) {
                drawIntoCanvas { canvas ->
                    val strokePaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        color = (if (isDark) Color.White else baseColor).toArgb()
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = 2.dp.toPx()
                        if (options.cornerRadius.toPx() > 0f) {
                            pathEffect = android.graphics.CornerPathEffect(options.cornerRadius.toPx())
                        }
                    }
                    val strokePath = android.graphics.Path().apply {
                        moveTo(xTL, yTop)
                        lineTo(xTR, yTop)
                        lineTo(xBR, yBottom)
                        lineTo(xBL, yBottom)
                        close()
                    }
                    canvas.nativeCanvas.drawPath(strokePath, strokePaint)
                }
            }

            // ================= 3. 绘制两侧的引导线与阶段文本 =================
            if (options.showLabels) {
                // 计算腰部中点作为引导线起点
                val yMid = (yTop + yBottom) / 2f
                val xLeftEdge = (xTL + xBL) / 2f
                val xRightEdge = (xTR + xBR) / 2f

                // 文字及线条颜色自适应
                val lineLabelColor = if (isDark) Color(0xFF8E8E93) else Color(0xFF666666)
                val textStyle = options.labelTextStyle ?: TextStyle(
                    color = if (isDark) Color.White else Color.Black,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )

                val labelText = "${slices[i].name} (${String.format("%.0f", slices[i].value)})"
                val labelLayout = textMeasurer.measure(labelText, style = textStyle)

                when (options.align) {
                    FunnelAlign.Center -> {
                        // 居中漏斗：左边画引导折线和阶段文字
                        val endX = gridRect.left - 12.dp.toPx()
                        drawLine(
                            color = lineLabelColor.copy(alpha = 0.5f),
                            start = Offset(xLeftEdge - 4.dp.toPx(), yMid),
                            end = Offset(endX, yMid),
                            strokeWidth = 0.8.dp.toPx()
                        )
                        drawText(
                            textLayoutResult = labelLayout,
                            topLeft = Offset(endX - labelLayout.size.width - 4.dp.toPx(), yMid - labelLayout.size.height / 2f)
                        )
                    }
                    FunnelAlign.Left -> {
                        // 左边对齐：所有文字绘制在右倾斜侧
                        val endX = gridRect.right + 8.dp.toPx()
                        drawLine(
                            color = lineLabelColor.copy(alpha = 0.5f),
                            start = Offset(xRightEdge + 4.dp.toPx(), yMid),
                            end = Offset(endX, yMid),
                            strokeWidth = 0.8.dp.toPx()
                        )
                        drawText(
                            textLayoutResult = labelLayout,
                            topLeft = Offset(endX + 4.dp.toPx(), yMid - labelLayout.size.height / 2f)
                        )
                    }
                    FunnelAlign.Right -> {
                        // 右边对齐：所有文字绘制在左倾斜侧
                        val endX = gridRect.left - 8.dp.toPx()
                        drawLine(
                            color = lineLabelColor.copy(alpha = 0.5f),
                            start = Offset(xLeftEdge - 4.dp.toPx(), yMid),
                            end = Offset(endX, yMid),
                            strokeWidth = 0.8.dp.toPx()
                        )
                        drawText(
                            textLayoutResult = labelLayout,
                            topLeft = Offset(endX - labelLayout.size.width - 4.dp.toPx(), yMid - labelLayout.size.height / 2f)
                        )
                    }
                }
            }

            // ================= 4. 绘制转化率胶囊文字 (Conversion Rate) =================
            if (options.showConversion && i < n - 1) {
                val nextSlice = slices[i + 1]
                val rate = if (slices[i].value > 0f) (nextSlice.value / slices[i].value) * 100f else 0f
                val rateText = "▼ ${String.format("%.1f", rate)}%"

                val rateTextStyle = options.conversionTextStyle ?: TextStyle(
                    color = if (isDark) Color(0xFFFAC858) else Color(0xFFE65100),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                val rateLayout = textMeasurer.measure(rateText, style = rateTextStyle)

                // 转换率放置在两层夹缝处 (yBottom 与下一层的 yTop 中点)
                val yGapMid = (yBottom + topYs[i + 1]) / 2f

                val capW = rateLayout.size.width + 10.dp.toPx()
                val capH = rateLayout.size.height + 4.dp.toPx()

                // 根据对齐排布气泡位置
                val capLeftX = when (options.align) {
                    FunnelAlign.Center -> {
                        // 居中：排在右斜边外侧
                        val xRightEdgeNext = (xTR + xBR) / 2f
                        xRightEdgeNext + 12.dp.toPx()
                    }
                    FunnelAlign.Left -> {
                        // 左边对齐：排在左侧垂直线内或右侧
                        val xRightEdgeNext = (xTR + xBR) / 2f
                        xRightEdgeNext + 12.dp.toPx()
                    }
                    FunnelAlign.Right -> {
                        // 右边对齐：排在左侧倾斜边外侧
                        val xLeftEdgeNext = (xTL + xBL) / 2f
                        xLeftEdgeNext - capW - 12.dp.toPx()
                    }
                }

                // 4.1 画精致胶囊小气泡背景
                drawRoundRect(
                    color = if (isDark) Color(0xFF2C2C2E) else Color(0xFFFFF3E0),
                    topLeft = Offset(capLeftX, yGapMid - capH / 2f),
                    size = Size(capW, capH),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                drawRoundRect(
                    color = if (isDark) Color(0xFF48484A) else Color(0xFFFFCC80),
                    topLeft = Offset(capLeftX, yGapMid - capH / 2f),
                    size = Size(capW, capH),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    style = Stroke(width = 0.6.dp.toPx())
                )

                // 4.2 居中绘制转化率文本
                drawText(
                    textLayoutResult = rateLayout,
                    topLeft = Offset(
                        capLeftX + 5.dp.toPx(),
                        yGapMid - rateLayout.size.height / 2f
                    )
                )
            }
        }
    }

    /**
     * 根据对齐方式换算梯形两边缘的 X 坐标对
     */
    private fun getAlignedX(
        centerX: Float,
        sliceWidthPx: Float,
        gridRect: Rect,
        align: FunnelAlign
    ): Pair<Float, Float> {
        return when (align) {
            FunnelAlign.Center -> {
                val left = centerX - sliceWidthPx / 2f
                val right = centerX + sliceWidthPx / 2f
                left to right
            }
            FunnelAlign.Left -> {
                val left = gridRect.left
                val right = gridRect.left + sliceWidthPx
                left to right
            }
            FunnelAlign.Right -> {
                val left = gridRect.right - sliceWidthPx
                val right = gridRect.right
                left to right
            }
        }
    }
}
