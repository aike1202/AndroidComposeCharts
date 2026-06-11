package io.github.composechart.charts.bar

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import io.github.composechart.core.data.CoordinateMapper
import io.github.composechart.core.style.ChartStyle
import kotlin.math.abs
import kotlin.math.min

/**
 * 柱状图/条形图 Canvas 具体绘制渲染器
 */
class BarChartRenderer(
    private val mapper: CoordinateMapper,
    private val allSeries: List<BarSeries>,
    private val style: ChartStyle,
    private val horizontal: Boolean,
    private val animationProgress: Float = 1.0f,
    private val y2Min: Float = 0f,
    private val y2Max: Float = 100f
) {
    private val grid = mapper.gridRect

    /**
     * 执行柱体层绘制。
     */
    fun draw(drawScope: DrawScope) = with(drawScope) {
        if (allSeries.isEmpty()) return@with

        // 1. 获取数据的最大点数上限
        val maxValuesCount = allSeries.maxOf { it.values.size }
        if (maxValuesCount == 0) return@with

        // 2. 依据当前可见视口对类目进行物理裁剪，防止大数据卡顿
        // 若为水平条形图，Y 轴为类目，所以使用 toDataY 裁剪；若为垂直柱状图，X 轴为类目，使用 toDataX
        val leftIndex: Int
        val rightIndex: Int
        if (!horizontal) {
            leftIndex = (mapper.toDataX(grid.left) - 1f).toInt().coerceAtLeast(0)
            rightIndex = (mapper.toDataX(grid.right) + 1f).toInt().coerceAtMost(maxValuesCount - 1)
        } else {
            // Y轴朝下方向增大，所以 top 对应数值的最大值，bottom 对应最小值。
            // mapper.toDataY(grid.bottom) 为 Y 轴最小值，mapper.toDataY(grid.top) 为 Y 轴最大值。
            leftIndex = (mapper.toDataY(grid.bottom) - 1f).toInt().coerceAtLeast(0)
            rightIndex = (mapper.toDataY(grid.top) + 1f).toInt().coerceAtMost(maxValuesCount - 1)
        }

        // 3. 计算绘制组划分（用于对比柱并列与数值堆叠的分组定位）
        val drawGroups = mutableListOf<String>()
        val seriesToGroupIndex = mutableMapOf<Int, Int>()

        allSeries.forEachIndexed { sIndex, series ->
            val groupKey = series.stack ?: series.name
            var gIndex = drawGroups.indexOf(groupKey)
            if (gIndex == -1) {
                drawGroups.add(groupKey)
                gIndex = drawGroups.lastIndex
            }
            seriesToGroupIndex[sIndex] = gIndex
        }

        val totalGroupsCount = drawGroups.size

        // 4. 计算柱体物理尺寸参数
        val firstSeries = allSeries.first()
        val barWidthRatio = firstSeries.barWidthRatio
        val barGapRatio = firstSeries.barGapRatio

        // 可用网格物理宽度
        val categoryLength = if (!horizontal) grid.width else grid.height
        val totalLabelsCount = maxValuesCount.toFloat()
        val itemSpacing = if (totalLabelsCount > 1) categoryLength / (totalLabelsCount - 1) else categoryLength

        val totalBarsWidth = itemSpacing * barWidthRatio
        // 单个绘制组的柱子物理大小 (宽度或高度)
        val barSize = if (totalGroupsCount > 0) {
            totalBarsWidth / (totalGroupsCount + (totalGroupsCount - 1) * barGapRatio)
        } else 0f
        val barGap = barSize * barGapRatio

        clipRect(
            left = grid.left,
            top = grid.top,
            right = grid.right,
            bottom = grid.bottom
        ) {
            // 5. 循环绘制每个类目的数据元素
            for (categoryIndex in leftIndex..rightIndex) {
            // 类目中心物理位置
            val centerPos = if (!horizontal) {
                mapper.toScreenX(categoryIndex.toFloat())
            } else {
                mapper.toScreenY(categoryIndex.toFloat())
            }

            // 在该类目下，初始化用于数值堆叠的累加器
            val posAccumulators = mutableMapOf<String, Float>()
            val negAccumulators = mutableMapOf<String, Float>()

            // 5.1 绘制柱子的背景阴影槽 (showBackground)
            if (firstSeries.showBackground) {
                for (groupIndex in 0 until totalGroupsCount) {
                    val groupOffset = -totalBarsWidth / 2f + groupIndex * (barSize + barGap)
                    val barStart = centerPos + groupOffset
                    val barEnd = barStart + barSize

                    if (!horizontal) {
                        drawRect(
                            color = firstSeries.backgroundColor,
                            topLeft = Offset(barStart, grid.top),
                            size = Size(barSize, grid.height)
                        )
                    } else {
                        drawRect(
                            color = firstSeries.backgroundColor,
                            topLeft = Offset(grid.left, barStart),
                            size = Size(grid.width, barSize)
                        )
                    }
                }
            }

            // 5.2 绘制实际数据柱体
            allSeries.forEachIndexed { sIndex, series ->
                val groupIndex = seriesToGroupIndex[sIndex] ?: return@forEachIndexed
                val barValObj = series.values.getOrNull(categoryIndex) ?: return@forEachIndexed
                val rawValue = barValObj.value

                // 柱体并列起始偏移
                val groupOffset = -totalBarsWidth / 2f + groupIndex * (barSize + barGap)
                val barStart = centerPos + groupOffset
                val barEnd = barStart + barSize

                // 堆叠及悬空区间计算
                val stackKey = series.stack
                val startVal: Float
                val endVal: Float

                if (barValObj.baseValue != null) {
                    val base = barValObj.baseValue
                    startVal = base
                    endVal = base + (rawValue - base) * animationProgress
                } else if (stackKey != null) {
                    if (rawValue >= 0f) {
                        val currentAccum = posAccumulators[stackKey] ?: 0f
                        startVal = currentAccum
                        endVal = currentAccum + rawValue * animationProgress
                        posAccumulators[stackKey] = endVal
                    } else {
                        val currentAccum = negAccumulators[stackKey] ?: 0f
                        startVal = currentAccum
                        endVal = currentAccum + rawValue * animationProgress
                        negAccumulators[stackKey] = endVal
                    }
                } else {
                    startVal = 0f
                    endVal = rawValue * animationProgress
                }

                // 计算 Canvas 上的物理坐标
                if (!horizontal) {
                    val yStartPx = if (series.yAxisIndex == 1) {
                        mapper.toScreenY2(startVal, y2Min, y2Max)
                    } else {
                        mapper.toScreenY(startVal)
                    }
                    val yEndPx = if (series.yAxisIndex == 1) {
                        mapper.toScreenY2(endVal, y2Min, y2Max)
                    } else {
                        mapper.toScreenY(endVal)
                    }

                    val topPx = min(yStartPx, yEndPx)
                    val heightPx = abs(yStartPx - yEndPx)

                    if (heightPx > 0f) {
                        drawBarRect(
                            left = barStart,
                            top = topPx,
                            right = barEnd,
                            bottom = topPx + heightPx,
                            color = series.color,
                            brush = series.gradientBrush,
                            cornerRadius = series.cornerRadius,
                            horizontal = false,
                            isPositive = rawValue >= 0f,
                            series = series
                        )
                    }
                } else {
                    val xStartPx = mapper.toScreenX(startVal)
                    val xEndPx = mapper.toScreenX(endVal)

                    val leftPx = min(xStartPx, xEndPx)
                    val widthPx = abs(xStartPx - xEndPx)

                    if (widthPx > 0f) {
                        drawBarRect(
                            left = leftPx,
                            top = barStart,
                            right = leftPx + widthPx,
                            bottom = barEnd,
                            color = series.color,
                            brush = series.gradientBrush,
                            cornerRadius = series.cornerRadius,
                            horizontal = true,
                            isPositive = rawValue >= 0f,
                            series = series
                        )
                    }
                }
            }
        }
    }
}

    /**
     * 手绘顶部或右侧单边圆角矩形 Path。
     */
    private fun DrawScope.drawBarRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        color: Color,
        brush: Brush?,
        cornerRadius: CornerRadius,
        horizontal: Boolean,
        isPositive: Boolean,
        series: BarSeries
    ) {
        val path = Path()
        val rX = cornerRadius.x.coerceAtMost(abs(right - left) / 2f)
        val rY = cornerRadius.y.coerceAtMost(abs(bottom - top) / 2f)

        if (rX <= 0f || rY <= 0f) {
            // 无圆角或极小，直接绘制常规矩形
            path.moveTo(left, top)
            path.lineTo(right, top)
            path.lineTo(right, bottom)
            path.lineTo(left, bottom)
            path.close()
        } else {
            if (!horizontal) {
                if (isPositive) {
                    // 正数柱子：上方两个角是圆角，下方是直角
                    path.moveTo(left, bottom)
                    path.lineTo(left, top + rY)
                    path.quadraticTo(left, top, left + rX, top)
                    path.lineTo(right - rX, top)
                    path.quadraticTo(right, top, right, top + rY)
                    path.lineTo(right, bottom)
                    path.close()
                } else {
                    // 负数柱子：上方是直角，下方两个角是圆角
                    path.moveTo(left, top)
                    path.lineTo(right, top)
                    path.lineTo(right, bottom - rY)
                    path.quadraticTo(right, bottom, right - rX, bottom)
                    path.lineTo(left + rX, bottom)
                    path.quadraticTo(left, bottom, left, bottom - rY)
                    path.close()
                }
            } else {
                if (isPositive) {
                    // 正数条形图：右边两个角是圆角，左边是直角
                    path.moveTo(left, top)
                    path.lineTo(right - rX, top)
                    path.quadraticTo(right, top, right, top + rY)
                    path.lineTo(right, bottom - rY)
                    path.quadraticTo(right, bottom, right - rX, bottom)
                    path.lineTo(left, bottom)
                    path.close()
                } else {
                    // 负数条形图：左边两个角是圆角，右边是直角
                    path.moveTo(right, top)
                    path.lineTo(right, bottom)
                    path.lineTo(left + rX, bottom)
                    path.quadraticTo(left, bottom, left, bottom - rY)
                    path.lineTo(left, top + rY)
                    path.quadraticTo(left, top, left + rX, top)
                    path.close()
                }
            }
        }

        // 绘制阴影外壳（双层渲染第一步）
        if (series.shadowColor != null && series.shadowBlur > 0f) {
            drawIntoCanvas { canvas ->
                val shadowPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    this.color = android.graphics.Color.TRANSPARENT
                    setShadowLayer(
                        series.shadowBlur,
                        series.shadowOffset.x,
                        series.shadowOffset.y,
                        series.shadowColor.toArgb()
                    )
                }
                canvas.nativeCanvas.drawPath(path.asAndroidPath(), shadowPaint)
            }
        }

        // 绘制实体柱子（双层渲染第二步）
        if (brush != null) {
            drawPath(path = path, brush = brush)
        } else {
            drawPath(path = path, color = color)
        }
    }
}
