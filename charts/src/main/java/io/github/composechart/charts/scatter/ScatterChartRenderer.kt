package io.github.composechart.charts.scatter

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.composechart.core.data.CoordinateMapper
import io.github.composechart.core.style.ChartStyle

/**
 * 散点图/气泡图系列渲染器，负责单系列数据的 Canvas 绘制逻辑（点、涟漪动画、高亮投影等）。
 */
class ScatterChartRenderer(
    private val mapper: CoordinateMapper,
    private val series: ScatterSeries,
    private val style: ChartStyle,
    private val textMeasurer: TextMeasurer,
    private val visualMap: ScatterVisualMap? = null,
    private val animationProgress: Float = 0f,      // 涟漪动画进度 0.0f..1.0f
    private val hoveredPointIndex: Int? = null     // 当前手势 Hover 选中的数据点索引
) {
    private val gridRect = mapper.gridRect

    /**
     * 点的渲染属性缓存
     */
    private data class PointProperties(
        val radius: Float,
        val color: Color
    )

    /**
     * 绘制整个系列的散点（含特效和发光）
     */
    fun draw(drawScope: DrawScope) = with(drawScope) {
        clipRect(
            left = gridRect.left,
            top = gridRect.top,
            right = gridRect.right,
            bottom = gridRect.bottom
        ) {
            for (i in series.points.indices) {
            val pt = series.points[i]
            val screenX = mapper.toScreenX(pt.x)
            val screenY = mapper.toScreenY(pt.y)

            // 裁剪过滤：只有在可视区域内的散点才进行绘制
            if (screenX !in (gridRect.left - 20f)..(gridRect.right + 20f)) continue
            if (screenY !in (gridRect.top - 20f)..(gridRect.bottom + 20f)) continue

            val center = Offset(screenX, screenY)
            val props = getPointProperties(this, pt)

            val isHovered = hoveredPointIndex == i
            val drawRadius = if (isHovered) props.radius * 1.5f else props.radius
            val drawColor = props.color

            // 1. 若开启涟漪，则绘制背景扩散波纹 (EffectScatter)
            if (series.effectScatter) {
                drawRipples(drawScope, center, props.radius, drawColor, series.effectOptions)
            }

            // 2. 绘制散点主体形状
            if (isHovered) {
                // Hover 状态：使用 Android 原生 Paint 渲染器绘制外发光阴影效果
                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        color = drawColor.toArgb()
                        this.style = android.graphics.Paint.Style.FILL
                        // 绘制霓虹外发光投影
                        setShadowLayer(
                            12.dp.toPx(),
                            0f, 0f,
                            drawColor.copy(alpha = 0.85f).toArgb()
                        )
                    }

                    when (series.symbol) {
                        ScatterSymbolType.Circle -> {
                            canvas.nativeCanvas.drawCircle(center.x, center.y, drawRadius, paint)
                        }
                        ScatterSymbolType.Square -> {
                            canvas.nativeCanvas.drawRect(
                                center.x - drawRadius, center.y - drawRadius,
                                center.x + drawRadius, center.y + drawRadius,
                                paint
                            )
                        }
                        ScatterSymbolType.Triangle -> {
                            val nativePath = android.graphics.Path().apply {
                                moveTo(center.x, center.y - drawRadius)
                                lineTo(center.x - drawRadius * 0.866f, center.y + drawRadius * 0.5f)
                                lineTo(center.x + drawRadius * 0.866f, center.y + drawRadius * 0.5f)
                                close()
                            }
                            canvas.nativeCanvas.drawPath(nativePath, paint)
                        }
                        ScatterSymbolType.Diamond -> {
                            val nativePath = android.graphics.Path().apply {
                                moveTo(center.x, center.y - drawRadius)
                                lineTo(center.x + drawRadius, center.y)
                                lineTo(center.x, center.y + drawRadius)
                                lineTo(center.x - drawRadius, center.y)
                                close()
                            }
                            canvas.nativeCanvas.drawPath(nativePath, paint)
                        }
                    }
                }
            } else {
                // 常规状态：纯 Compose Canvas 高效绘制
                drawSymbol(this, center, drawRadius, drawColor, series.symbol, Fill)
            }

            // 3. 绘制散点边框（描边）
            val strokeColor = series.strokeColor ?: drawColor.copy(alpha = 0.6f)
            val strokeWidthPx = if (isHovered) {
                (series.strokeWidth * 1.5f).toPx()
            } else {
                series.strokeWidth.toPx()
            }
            if (strokeWidthPx > 0f) {
                drawSymbol(this, center, drawRadius, strokeColor, series.symbol, Stroke(width = strokeWidthPx))
            }
        }
    }
}

    /**
     * 计算特定点的半径大小与色彩（融合 VisualMap 配置）
     */
    private fun getPointProperties(density: Density, point: ScatterPoint): PointProperties = with(density) {
        val baseRadius = (series.symbolSize / 2f).toPx()
        val baseColor = series.color

        if (point.value == null || visualMap == null) {
            return PointProperties(baseRadius, baseColor)
        }

        // 归一化比率 fraction 0.0f..1.0f
        val range = visualMap.max - visualMap.min
        val fraction = if (range > 0f) {
            ((point.value - visualMap.min) / range).coerceIn(0f, 1f)
        } else {
            0.5f
        }

        // 1. 映射半径
        val minSizePx = visualMap.minSize.toPx()
        val maxSizePx = visualMap.maxSize.toPx()
        val mappedRadius = minSizePx / 2f + fraction * (maxSizePx - minSizePx) / 2f

        // 2. 映射颜色
        val mappedColor = if (visualMap.colorRange.isNotEmpty()) {
            interpolateColor(visualMap.colorRange, fraction)
        } else {
            baseColor
        }

        // 3. 映射透明度
        val finalColor = if (visualMap.alphaRange != null) {
            val (minAlpha, maxAlpha) = visualMap.alphaRange
            val alpha = minAlpha + fraction * (maxAlpha - minAlpha)
            mappedColor.copy(alpha = alpha)
        } else {
            mappedColor
        }

        return PointProperties(mappedRadius, finalColor)
    }

    /**
     * 多色阶线性颜色插值
     */
    private fun interpolateColor(colors: List<Color>, fraction: Float): Color {
        if (colors.isEmpty()) return Color.Unspecified
        if (colors.size == 1) return colors[0]

        val f = fraction.coerceIn(0f, 1f)
        val segmentCount = colors.size - 1
        val scaledFraction = f * segmentCount
        val index = scaledFraction.toInt().coerceAtMost(segmentCount - 1)
        val segmentFraction = scaledFraction - index

        val startColor = colors[index]
        val endColor = colors[index + 1]

        return Color(
            red = startColor.red + segmentFraction * (endColor.red - startColor.red),
            green = startColor.green + segmentFraction * (endColor.green - startColor.green),
            blue = startColor.blue + segmentFraction * (endColor.blue - startColor.blue),
            alpha = startColor.alpha + segmentFraction * (endColor.alpha - startColor.alpha)
        )
    }

    /**
     * 绘制指定形状 symbol
     */
    private fun drawSymbol(
        drawScope: DrawScope,
        center: Offset,
        radius: Float,
        color: Color,
        symbolType: ScatterSymbolType,
        drawStyle: androidx.compose.ui.graphics.drawscope.DrawStyle
    ) = with(drawScope) {
        when (symbolType) {
            ScatterSymbolType.Circle -> {
                drawCircle(
                    color = color,
                    radius = radius,
                    center = center,
                    style = drawStyle
                )
            }
            ScatterSymbolType.Square -> {
                drawRect(
                    color = color,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = drawStyle
                )
            }
            ScatterSymbolType.Triangle -> {
                val path = Path().apply {
                    moveTo(center.x, center.y - radius)
                    lineTo(center.x - radius * 0.866f, center.y + radius * 0.5f)
                    lineTo(center.x + radius * 0.866f, center.y + radius * 0.5f)
                    close()
                }
                drawPath(path = path, color = color, style = drawStyle)
            }
            ScatterSymbolType.Diamond -> {
                val path = Path().apply {
                    moveTo(center.x, center.y - radius)
                    lineTo(center.x + radius, center.y)
                    lineTo(center.x, center.y + radius)
                    lineTo(center.x - radius, center.y)
                    close()
                }
                drawPath(path = path, color = color, style = drawStyle)
            }
        }
    }

    /**
     * 绘制特效涟漪扩散波纹
     */
    private fun drawRipples(
        drawScope: DrawScope,
        center: Offset,
        baseRadius: Float,
        baseColor: Color,
        options: EffectOptions
    ) = with(drawScope) {
        val count = options.rippleCount
        val ratio = options.rippleRadiusRatio

        for (k in 0 until count) {
            // 计算当前波纹圈的独立动画进度
            val progress = (animationProgress + k.toFloat() / count) % 1.0f
            val rippleRadius = baseRadius * (1f + (ratio - 1f) * progress)
            val rippleAlpha = (1f - progress) * 0.45f * baseColor.alpha

            drawCircle(
                color = baseColor.copy(alpha = rippleAlpha),
                radius = rippleRadius,
                center = center,
                style = Stroke(width = 1.2.dp.toPx())
            )
        }
    }
}
