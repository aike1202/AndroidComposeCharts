package io.github.composechart.charts.boxplot

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.dp
import io.github.composechart.core.data.CoordinateMapper
import io.github.composechart.core.style.ChartStyle
import kotlin.math.abs

/**
 * 盒须图直角坐标系绘制器，处理多系列并列位移、动画拉伸插值、箱线和异常值标记的 Canvas 绘制。
 */
class BoxplotChartRenderer(
    private val mapper: CoordinateMapper,
    private val series: BoxplotSeries,
    private val style: ChartStyle,
    private val textMeasurer: TextMeasurer,
    private val boxGrowth: Float = 1.0f,         // 箱体生长系数 (0f->1f)
    private val whiskerGrowth: Float = 1.0f,     // 触须生长系数 (0f->1f)
    private val hoveredCategoryIndex: Int? = null, // 当前 Hover 锁定的类目索引
    private val seriesIndex: Int = 0,             // 当前并列系列的索引
    private val totalSeriesCount: Int = 1,        // 总并列系列数
    private val totalLabelsCount: Int = 1
) {
    private val gridRect = mapper.gridRect

    /**
     * 绘制该系列的全部盒须图和异常值散点
     */
    fun draw(drawScope: DrawScope) = with(drawScope) {
        val density = drawScope.density
        val colorPalette = style.colorPalette

        val n = series.points.size
        if (n == 0) return@with

        // 1. 计算类目间距，并据此得出多系列并列排布的水平偏置量
        val slotWidth = if (totalLabelsCount > 1) {
            mapper.toScreenX(1f) - mapper.toScreenX(0f)
        } else {
            gridRect.width * 0.6f
        }

        // 多系列并列总跨度设为 slotWidth 的 65%
        val groupWidth = slotWidth * 0.65f
        val singleBoxSpace = groupWidth / totalSeriesCount
        val boxWidth = singleBoxSpace * series.boxWidthRatio
        val whiskerWidth = singleBoxSpace * series.whiskerWidthRatio

        // 每个盒子相对类目中线 X 的水平位移
        val groupOffset = -groupWidth / 2f + (seriesIndex + 0.5f) * singleBoxSpace

        clipRect(
            left = gridRect.left,
            top = gridRect.top,
            right = gridRect.right,
            bottom = gridRect.bottom
        ) {
            for (i in 0 until n) {
                val pt = series.points[i]
            
            // X 中轴物理像素
            val centerX = mapper.toScreenX(i.toFloat())
            val drawX = centerX + groupOffset

            // Q1, Median, Q3, Min, Max 对应的屏幕 Y 像素高度
            val yMin = mapper.toScreenY(pt.min)
            val yQ1 = mapper.toScreenY(pt.q1)
            val yMedian = mapper.toScreenY(pt.median)
            val yQ3 = mapper.toScreenY(pt.q3)
            val yMax = mapper.toScreenY(pt.max)

            // 2. 两阶段生长动画插值计算
            // 2.1 第一阶段：箱体高度由中位数线平滑往上下两侧展开
            val yQ1Anim = yMedian + (yQ1 - yMedian) * boxGrowth
            val yQ3Anim = yMedian + (yQ3 - yMedian) * boxGrowth

            // 2.2 第二阶段：触须在箱体展开完毕后，从箱体上下边缘向两端延展
            val yMaxAnim = yQ3Anim + (yMax - yQ3Anim) * whiskerGrowth
            val yMinAnim = yQ1Anim + (yMin - yQ1Anim) * whiskerGrowth

            // 箱顶与箱底 Y 像素 (Q3 数值大，故对应的像素 Y 值偏小)
            val boxTopY = minOf(yQ1Anim, yQ3Anim)
            val boxBottomY = maxOf(yQ1Anim, yQ3Anim)
            val boxHeight = abs(yQ1Anim - yQ3Anim)

            val isHovered = hoveredCategoryIndex == i
            val strokeWidthPx = if (isHovered) 2.5.dp.toPx() else 1.2.dp.toPx()
            val fillColor = series.fillColor ?: series.color.copy(alpha = 0.2f)

            // 3. 开始绘制箱线主体
            // 3.1 绘制第一和第三四分位数之间的填充箱体 (Box Body)
            drawRect(
                color = fillColor,
                topLeft = Offset(drawX - boxWidth / 2f, boxTopY),
                size = Size(boxWidth, boxHeight.coerceAtLeast(1f)),
                style = Fill
            )
            drawRect(
                color = series.color,
                topLeft = Offset(drawX - boxWidth / 2f, boxTopY),
                size = Size(boxWidth, boxHeight.coerceAtLeast(1f)),
                style = Stroke(width = strokeWidthPx)
            )

            // 3.2 绘制中位数横向分割线 (Median Line)
            drawLine(
                color = series.color,
                start = Offset(drawX - boxWidth / 2f, yMedian),
                end = Offset(drawX + boxWidth / 2f, yMedian),
                strokeWidth = if (isHovered) 3.dp.toPx() else 2.dp.toPx()
            )

            // 3.3 绘制上下垂直触须线 (Whiskers)
            drawLine(
                color = series.color,
                start = Offset(drawX, boxTopY),
                end = Offset(drawX, yMaxAnim),
                strokeWidth = strokeWidthPx
            )
            drawLine(
                color = series.color,
                start = Offset(drawX, boxBottomY),
                end = Offset(drawX, yMinAnim),
                strokeWidth = strokeWidthPx
            )

            // 3.4 绘制最大/最小值水平端盖 (Caps)
            drawLine(
                color = series.color,
                start = Offset(drawX - whiskerWidth / 2f, yMaxAnim),
                end = Offset(drawX + whiskerWidth / 2f, yMaxAnim),
                strokeWidth = strokeWidthPx
            )
            drawLine(
                color = series.color,
                start = Offset(drawX - whiskerWidth / 2f, yMinAnim),
                end = Offset(drawX + whiskerWidth / 2f, yMinAnim),
                strokeWidth = strokeWidthPx
            )

            // 3.5 绘制本系列在本类目下的异常值 (Outliers)
            val indexOutliers = series.outliers.filter { it.xIndex == i }
            for (outlier in indexOutliers) {
                val yOutlier = mapper.toScreenY(outlier.value)
                // 异常值以 3.dp 左右空心小圆圈表示
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = Offset(drawX, yOutlier),
                    style = Fill
                )
                drawCircle(
                    color = series.color,
                    radius = 3.dp.toPx(),
                    center = Offset(drawX, yOutlier),
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }
        }
    }
}
}
