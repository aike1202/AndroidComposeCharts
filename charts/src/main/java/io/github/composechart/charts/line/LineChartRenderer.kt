package io.github.composechart.charts.line

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.drawscope.clipRect
import io.github.composechart.core.data.CoordinateMapper
import io.github.composechart.core.style.ChartStyle

/**
 * 折线图、面积图具体渲染器，包含贝塞尔平滑、分段空值逻辑及辅助线/极值点绘制。
 */
class LineChartRenderer(
    private val mapper: CoordinateMapper,
    private val series: LineSeries,
    private val style: ChartStyle,
    private val textMeasurer: TextMeasurer,
    private val baselinePoints: List<Float>? = null,
    private val y2Min: Float = 0f,
    private val y2Max: Float = 100f
) {
    private val grid = mapper.gridRect

    private fun toScreenY(yVal: Float): Float {
        return if (series.yAxisIndex == 1) {
            mapper.toScreenY2(yVal, y2Min, y2Max)
        } else {
            mapper.toScreenY(yVal)
        }
    }

    private fun getDisplayY(pt: LinePoint): Float {
        val yVal = pt.y ?: 0f
        val base = baselinePoints?.getOrNull(pt.x.toInt()) ?: 0f
        return yVal + base
    }

    /**
     * 在 Canvas 上执行主折线、面积渐变和数据拐点的绘制。
     */
    fun draw(drawScope: DrawScope) = with(drawScope) {
        val points = series.points
        if (points.isEmpty()) return@with

        // 1. 根据可见物理宽度范围对数据进行二分边界裁剪，防止百万大数据绘制卡顿
        val (leftIndex, rightIndex) = points.getVisibleRange(mapper.toDataX(grid.left) - 1f, mapper.toDataX(grid.right) + 1f)
        val visiblePoints = if (leftIndex <= rightIndex) points.subList(leftIndex, rightIndex + 1) else emptyList()
        if (visiblePoints.isEmpty()) return@with

        // 2. 根据 connectNulls 对可见数据做空值段划分
        val segments = mutableListOf<List<LinePoint>>()
        if (series.connectNulls) {
            val validPoints = visiblePoints.filter { it.y != null }
            if (validPoints.isNotEmpty()) {
                segments.add(validPoints)
            }
        } else {
            var currentSegment = mutableListOf<LinePoint>()
            for (pt in visiblePoints) {
                if (pt.y != null) {
                    currentSegment.add(pt)
                } else {
                    if (currentSegment.isNotEmpty()) {
                        segments.add(currentSegment)
                        currentSegment = mutableListOf()
                    }
                }
            }
            if (currentSegment.isNotEmpty()) {
                segments.add(currentSegment)
            }
        }

        // 3. 对每一个有效数据分段，分别构建折线与渐变面积填充的 Path 并绘制
        val clipPadding = 8.dp.toPx()

        // 3.1 优先绘制面积渐变层 (严格裁剪在网格轴线内，不溢出至左右 Y 轴范围)
        clipRect(
            left = grid.left,
            top = grid.top,
            right = grid.right,
            bottom = grid.bottom
        ) {
            if (series.drawArea) {
                val finalAreaPath = generateAreaPath()
                val brush = series.areaBrush ?: Brush.verticalGradient(
                    colors = listOf(series.color.copy(alpha = 0.35f), series.color.copy(alpha = 0.0f)),
                    startY = grid.top,
                    endY = grid.bottom
                )
                drawPath(path = finalAreaPath, brush = brush)
            }
        }

        // 3.2 绘制各分段的折线 (同样严格裁剪在左右轴线内)
        clipRect(
            left = grid.left,
            top = grid.top,
            right = grid.right,
            bottom = grid.bottom
        ) {
            for (segment in segments) {
                if (segment.isEmpty()) continue

                val linePath = Path()
                val startX = mapper.toScreenX(segment[0].x)
                val startY = toScreenY(getDisplayY(segment[0]))
                linePath.moveTo(startX, startY)

                if (segment.size >= 2) {
                    if (series.stepType != StepType.None) {
                        for (i in 0 until segment.size - 1) {
                            val p1 = segment[i]
                            val p2 = segment[i + 1]
                            val x1 = mapper.toScreenX(p1.x)
                            val y1 = toScreenY(getDisplayY(p1))
                            val x2 = mapper.toScreenX(p2.x)
                            val y2 = toScreenY(getDisplayY(p2))

                            when (series.stepType) {
                                StepType.Start -> {
                                    linePath.lineTo(x1, y2)
                                    linePath.lineTo(x2, y2)
                                }
                                StepType.End -> {
                                    linePath.lineTo(x2, y1)
                                    linePath.lineTo(x2, y2)
                                }
                                StepType.Middle -> {
                                    val xmid = (x1 + x2) / 2f
                                    linePath.lineTo(xmid, y1)
                                    linePath.lineTo(xmid, y2)
                                    linePath.lineTo(x2, y2)
                                }
                                else -> linePath.lineTo(x2, y2)
                            }
                        }
                    } else if (series.isSmooth) {
                        val smoothness = 0.16f
                        for (i in 0 until segment.size - 1) {
                            val p0 = segment[if (i - 1 >= 0) i - 1 else i]
                            val p1 = segment[i]
                            val p2 = segment[i + 1]
                            val p3 = segment[if (i + 2 < segment.size) i + 2 else i + 1]

                            val op0 = Offset(mapper.toScreenX(p0.x), toScreenY(getDisplayY(p0)))
                            val op1 = Offset(mapper.toScreenX(p1.x), toScreenY(getDisplayY(p1)))
                            val op2 = Offset(mapper.toScreenX(p2.x), toScreenY(getDisplayY(p2)))
                            val op3 = Offset(mapper.toScreenX(p3.x), toScreenY(getDisplayY(p3)))

                            val c1 = op1 + (op2 - op0) * smoothness
                            val c2 = op2 - (op3 - op1) * smoothness

                            linePath.cubicTo(c1.x, c1.y, c2.x, c2.y, op2.x, op2.y)
                        }
                    } else {
                        for (i in 1 until segment.size) {
                            val p = segment[i]
                            linePath.lineTo(mapper.toScreenX(p.x), toScreenY(getDisplayY(p)))
                        }
                    }
                }

                // 绘制主折线
                val pathEffect = when (series.lineStyle) {
                    LineStyleType.Dashed -> PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                    LineStyleType.Dotted -> PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                    LineStyleType.Solid -> null
                }
                drawPath(
                    path = linePath,
                    color = series.color,
                    style = Stroke(
                        width = series.lineWidth.toPx(),
                        pathEffect = pathEffect
                    )
                )
            }
        }

        // 3.3 独立绘制数据拐点 Symbol (向外扩展 8.dp 裁剪区以确保边缘数据圆点及虚线不残缺)
        clipRect(
            left = grid.left - clipPadding,
            top = grid.top - clipPadding,
            right = grid.right + clipPadding,
            bottom = grid.bottom + clipPadding
        ) {
            for (segment in segments) {
                if (series.symbol != SymbolType.None) {
                    val radius = series.symbolSize.toPx()
                    val symColor = series.symbolColor ?: series.color

                    for (pt in segment) {
                        val x = mapper.toScreenX(pt.x)
                        val y = toScreenY(getDisplayY(pt))

                        // 仅当拐点中心处于可见坐标网格范围内时才进行绘制，防止滑动时滚出界面的圆点在边缘留存
                        if (x in grid.left..grid.right && y in grid.top..grid.bottom) {
                            val center = Offset(x, y)
                            when (series.symbol) {
                                SymbolType.Circle -> {
                                    drawCircle(color = symColor, radius = radius, center = center)
                                    if (series.showSymbolLabel && pt.y != null) {
                                        val text = pt.y.toInt().toString()
                                        val textLayout = textMeasurer.measure(
                                            text = text,
                                            style = style.xAxisOptions.labelTextStyle.copy(
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        val tw = textLayout.size.width
                                        val th = textLayout.size.height
                                        drawText(
                                            textLayoutResult = textLayout,
                                            topLeft = Offset(x - tw / 2f, y - th / 2f)
                                        )
                                    } else {
                                        drawCircle(color = Color.White, radius = radius * 0.6f, center = center)
                                    }
                                }
                                SymbolType.Square -> {
                                    drawRect(
                                        color = symColor,
                                        topLeft = Offset(x - radius, y - radius),
                                        size = Size(radius * 2f, radius * 2f)
                                    )
                                    if (series.showSymbolLabel && pt.y != null) {
                                        val text = pt.y.toInt().toString()
                                        val textLayout = textMeasurer.measure(
                                            text = text,
                                            style = style.xAxisOptions.labelTextStyle.copy(
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        val tw = textLayout.size.width
                                        val th = textLayout.size.height
                                        drawText(
                                            textLayoutResult = textLayout,
                                            topLeft = Offset(x - tw / 2f, y - th / 2f)
                                        )
                                    } else {
                                        drawRect(
                                            color = Color.White,
                                            topLeft = Offset(x - radius * 0.6f, y - radius * 0.6f),
                                            size = Size(radius * 1.2f, radius * 1.2f)
                                        )
                                    }
                                }
                                SymbolType.Diamond -> {
                                    val diamondPath = Path().apply {
                                        moveTo(x, y - radius)
                                        lineTo(x + radius, y)
                                        lineTo(x, y + radius)
                                        lineTo(x - radius, y)
                                        close()
                                    }
                                    drawPath(path = diamondPath, color = symColor)
                                    if (series.showSymbolLabel && pt.y != null) {
                                        val text = pt.y.toInt().toString()
                                        val textLayout = textMeasurer.measure(
                                            text = text,
                                            style = style.xAxisOptions.labelTextStyle.copy(
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        val tw = textLayout.size.width
                                        val th = textLayout.size.height
                                        drawText(
                                            textLayoutResult = textLayout,
                                            topLeft = Offset(x - tw / 2f, y - th / 2f)
                                        )
                                    } else {
                                        val innerDiamond = Path().apply {
                                            moveTo(x, y - radius * 0.6f)
                                            lineTo(x + radius * 0.6f, y)
                                            lineTo(x, y + radius * 0.6f)
                                            lineTo(x - radius * 0.6f, y)
                                            close()
                                        }
                                        drawPath(path = innerDiamond, color = Color.White)
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 生成当前系列对应到网格底部的完整面积 Path（单 contour 设计，彻底规避 addPath 导致的闭合 Bug）。
     */
    fun generateAreaPath(): Path {
        val totalAreaPath = Path()
        val points = series.points
        if (points.isEmpty()) return totalAreaPath

        val (leftIndex, rightIndex) = points.getVisibleRange(mapper.toDataX(grid.left) - 1f, mapper.toDataX(grid.right) + 1f)
        val visiblePoints = if (leftIndex <= rightIndex) points.subList(leftIndex, rightIndex + 1) else emptyList()
        if (visiblePoints.isEmpty()) return totalAreaPath

        val segments = mutableListOf<List<LinePoint>>()
        if (series.connectNulls) {
            val validPoints = visiblePoints.filter { it.y != null }
            if (validPoints.isNotEmpty()) {
                segments.add(validPoints)
            }
        } else {
            var currentSegment = mutableListOf<LinePoint>()
            for (pt in visiblePoints) {
                if (pt.y != null) {
                    currentSegment.add(pt)
                } else {
                    if (currentSegment.isNotEmpty()) {
                        segments.add(currentSegment)
                        currentSegment = mutableListOf()
                    }
                }
            }
            if (currentSegment.isNotEmpty()) {
                segments.add(currentSegment)
            }
        }

        for (segment in segments) {
            if (segment.isEmpty()) continue

            val startX = mapper.toScreenX(segment[0].x)
            val startY = toScreenY(getDisplayY(segment[0]))

            val areaPath = Path()
            // 单 contour 设计：起点为左下角底线位置
            areaPath.moveTo(startX, grid.bottom)
            areaPath.lineTo(startX, startY)

            if (segment.size >= 2) {
                if (series.stepType != StepType.None) {
                    for (i in 0 until segment.size - 1) {
                        val p1 = segment[i]
                        val p2 = segment[i + 1]
                        val x1 = mapper.toScreenX(p1.x)
                        val y1 = toScreenY(getDisplayY(p1))
                        val x2 = mapper.toScreenX(p2.x)
                        val y2 = toScreenY(getDisplayY(p2))

                        when (series.stepType) {
                            StepType.Start -> {
                                areaPath.lineTo(x1, y2)
                                areaPath.lineTo(x2, y2)
                            }
                            StepType.End -> {
                                areaPath.lineTo(x2, y1)
                                areaPath.lineTo(x2, y2)
                            }
                            StepType.Middle -> {
                                val xmid = (x1 + x2) / 2f
                                areaPath.lineTo(xmid, y1)
                                areaPath.lineTo(xmid, y2)
                                areaPath.lineTo(x2, y2)
                            }
                            else -> areaPath.lineTo(x2, y2)
                        }
                    }
                } else if (series.isSmooth) {
                    val smoothness = 0.16f
                    for (i in 0 until segment.size - 1) {
                        val p0 = segment[if (i - 1 >= 0) i - 1 else i]
                        val p1 = segment[i]
                        val p2 = segment[i + 1]
                        val p3 = segment[if (i + 2 < segment.size) i + 2 else i + 1]

                        val op0 = Offset(mapper.toScreenX(p0.x), toScreenY(getDisplayY(p0)))
                        val op1 = Offset(mapper.toScreenX(p1.x), toScreenY(getDisplayY(p1)))
                        val op2 = Offset(mapper.toScreenX(p2.x), toScreenY(getDisplayY(p2)))
                        val op3 = Offset(mapper.toScreenX(p3.x), toScreenY(getDisplayY(p3)))

                        val c1 = op1 + (op2 - op0) * smoothness
                        val c2 = op2 - (op3 - op1) * smoothness

                        areaPath.cubicTo(c1.x, c1.y, c2.x, c2.y, op2.x, op2.y)
                    }
                } else {
                    for (i in 1 until segment.size) {
                        val p = segment[i]
                        areaPath.lineTo(mapper.toScreenX(p.x), toScreenY(getDisplayY(p)))
                    }
                }
            }

            // 垂直落到底线并闭合
            val lastX = mapper.toScreenX(segment.last().x)
            areaPath.lineTo(lastX, grid.bottom)
            areaPath.close()

            val isStacked = baselinePoints != null && baselinePoints.any { it > 0f }
            val finalAreaPath = if (isStacked) {
                val baselineAreaPath = Path()
                val startBaselineY = toScreenY(baselinePoints!!.getOrNull(segment[0].x.toInt()) ?: 0f)
                
                // 基线单 contour 设计
                baselineAreaPath.moveTo(startX, grid.bottom)
                baselineAreaPath.lineTo(startX, startBaselineY)

                if (segment.size >= 2) {
                    if (series.stepType != StepType.None) {
                        for (i in 0 until segment.size - 1) {
                            val p1 = segment[i]
                            val p2 = segment[i + 1]
                            val x1 = mapper.toScreenX(p1.x)
                            val y1 = toScreenY(baselinePoints.getOrNull(p1.x.toInt()) ?: 0f)
                            val x2 = mapper.toScreenX(p2.x)
                            val y2 = toScreenY(baselinePoints.getOrNull(p2.x.toInt()) ?: 0f)

                            when (series.stepType) {
                                StepType.Start -> {
                                    baselineAreaPath.lineTo(x1, y2)
                                    baselineAreaPath.lineTo(x2, y2)
                                }
                                StepType.End -> {
                                    baselineAreaPath.lineTo(x2, y1)
                                    baselineAreaPath.lineTo(x2, y2)
                                }
                                StepType.Middle -> {
                                    val xmid = (x1 + x2) / 2f
                                    baselineAreaPath.lineTo(xmid, y1)
                                    baselineAreaPath.lineTo(xmid, y2)
                                    baselineAreaPath.lineTo(x2, y2)
                                }
                                else -> baselineAreaPath.lineTo(x2, y2)
                            }
                        }
                    } else if (series.isSmooth) {
                        val smoothness = 0.16f
                        for (i in 0 until segment.size - 1) {
                            val p0 = segment[if (i - 1 >= 0) i - 1 else i]
                            val p1 = segment[i]
                            val p2 = segment[i + 1]
                            val p3 = segment[if (i + 2 < segment.size) i + 2 else i + 1]

                            val b0 = baselinePoints.getOrNull(p0.x.toInt()) ?: 0f
                            val b1 = baselinePoints.getOrNull(p1.x.toInt()) ?: 0f
                            val b2 = baselinePoints.getOrNull(p2.x.toInt()) ?: 0f
                            val b3 = baselinePoints.getOrNull(p3.x.toInt()) ?: 0f

                            val op0 = Offset(mapper.toScreenX(p0.x), toScreenY(b0))
                            val op1 = Offset(mapper.toScreenX(p1.x), toScreenY(b1))
                            val op2 = Offset(mapper.toScreenX(p2.x), toScreenY(b2))
                            val op3 = Offset(mapper.toScreenX(p3.x), toScreenY(b3))

                            val c1 = op1 + (op2 - op0) * smoothness
                            val c2 = op2 - (op3 - op1) * smoothness

                            baselineAreaPath.cubicTo(c1.x, c1.y, c2.x, c2.y, op2.x, op2.y)
                        }
                    } else {
                        for (i in 1 until segment.size) {
                            val p = segment[i]
                            val by = toScreenY(baselinePoints.getOrNull(p.x.toInt()) ?: 0f)
                            baselineAreaPath.lineTo(mapper.toScreenX(p.x), by)
                        }
                    }
                }

                baselineAreaPath.lineTo(lastX, grid.bottom)
                baselineAreaPath.close()

                Path.combine(
                    operation = androidx.compose.ui.graphics.PathOperation.Difference,
                    path1 = areaPath,
                    path2 = baselineAreaPath
                )
            } else {
                areaPath
            }

            totalAreaPath.addPath(finalAreaPath)
        }

        return totalAreaPath
    }

    /**
     * 绘制最大/最小值红色定位浮标极其值文本 (MarkPoint)。
     */
    fun drawMarkPoints(drawScope: DrawScope) = with(drawScope) {
        val points = series.points.filter { it.y != null }
        if (points.isEmpty()) return@with

        // 仅在当前可见网格物理区间内查找极值
        val (leftIndex, rightIndex) = points.getVisibleRange(mapper.toDataX(grid.left) - 1f, mapper.toDataX(grid.right) + 1f)
        val visiblePoints = if (leftIndex <= rightIndex) points.subList(leftIndex, rightIndex + 1) else emptyList()
        if (visiblePoints.isEmpty()) return@with

        val maxPt = visiblePoints.maxByOrNull { it.y!! }
        val minPt = visiblePoints.minByOrNull { it.y!! }

        for (mp in series.markPoints) {
            val targetPt = when (mp.type) {
                MarkPointType.Max -> maxPt
                MarkPointType.Min -> minPt
                else -> null
            } ?: continue

            val x = mapper.toScreenX(targetPt.x)
            if (x !in grid.left..grid.right) continue
            val y = toScreenY(getDisplayY(targetPt))
            val label = mp.label ?: String.format("%.1f", targetPt.y).removeSuffix(".0")

            // 用 Path 直绘一个精美的水滴气泡定位浮标
            val bubbleRadius = 14.dp.toPx()
            val pinPath = Path().apply {
                moveTo(x, y)
                cubicTo(
                    x - bubbleRadius, y - bubbleRadius,
                    x - bubbleRadius, y - bubbleRadius * 2.2f,
                    x, y - bubbleRadius * 2.2f
                )
                cubicTo(
                    x + bubbleRadius, y - bubbleRadius * 2.2f,
                    x + bubbleRadius, y - bubbleRadius,
                    x, y
                )
                close()
            }
            drawScope.drawPath(path = pinPath, color = Color(0xFFEF5350))

            // 绘制气泡内部白字数值标签
            val textLayout = textMeasurer.measure(
                text = label,
                style = style.xAxisOptions.labelTextStyle.copy(
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            drawText(
                textLayoutResult = textLayout,
                topLeft = Offset(x - textLayout.size.width / 2f, y - bubbleRadius * 1.6f)
            )
        }
    }

    /**
     * 绘制平均值/恒定值水平虚线 (MarkLine)。
     */
    fun drawMarkLines(drawScope: DrawScope) = with(drawScope) {
        val points = series.points.filter { it.y != null }
        if (points.isEmpty()) return@with

        for (ml in series.markLines) {
            val lineVal = when (ml.type) {
                MarkLineType.Average -> points.map { it.y!! }.average().toFloat()
                MarkLineType.Constant -> ml.value ?: 0f
            }

            val y = toScreenY(lineVal)
            if (y in grid.top..grid.bottom) {
                // 绘制贯穿网格的水平虚线辅助线
                drawLine(
                    color = Color(0xFFEF5350),
                    start = Offset(grid.left, y),
                    end = Offset(grid.right, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                )

                // 绘制辅助线右下方的刻度文本
                val labelText = ml.label ?: String.format("平均值: %.1f", lineVal).removeSuffix(".0")
                val textLayout = textMeasurer.measure(
                    text = labelText,
                    style = style.xAxisOptions.labelTextStyle.copy(
                        color = Color(0xFFEF5350),
                        fontSize = 9.sp
                    )
                )
                drawText(
                    textLayoutResult = textLayout,
                    topLeft = Offset(
                        grid.right - textLayout.size.width - 4.dp.toPx(),
                        y - textLayout.size.height - 2.dp.toPx()
                    )
                )
            }
        }
    }
}
