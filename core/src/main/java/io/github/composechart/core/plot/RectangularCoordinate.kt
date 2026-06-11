package io.github.composechart.core.plot

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import io.github.composechart.core.data.CoordinateMapper
import io.github.composechart.core.style.ChartStyle
import io.github.composechart.core.style.GridLineStyle
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

/**
 * 直角坐标系轴网格及标签绘制器，配合 CoordinateMapper 进行具体的 Canvas 渲染。
 */
class RectangularCoordinate(
    private val mapper: CoordinateMapper,
    private val style: ChartStyle,
    private val textMeasurer: TextMeasurer,
    private val xLabels: List<String>,
    private val yTicks: List<Float>,
    private val xTicks: List<Float>? = null,
    private val y2Ticks: List<Float>? = null,
    private val x2Labels: List<String>? = null,
    private val x2Ticks: List<Float>? = null,
    private val categoryValueProvider: ((index: Int) -> Float)? = null
) {
    private val gridRect = mapper.gridRect
    private val y2Min = y2Ticks?.minOrNull() ?: 0f
    private val y2Max = y2Ticks?.maxOrNull() ?: 100f

    /**
     * 绘制图表背景层：交替网格背景色 (Split Area) 及水平/垂直网格线。
     * 该部分应在数据折线绘制前调用，作为底色。
     */
    fun drawBackground(drawScope: DrawScope) = with(drawScope) {
        val yAxisOpt = style.yAxisOptions
        val xAxisOpt = style.xAxisOptions

        // 1. 绘制 Y 轴交替分割区域背景色 (Split Area)
        if (yAxisOpt.showSplitArea && yAxisOpt.splitAreaColors.isNotEmpty() && yTicks.size > 1) {
            for (i in 0 until yTicks.size - 1) {
                val y1 = mapper.toScreenY(yTicks[i])
                val y2 = mapper.toScreenY(yTicks[i + 1])
                // 限制在 grid 裁剪区域内
                val topY = y2.coerceIn(gridRect.top, gridRect.bottom)
                val bottomY = y1.coerceIn(gridRect.top, gridRect.bottom)
                if (bottomY > topY) {
                    val color = yAxisOpt.splitAreaColors[i % yAxisOpt.splitAreaColors.size]
                    drawScope.drawRect(
                        color = color,
                        topLeft = Offset(gridRect.left, topY),
                        size = Size(gridRect.width, bottomY - topY)
                    )
                }
            }
        }

        // 2. 绘制 Y 轴水平分割网格线
        if (yAxisOpt.showGridLines) {
            val pathEffect = if (yAxisOpt.gridLineStyle == GridLineStyle.Dashed) {
                PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
            } else null

            for (tick in yTicks) {
                val y = mapper.toScreenY(tick)
                if (y in (gridRect.top - 0.5f)..(gridRect.bottom + 0.5f)) {
                    drawScope.drawLine(
                        color = yAxisOpt.gridLineColor,
                        start = Offset(gridRect.left, y),
                        end = Offset(gridRect.right, y),
                        strokeWidth = yAxisOpt.gridLineWidth.toPx(),
                        pathEffect = pathEffect
                    )
                }
            }
        }

        // 3. 绘制 X 轴垂直分割网格线
        if (xAxisOpt.showGridLines) {
            val pathEffect = if (xAxisOpt.gridLineStyle == GridLineStyle.Dashed) {
                PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
            } else null

            if (xTicks != null) {
                for (tick in xTicks) {
                    val x = mapper.toScreenX(tick)
                    if (x in (gridRect.left - 0.5f)..(gridRect.right + 0.5f)) {
                        drawScope.drawLine(
                            color = xAxisOpt.gridLineColor,
                            start = Offset(x, gridRect.top),
                            end = Offset(x, gridRect.bottom),
                            strokeWidth = xAxisOpt.gridLineWidth.toPx(),
                            pathEffect = pathEffect
                        )
                    }
                }
            } else {
                for (i in xLabels.indices) {
                    val x = mapper.toScreenX(i.toFloat())
                    if (x in (gridRect.left - 0.5f)..(gridRect.right + 0.5f)) {
                        drawScope.drawLine(
                            color = xAxisOpt.gridLineColor,
                            start = Offset(x, gridRect.top),
                            end = Offset(x, gridRect.bottom),
                            strokeWidth = xAxisOpt.gridLineWidth.toPx(),
                            pathEffect = pathEffect
                        )
                    }
                }
            }
        }
    }

    /**
     * 绘制图表前置层：包含主轴线、数据刻度短线、轴文本及自适应防重叠标签。
     */
    fun drawAxesAndLabels(drawScope: DrawScope) = with(drawScope) {
        val xAxisOpt = style.xAxisOptions
        val yAxisOpt = style.yAxisOptions

        val density = drawScope.density
        val marginTickText = with(drawScope) { 6.dp.toPx() }

        // ================= 计算主轴线零值交叉绘制位置 =================
        val xMainY = if (xAxisOpt.onZero) {
            val zeroY = mapper.toScreenY(0f)
            if (zeroY in gridRect.top..gridRect.bottom) zeroY else gridRect.bottom
        } else {
            gridRect.bottom
        }

        val yMainX = if (yAxisOpt.onZero) {
            val zeroX = mapper.toScreenX(0f)
            if (zeroX in gridRect.left..gridRect.right) zeroX else gridRect.left
        } else {
            gridRect.left
        }

        // ================= Y 轴绘制 =================
        // 1. 绘制 Y 主轴线
        if (yAxisOpt.showLine) {
            drawScope.drawLine(
                color = yAxisOpt.lineColor,
                start = Offset(yMainX, gridRect.top),
                end = Offset(yMainX, gridRect.bottom),
                strokeWidth = yAxisOpt.lineWidth.toPx()
            )
        }

        // 2. 绘制 Y 轴刻度短线
        if (yAxisOpt.showTicks) {
            for (tick in yTicks) {
                val y = mapper.toScreenY(tick)
                if (y in (gridRect.top - 0.5f)..(gridRect.bottom + 0.5f)) {
                    drawScope.drawLine(
                        color = yAxisOpt.tickColor,
                        start = Offset(yMainX - yAxisOpt.tickLength.toPx(), y),
                        end = Offset(yMainX, y),
                        strokeWidth = yAxisOpt.lineWidth.toPx()
                    )
                }
            }
        }

        // 3. 绘制 Y 轴标签文本
        if (yAxisOpt.showLabels) {
            val zeroX = if (yAxisOpt.onZero) mapper.toScreenX(0f) else gridRect.left
            val isZeroInGrid = zeroX in gridRect.left..gridRect.right

            for (tick in yTicks) {
                val rawText = yAxisOpt.labelFormatter?.invoke(tick) ?: String.format("%.1f", tick).removeSuffix(".0")
                val textLayout = textMeasurer.measure(
                    text = rawText,
                    style = yAxisOpt.labelTextStyle
                )
                val y = mapper.toScreenY(tick)
                val textWidth = textLayout.size.width
                val textHeight = textLayout.size.height

                val drawX = if (yAxisOpt.onZero && yAxisOpt.labelOnZero && categoryValueProvider != null && isZeroInGrid) {
                    val valSign = categoryValueProvider.invoke(tick.toInt())
                    if (valSign >= 0f) {
                        zeroX - textWidth - marginTickText
                    } else {
                        zeroX + marginTickText
                    }
                } else {
                    yMainX - textWidth - marginTickText
                }
                val drawY = y - textHeight / 2f

                if (drawY + textHeight >= gridRect.top && drawY <= gridRect.bottom) {
                    drawScope.drawText(
                        textLayoutResult = textLayout,
                        topLeft = Offset(drawX, drawY)
                    )
                }
            }
        }

        // 4. 绘制 Y 轴单位名称
        if (yAxisOpt.show && yAxisOpt.name.isNotEmpty()) {
            val nameLayout = textMeasurer.measure(
                text = yAxisOpt.name,
                style = yAxisOpt.nameTextStyle
            )
            val nameHeight = nameLayout.size.height
            drawScope.drawText(
                textLayoutResult = nameLayout,
                topLeft = Offset(
                    yMainX - nameLayout.size.width / 2f,
                    gridRect.top - nameHeight - with(drawScope) { 8.dp.toPx() }
                )
            )
        }

        // ================= Y2 轴绘制 =================
        val y2AxisOpt = style.y2AxisOptions
        if (y2Ticks != null && y2AxisOpt.show) {
            // 1. 绘制 Y2 主轴线
            if (y2AxisOpt.showLine) {
                drawScope.drawLine(
                    color = y2AxisOpt.lineColor,
                    start = Offset(gridRect.right, gridRect.top),
                    end = Offset(gridRect.right, gridRect.bottom),
                    strokeWidth = y2AxisOpt.lineWidth.toPx()
                )
            }

            // 2. 绘制 Y2 轴刻度短线
            if (y2AxisOpt.showTicks) {
                for (tick in y2Ticks) {
                    val y = mapper.toScreenY2(tick, y2Min, y2Max)
                    if (y in (gridRect.top - 0.5f)..(gridRect.bottom + 0.5f)) {
                        drawScope.drawLine(
                            color = y2AxisOpt.tickColor,
                            start = Offset(gridRect.right, y),
                            end = Offset(gridRect.right + y2AxisOpt.tickLength.toPx(), y),
                            strokeWidth = y2AxisOpt.lineWidth.toPx()
                        )
                    }
                }
            }

            // 3. 绘制 Y2 轴标签文本
            if (y2AxisOpt.showLabels) {
                for (tick in y2Ticks) {
                    val rawText = y2AxisOpt.labelFormatter?.invoke(tick) ?: String.format("%.1f", tick).removeSuffix(".0")
                    val textLayout = textMeasurer.measure(
                        text = rawText,
                        style = y2AxisOpt.labelTextStyle
                    )
                    val y = mapper.toScreenY2(tick, y2Min, y2Max)
                    val textWidth = textLayout.size.width
                    val textHeight = textLayout.size.height

                    val drawX = gridRect.right + marginTickText
                    val drawY = y - textHeight / 2f

                    if (drawY + textHeight >= gridRect.top && drawY <= gridRect.bottom) {
                        drawScope.drawText(
                            textLayoutResult = textLayout,
                            topLeft = Offset(drawX, drawY)
                        )
                    }
                }
            }

            // 4. 绘制 Y2 轴单位名称
            if (y2AxisOpt.name.isNotEmpty()) {
                val nameLayout = textMeasurer.measure(
                    text = y2AxisOpt.name,
                    style = y2AxisOpt.nameTextStyle
                )
                val nameHeight = nameLayout.size.height
                drawScope.drawText(
                    textLayoutResult = nameLayout,
                    topLeft = Offset(
                        gridRect.right - nameLayout.size.width / 2f,
                        gridRect.top - nameHeight - with(drawScope) { 8.dp.toPx() }
                    )
                )
            }
        }

        // ================= X 轴绘制 =================
        // 1. 绘制 X 主轴线
        if (xAxisOpt.showLine) {
            drawScope.drawLine(
                color = xAxisOpt.lineColor,
                start = Offset(gridRect.left, xMainY),
                end = Offset(gridRect.right, xMainY),
                strokeWidth = xAxisOpt.lineWidth.toPx()
            )
        }

        // 2. 绘制 X 轴刻度短线
        if (xAxisOpt.showTicks) {
            if (xTicks != null) {
                for (tick in xTicks) {
                    val x = mapper.toScreenX(tick)
                    if (x in (gridRect.left - 0.5f)..(gridRect.right + 0.5f)) {
                        drawScope.drawLine(
                            color = xAxisOpt.tickColor,
                            start = Offset(x, xMainY),
                            end = Offset(x, xMainY + xAxisOpt.tickLength.toPx()),
                            strokeWidth = xAxisOpt.lineWidth.toPx()
                        )
                    }
                }
            } else {
                for (i in xLabels.indices) {
                    val x = mapper.toScreenX(i.toFloat())
                    if (x in (gridRect.left - 0.5f)..(gridRect.right + 0.5f)) {
                        drawScope.drawLine(
                            color = xAxisOpt.tickColor,
                            start = Offset(x, xMainY),
                            end = Offset(x, xMainY + xAxisOpt.tickLength.toPx()),
                            strokeWidth = xAxisOpt.lineWidth.toPx()
                        )
                    }
                }
            }
        }

        // 3. 绘制 X 轴标签（内置自适应防重叠及旋转算法）
        if (xAxisOpt.showLabels) {
            val labelsToDraw = if (xTicks != null) {
                xTicks.map { xAxisOpt.labelFormatter?.invoke(it) ?: String.format("%.1f", it).removeSuffix(".0") }
            } else {
                xLabels
            }

            if (labelsToDraw.isNotEmpty()) {
                // 3.1 预先测量所有类目标签的物理宽度以评估重叠比率
                val measuredLayouts = labelsToDraw.map {
                    textMeasurer.measure(text = it, style = xAxisOpt.labelTextStyle)
                }
                val maxLabelWidth = measuredLayouts.maxOf { it.size.width }

                // 计算当前屏幕上网格的可用物理间距
                val leftVal = mapper.toDataX(gridRect.left)
                val rightVal = mapper.toDataX(gridRect.right)
                
                val visibleTicksCount = if (xTicks != null) {
                    xTicks.count { it in (leftVal - 0.01f)..(rightVal + 0.01f) }.coerceAtLeast(1).toFloat()
                } else {
                    val leftIndex = leftVal.coerceIn(0f, xLabels.lastIndex.toFloat())
                    val rightIndex = rightVal.coerceIn(0f, xLabels.lastIndex.toFloat())
                    (rightIndex - leftIndex).coerceAtLeast(1f)
                }
                val avgItemSpacing = gridRect.width / visibleTicksCount

                // 判定拥挤策略：留有 8.dp 左右的安全缓冲区
                val bufferPx = with(drawScope) { 8.dp.toPx() }
                val isCrowded = maxLabelWidth > (avgItemSpacing - bufferPx)

                val zeroY = if (xAxisOpt.onZero) mapper.toScreenY(0f) else gridRect.bottom
                val isZeroInGrid = zeroY in gridRect.top..gridRect.bottom

                // 计算每个类目的标签 Y 物理位置
                fun getLabelY(categoryIndex: Int, textHeight: Float, isForRotate: Boolean): Float {
                    return if (xAxisOpt.onZero && xAxisOpt.labelOnZero && categoryValueProvider != null && isZeroInGrid) {
                        val valSign = categoryValueProvider.invoke(categoryIndex)
                        if (valSign >= 0f) {
                            zeroY + marginTickText
                        } else {
                            if (isForRotate) {
                                zeroY - marginTickText
                            } else {
                                zeroY - textHeight - marginTickText
                            }
                        }
                    } else {
                        xMainY + marginTickText
                    }
                }

                // 判断是否采用旋转绘制
                val hasUserRotation = xAxisOpt.labelRotate != 0f
                if (!hasUserRotation && !isCrowded) {
                    // 策略 1：无需旋转且空间充裕，直接常规水平对齐绘制
                    for (i in labelsToDraw.indices) {
                        val tickVal = if (xTicks != null) xTicks[i] else i.toFloat()
                        val x = mapper.toScreenX(tickVal)
                        if (x in (gridRect.left - 0.5f)..(gridRect.right + 0.5f)) {
                            val layout = measuredLayouts[i]
                            val textWidth = layout.size.width
                            val textHeight = layout.size.height.toFloat()
                            val drawY = getLabelY(i, textHeight, isForRotate = false)
                            drawScope.drawText(
                                textLayoutResult = layout,
                                topLeft = Offset(x - textWidth / 2f, drawY)
                            )
                        }
                    }
                } else {
                    // 策略 2：空间拥挤或用户指定了旋转，启用旋转渲染
                    val rotateDegree = if (hasUserRotation) xAxisOpt.labelRotate else -45f
                    
                    // 如果是采样旋转模式下，计算采样步长；若用户自己指定了旋转，一般认为每个都绘制出来
                    val sampleInterval = if (hasUserRotation) {
                        1
                    } else {
                        val cos45 = cos(Math.toRadians(45.0)).toFloat()
                        val projectionWidth = maxLabelWidth * cos45
                        val minSpacingNeeded = projectionWidth + bufferPx
                        ceil(minSpacingNeeded / avgItemSpacing).toInt().coerceAtLeast(1)
                    }

                    for (i in labelsToDraw.indices) {
                        if (i % sampleInterval == 0) {
                            val tickVal = if (xTicks != null) xTicks[i] else i.toFloat()
                            val x = mapper.toScreenX(tickVal)
                            if (x in (gridRect.left - 0.5f)..(gridRect.right + 0.5f)) {
                                val layout = measuredLayouts[i]
                                val textWidth = layout.size.width
                                val textHeight = layout.size.height.toFloat()

                                val drawY = getLabelY(i, textHeight, isForRotate = true)

                                val valSign = if (xAxisOpt.onZero && xAxisOpt.labelOnZero && categoryValueProvider != null && isZeroInGrid) {
                                    categoryValueProvider.invoke(i)
                                } else 1f
                                
                                val drawX = if (valSign >= 0f) {
                                    if (rotateDegree < 0) x - textWidth else x
                                } else {
                                    if (rotateDegree < 0) x else x - textWidth
                                }

                                drawScope.withTransform({
                                    rotate(degrees = rotateDegree, pivot = Offset(x, drawY))
                                }) {
                                    drawText(
                                        textLayoutResult = layout,
                                        topLeft = Offset(drawX, drawY)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ================= X2 轴绘制 (顶部 X 轴) =================
        val x2AxisOpt = style.x2AxisOptions
        if (x2AxisOpt.show && (x2Labels != null || x2Ticks != null)) {
            // 1. 绘制 X2 主轴线
            if (x2AxisOpt.showLine) {
                drawScope.drawLine(
                    color = x2AxisOpt.lineColor,
                    start = Offset(gridRect.left, gridRect.top),
                    end = Offset(gridRect.right, gridRect.top),
                    strokeWidth = x2AxisOpt.lineWidth.toPx()
                )
            }

            // 2. 绘制 X2 轴刻度短线 (向上)
            if (x2AxisOpt.showTicks) {
                val ticksToUse = x2Ticks ?: x2Labels?.indices?.map { it.toFloat() }
                if (ticksToUse != null) {
                    for (tick in ticksToUse) {
                        val x = mapper.toScreenX(tick)
                        if (x in (gridRect.left - 0.5f)..(gridRect.right + 0.5f)) {
                            drawScope.drawLine(
                                color = x2AxisOpt.tickColor,
                                start = Offset(x, gridRect.top),
                                end = Offset(x, gridRect.top - x2AxisOpt.tickLength.toPx()),
                                strokeWidth = x2AxisOpt.lineWidth.toPx()
                            )
                        }
                    }
                }
            }

            // 3. 绘制 X2 轴标签文本 (在顶部轴线之上)
            if (x2AxisOpt.showLabels) {
                val labelsToDraw = if (x2Ticks != null) {
                    x2Ticks.map { x2AxisOpt.labelFormatter?.invoke(it) ?: String.format("%.1f", it).removeSuffix(".0") }
                } else {
                    x2Labels ?: emptyList()
                }

                if (labelsToDraw.isNotEmpty()) {
                    val measuredLayouts = labelsToDraw.map {
                        textMeasurer.measure(text = it, style = x2AxisOpt.labelTextStyle)
                    }
                    
                    for (i in labelsToDraw.indices) {
                        val tickVal = if (x2Ticks != null) x2Ticks[i] else i.toFloat()
                        val x = mapper.toScreenX(tickVal)
                        if (x in (gridRect.left - 0.5f)..(gridRect.right + 0.5f)) {
                            val layout = measuredLayouts[i]
                            val textWidth = layout.size.width
                            val textHeight = layout.size.height
                            // 绘制在轴线上方：向上偏移 tickLength + margin + textHeight
                            val drawY = gridRect.top - x2AxisOpt.tickLength.toPx() - marginTickText - textHeight
                            drawScope.drawText(
                                textLayoutResult = layout,
                                topLeft = Offset(x - textWidth / 2f, drawY)
                            )
                        }
                    }
                }
            }
        }
    }
}
