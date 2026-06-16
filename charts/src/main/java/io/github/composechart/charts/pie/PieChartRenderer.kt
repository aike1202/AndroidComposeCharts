package io.github.composechart.charts.pie

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.composechart.core.data.CoordinateMapper
import io.github.composechart.core.style.ChartStyle
import io.github.composechart.core.style.RoseType
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * 纯 Compose Canvas 绘制的饼图/环形图/玫瑰图渲染器
 */
class PieChartRenderer(
    private val mapper: CoordinateMapper,
    private val style: ChartStyle,
    private val textMeasurer: TextMeasurer,
    private val density: Density,
    private val animationProgress: Float = 1.0f
) {
    private var grid = Rect.Zero
    var centerX = 0f
        private set
    var centerY = 0f
        private set
    var maxRadius = 0f
        private set

    // 暴露计算完毕的扇区参数，供交互碰撞检测使用
    var sliceDrawInfos = listOf<SliceDrawInfo>()
        private set

    data class SliceDrawInfo(
        val index: Int,
        val slice: PieSlice,
        val startAngle: Float,
        val sweepAngle: Float,
        val outerRadius: Float,
        val innerRadius: Float,
        val centerAngleRad: Double,
        val isSelected: Boolean,
        val animOffset: Offset
    )

    /**
     * 计算各扇区绘制位置
     */
    fun calculateSlices(
        slices: List<PieSlice>,
        hiddenList: List<String>,
        selectedIndex: Int?,
        selectedOffsetPx: Float,
        animProgress: Float = 1.0f,
        gridRect: Rect = Rect.Zero
    ) {
        val visibleSlices = slices.filter { it.name !in hiddenList }
        if (visibleSlices.isEmpty()) {
            sliceDrawInfos = emptyList()
            return
        }
        val totalValue = visibleSlices.sumOf { it.value.toDouble() }.toFloat()

        if (gridRect != Rect.Zero) {
            grid = gridRect
            centerX = gridRect.left + gridRect.width / 2f
            centerY = gridRect.top + gridRect.height / 2f

            val baseRadius = min(gridRect.width, gridRect.height) / 2f

            if (style.pieOptions.showLabel) {
                val line1 = with(density) { style.pieOptions.labelLineLength1.toPx() }
                val line2 = with(density) { style.pieOptions.labelLineLength2.toPx() }
                val textMargin = with(density) { 4.dp.toPx() }
                
                val baseColor = style.legendOptions.textStyle.color
                val labelColor = if (baseColor == Color.Unspecified) {
                    if (style.backgroundColor == Color.Transparent || style.backgroundColor.red > 0.5f) Color.Black else Color.White
                } else baseColor
                val labelStyle = style.legendOptions.textStyle.copy(fontSize = 11.sp, color = labelColor)

                // 找出所有可见标签中最长的那一个文本宽度
                var maxTextWidth = 0f
                visibleSlices.forEach { slice ->
                    val percent = if (totalValue > 0f) (slice.value / totalValue) * 100f else 0f
                    val text = "${slice.name} (${String.format("%.1f", percent)}%)"
                    val maxLabelWidth = (gridRect.width * 0.28f).coerceAtLeast(with(density) { 60.dp.toPx() }).toInt()
                    val textLayout = textMeasurer.measure(
                        text = text,
                        style = labelStyle,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        maxLines = 1,
                        constraints = androidx.compose.ui.unit.Constraints(maxWidth = maxLabelWidth)
                    )
                    maxTextWidth = maxTextWidth.coerceAtLeast(textLayout.size.width.toFloat())
                }

                val wReserve = maxTextWidth + line1 + line2 + textMargin
                val maxRadiusX = (gridRect.width / 2f - wReserve).coerceAtLeast(0f)
                val maxRadiusY = (gridRect.height / 2f - with(density) { 16.dp.toPx() }).coerceAtLeast(0f)
                val autoRadius = min(maxRadiusX, maxRadiusY)
                
                // 空间局促时强制使用保底比例(baseRadius * 0.5f)，防止饼图被挤压得太小；空间充裕时才进行比例缩放
                val limitRadius = baseRadius * 0.5f
                if (autoRadius < limitRadius) {
                    maxRadius = limitRadius
                } else {
                    maxRadius = autoRadius * style.pieOptions.outerRadiusRatio
                }
            } else {
                maxRadius = baseRadius * style.pieOptions.outerRadiusRatio
            }
        }
        val totalCount = visibleSlices.size

        val maxVal = visibleSlices.maxOfOrNull { it.value } ?: 1f
        val minVal = visibleSlices.minOfOrNull { it.value } ?: 0f

        val innerRadius = maxRadius * style.pieOptions.innerRadiusRatio
        val roseType = style.pieOptions.roseType
        val padAngle = style.pieOptions.padAngle

        val totalSweep = style.pieOptions.maxAngleSweep
        val minAngle = style.pieOptions.minAngle
        
        // 1. 计算原始扫掠角度列表
        val initialSweeps = visibleSlices.map { slice ->
            if (roseType == RoseType.Radius) {
                totalSweep / totalCount
            } else {
                if (totalValue > 0f) (slice.value / totalValue) * totalSweep else 0f
            }
        }.toFloatArray()

        // 2. 若配置了 minAngle 且具备分配余量，则进入重分配收敛迭代
        val finalSweeps = FloatArray(visibleSlices.size)
        if (minAngle > 0f && totalCount * minAngle <= totalSweep) {
            val adjusted = BooleanArray(visibleSlices.size) { false }
            val tempSweeps = initialSweeps.clone()
            var hasUnderMin = true
            var loopCount = 0
            while (hasUnderMin && loopCount < 10) { // 最多迭代 10 次收敛
                hasUnderMin = false
                var fixedSweepSum = 0f
                var activeValueSum = 0f
                for (i in tempSweeps.indices) {
                    if (adjusted[i]) {
                        fixedSweepSum += tempSweeps[i]
                    } else if (tempSweeps[i] < minAngle) {
                        tempSweeps[i] = minAngle
                        adjusted[i] = true
                        fixedSweepSum += minAngle
                        hasUnderMin = true
                    } else {
                        activeValueSum += visibleSlices[i].value
                    }
                }
                val remainingSweep = (totalSweep - fixedSweepSum).coerceAtLeast(0f)
                if (activeValueSum > 0f) {
                    for (i in tempSweeps.indices) {
                        if (!adjusted[i]) {
                            tempSweeps[i] = (visibleSlices[i].value / activeValueSum) * remainingSweep
                        }
                    }
                } else {
                    break
                }
                loopCount++
            }
            for (i in tempSweeps.indices) {
                finalSweeps[i] = tempSweeps[i]
            }
        } else {
            for (i in initialSweeps.indices) {
                finalSweeps[i] = initialSweeps[i]
            }
        }

        var currentAngle = style.pieOptions.startAngle

        val infos = mutableListOf<SliceDrawInfo>()
        var visibleIndex = 0

        slices.forEachIndexed { index, slice ->
            if (slice.name in hiddenList) return@forEachIndexed

            // 1. 圆心角计算 (应用 finalSweeps 得到满足 minAngle 后的最终角度)
            val sweepAngleRaw = finalSweeps.getOrNull(visibleIndex) ?: 0f
            val sweepAngle = sweepAngleRaw * animProgress

            // 2. 外径大小计算
            val outerRadius = if (roseType != RoseType.None) {
                // 玫瑰图外径根据数据极值进行比例映射，留底比例设为 35%
                val minRadius = innerRadius + (maxRadius - innerRadius) * 0.35f
                if (maxVal > minVal) {
                    minRadius + ((slice.value - minVal) / (maxVal - minVal)) * (maxRadius - minRadius)
                } else {
                    maxRadius
                }
            } else {
                maxRadius
            }

            // 3. 选中项平移 Offset
            val centerAngleRad = Math.toRadians((currentAngle + sweepAngle / 2f).toDouble())
            val isSelected = selectedIndex == index
            val animOffset = if (isSelected) {
                Offset(
                    (selectedOffsetPx * cos(centerAngleRad)).toFloat(),
                    (selectedOffsetPx * sin(centerAngleRad)).toFloat()
                )
            } else Offset.Zero

            // 4. 收缩空隙角 padAngle
            val startDrawAngle = currentAngle + padAngle / 2f
            val sweepDrawAngle = (sweepAngle - padAngle).coerceAtLeast(0.1f)

            infos.add(
                SliceDrawInfo(
                    index = index,
                    slice = slice,
                    startAngle = startDrawAngle,
                    sweepAngle = sweepDrawAngle,
                    outerRadius = outerRadius,
                    innerRadius = innerRadius,
                    centerAngleRad = centerAngleRad,
                    isSelected = isSelected,
                    animOffset = animOffset
                )
            )

            currentAngle += sweepAngleRaw
            visibleIndex++
        }
        sliceDrawInfos = infos
    }

    /**
     * 绘制扇区
     */
    fun drawSlices(drawScope: DrawScope) = with(drawScope) {
        val cornerRadiusPx = with(density) { style.pieOptions.cornerRadius.toPx() }

        // 确定背景隔离线的颜色
        val bgClr = if (style.backgroundColor == Color.Transparent) {
            val baseColor = style.legendOptions.textStyle.color
            if (baseColor != Color.Unspecified && baseColor.red < 0.5f) Color.White else Color(0xFF1B1B1D)
        } else style.backgroundColor

        // 临时缓存，以供第二图层统一描边
        val pathList = mutableListOf<Path>()

        sliceDrawInfos.forEach { info ->

            if (info.sweepAngle <= 0f) return@forEach

            val path = Path()
            val xc = centerX + info.animOffset.x
            val yc = centerY + info.animOffset.y
            val rOut = info.outerRadius
            val rIn = info.innerRadius

            // 限制圆角半径，防止当扫掠角过小或者厚度不够时圆角重叠畸变
            var realRc = cornerRadiusPx
            if (realRc > 0f) {
                val maxRcHeight = (rOut - rIn) / 2f
                val sweepRad = Math.toRadians(info.sweepAngle.toDouble())
                val maxRcArcOut = (rOut * sweepRad / 2f).toFloat()
                val maxRcArcIn = if (rIn > 0f) {
                    (rIn * sweepRad / 2f).toFloat()
                } else {
                    maxRcArcOut
                }
                realRc = realRc.coerceAtMost(maxRcHeight).coerceAtMost(maxRcArcOut).coerceAtMost(maxRcArcIn)
            }

            val startAngle = info.startAngle
            val sweepAngle = info.sweepAngle
            val endAngle = startAngle + sweepAngle

            val startRad = Math.toRadians(startAngle.toDouble())
            val endRad = Math.toRadians(endAngle.toDouble())

            if (realRc > 0f) {
                // 计算内外径上圆角对应的偏转角（弧度）
                val daOut = (realRc / rOut).toDouble()
                val daIn = if (rIn > 0f) (realRc / rIn).toDouble() else 0.0

                val daOutDeg = Math.toDegrees(daOut).toFloat()
                val daInDeg = Math.toDegrees(daIn).toFloat()

                // 直角顶点 (用于贝塞尔曲线控制点)
                val vInStart = Offset(xc + rIn * cos(startRad).toFloat(), yc + rIn * sin(startRad).toFloat())
                val vOutStart = Offset(xc + rOut * cos(startRad).toFloat(), yc + rOut * sin(startRad).toFloat())
                val vOutEnd = Offset(xc + rOut * cos(endRad).toFloat(), yc + rOut * sin(endRad).toFloat())
                val vInEnd = Offset(xc + rIn * cos(endRad).toFloat(), yc + rIn * sin(endRad).toFloat())

                // 缩进点：径向直线端
                val lInStart = Offset(xc + (rIn + realRc) * cos(startRad).toFloat(), yc + (rIn + realRc) * sin(startRad).toFloat())
                val lOutStart = Offset(xc + (rOut - realRc) * cos(startRad).toFloat(), yc + (rOut - realRc) * sin(startRad).toFloat())
                val lOutEnd = Offset(xc + (rOut - realRc) * cos(endRad).toFloat(), yc + (rOut - realRc) * sin(endRad).toFloat())
                val lInEnd = Offset(xc + (rIn + realRc) * cos(endRad).toFloat(), yc + (rIn + realRc) * sin(endRad).toFloat())

                // 缩进点：大圆弧端
                val aOutStartAngle = startAngle + daOutDeg
                val aOutEndAngle = endAngle - daOutDeg
                val aOutStart = Offset(
                    xc + rOut * cos(Math.toRadians(aOutStartAngle.toDouble())).toFloat(),
                    yc + rOut * sin(Math.toRadians(aOutStartAngle.toDouble())).toFloat()
                )

                if (rIn > 0f) {
                    // 缩进点：内圆弧端
                    val aInStartAngle = startAngle + daInDeg
                    val aInEndAngle = endAngle - daInDeg
                    val aInStart = Offset(
                        xc + rIn * cos(Math.toRadians(aInStartAngle.toDouble())).toFloat(),
                        yc + rIn * sin(Math.toRadians(aInStartAngle.toDouble())).toFloat()
                    )
                    val aInEnd = Offset(
                        xc + rIn * cos(Math.toRadians(aInEndAngle.toDouble())).toFloat(),
                        yc + rIn * sin(Math.toRadians(aInEndAngle.toDouble())).toFloat()
                    )

                    // 1. 起点定位在内径起始圆弧端
                    path.moveTo(aInStart.x, aInStart.y)
                    // 2. 绘制内径起始角的圆角
                    path.quadraticTo(vInStart.x, vInStart.y, lInStart.x, lInStart.y)
                    // 3. 连直线到外径起始直角缩进点
                    path.lineTo(lOutStart.x, lOutStart.y)
                    // 4. 绘制外径起始角的圆角
                    path.quadraticTo(vOutStart.x, vOutStart.y, aOutStart.x, aOutStart.y)
                    // 5. 绘制外圆弧
                    val outerRect = Rect(xc - rOut, yc - rOut, xc + rOut, yc + rOut)
                    path.arcTo(outerRect, aOutStartAngle, aOutEndAngle - aOutStartAngle, false)
                    // 6. 绘制外径结束角的圆角
                    path.quadraticTo(vOutEnd.x, vOutEnd.y, lOutEnd.x, lOutEnd.y)
                    // 7. 连直线到内径结束直角缩进点
                    path.lineTo(lInEnd.x, lInEnd.y)
                    // 8. 绘制内径结束角的圆角
                    path.quadraticTo(vInEnd.x, vInEnd.y, aInEnd.x, aInEnd.y)
                    // 9. 绘制内圆弧并闭合
                    val innerRect = Rect(xc - rIn, yc - rIn, xc + rIn, yc + rIn)
                    path.arcTo(innerRect, aInEndAngle, aInStartAngle - aInEndAngle, false)
                } else {
                    // 1. 起点定位在圆心
                    path.moveTo(xc, yc)
                    // 2. 连线到外侧起始直角缩进点
                    path.lineTo(lOutStart.x, lOutStart.y)
                    // 3. 绘制外径起始角圆角
                    path.quadraticTo(vOutStart.x, vOutStart.y, aOutStart.x, aOutStart.y)
                    // 4. 绘制外圆弧
                    val outerRect = Rect(xc - rOut, yc - rOut, xc + rOut, yc + rOut)
                    path.arcTo(outerRect, aOutStartAngle, aOutEndAngle - aOutStartAngle, false)
                    // 5. 绘制外径结束角圆角
                    path.quadraticTo(vOutEnd.x, vOutEnd.y, lOutEnd.x, lOutEnd.y)
                    // 6. 连线回圆心
                    path.lineTo(xc, yc)
                }
                path.close()
            } else {
                // 用最稳健的原生 drawArc 绘制彩色填充，免去任何 Path 兼容性或精度造成的渲染失败
                if (rIn > 0f) {
                    val thickness = rOut - rIn
                    val midRadius = (rOut + rIn) / 2f
                    drawArc(
                        color = info.slice.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(xc - midRadius, yc - midRadius),
                        size = Size(midRadius * 2f, midRadius * 2f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = thickness)
                    )
                } else {
                    drawArc(
                        color = info.slice.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        topLeft = Offset(xc - rOut, yc - rOut),
                        size = Size(rOut * 2f, rOut * 2f),
                        style = androidx.compose.ui.graphics.drawscope.Fill
                    )
                }

                // 依然构建 path，留作后面的边框 Stroke（如果配置了 borderWidth 的话）
                val p1X = xc + rIn * cos(startRad).toFloat()
                val p1Y = yc + rIn * sin(startRad).toFloat()
                path.moveTo(p1X, p1Y)

                val p2X = xc + rOut * cos(startRad).toFloat()
                val p2Y = yc + rOut * sin(startRad).toFloat()
                path.lineTo(p2X, p2Y)

                val outerRect = Rect(xc - rOut, yc - rOut, xc + rOut, yc + rOut)
                path.arcTo(
                    rect = outerRect,
                    startAngleDegrees = startAngle,
                    sweepAngleDegrees = sweepAngle,
                    forceMoveTo = false
                )

                val p4X = xc + rIn * cos(endRad).toFloat()
                val p4Y = yc + rIn * sin(endRad).toFloat()
                path.lineTo(p4X, p4Y)

                if (rIn > 0f) {
                    val innerRect = Rect(xc - rIn, yc - rIn, xc + rIn, yc + rIn)
                    path.arcTo(
                        rect = innerRect,
                        startAngleDegrees = endAngle,
                        sweepAngleDegrees = -sweepAngle,
                        forceMoveTo = false
                    )
                } else {
                    path.lineTo(xc, yc)
                }
                path.close()
            }

            pathList.add(path)

            // 第一步：如果是圆角路径，则通过 drawPath 绘制彩色填充；直角扇区已用 drawArc 画过
            if (realRc > 0f) {
                drawPath(
                    path = path,
                    color = info.slice.color,
                    style = androidx.compose.ui.graphics.drawscope.Fill
                )
            }
        }

        // 第二步：统一绘制背景描边边框 (Stroke)，防止后画的 Fill 遮挡/截断先画的 Border
        val borderWidthPx = with(density) { style.pieOptions.borderWidth.toPx() }
        if (borderWidthPx > 0f) {
            val borderClr = style.pieOptions.borderColor ?: bgClr
            pathList.forEach { path ->
                drawPath(
                    path = path,
                    color = borderClr,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = borderWidthPx)
                )
            }
        }

        // 第三步：统一绘制 padAngle 背景分割线，防止后画的 Fill 遮挡/截断先画的 padAngle 线
        if (style.pieOptions.padAngle > 0f) {
            sliceDrawInfos.forEach { info ->
                if (info.sweepAngle <= 0f) return@forEach
                val xc = centerX + info.animOffset.x
                val yc = centerY + info.animOffset.y
                val rOut = info.outerRadius
                val rIn = info.innerRadius

                val padAngle = style.pieOptions.padAngle
                val boundaryAngle = info.startAngle - padAngle / 2f
                val bRad = Math.toRadians(boundaryAngle.toDouble())

                val pStart = Offset(xc + rIn * cos(bRad).toFloat(), yc + rIn * sin(bRad).toFloat())
                val pEnd = Offset(xc + rOut * cos(bRad).toFloat(), yc + rOut * sin(bRad).toFloat())

                drawLine(
                    color = bgClr,
                    start = pStart,
                    end = pEnd,
                    strokeWidth = with(density) { padAngle.dp.toPx() }
                )
            }
        }
    }

    /**
     * 绘制引导折线与百分比文本
     */
    fun drawLabelsAndLines(drawScope: DrawScope, totalValue: Float) = with(drawScope) {
        if (!style.pieOptions.showLabel || sliceDrawInfos.isEmpty()) return@with

        val lineLength1 = with(density) { style.pieOptions.labelLineLength1.toPx() }
        val lineLength2 = with(density) { style.pieOptions.labelLineLength2.toPx() }
        val lineWidth = with(density) { style.pieOptions.labelLineWidth.toPx() }
        
        // 确保字体颜色在暗色/浅色背景下高可读
        val baseColor = style.legendOptions.textStyle.color
        val labelColor = if (baseColor == Color.Unspecified) {
            if (style.backgroundColor == Color.Transparent || style.backgroundColor.red > 0.5f) Color.Black else Color.White
        } else baseColor
        val labelStyle = style.legendOptions.textStyle.copy(fontSize = 11.sp, color = labelColor)

        // 收集待渲染的文字及坐标
        val labelItems = sliceDrawInfos.mapNotNull { info ->
            val percent = if (totalValue > 0f) (info.slice.value / totalValue) * 100f else 0f
            if (percent < style.pieOptions.minShowLabelPercent) return@mapNotNull null

            val xc = centerX + info.animOffset.x
            val yc = centerY + info.animOffset.y
            val rOut = info.outerRadius

            // 起点 (外径边缘)
            val startX = xc + rOut * cos(info.centerAngleRad).toFloat()
            val startY = yc + rOut * sin(info.centerAngleRad).toFloat()

            // 是否在圆心左侧
            val isLeft = cos(info.centerAngleRad) < 0
            val isTop = sin(info.centerAngleRad) < 0

            // 根据 labelAlign 决定折角点 X, Y 坐标
            val midX: Float
            val midY: Float
            when (style.pieOptions.labelAlign) {
                io.github.composechart.core.style.PieLabelAlign.LeftRight -> {
                    midX = if (isLeft) {
                        centerX - maxRadius - lineLength1
                    } else {
                        centerX + maxRadius + lineLength1
                    }
                    midY = yc + (rOut + lineLength1) * sin(info.centerAngleRad).toFloat()
                }
                io.github.composechart.core.style.PieLabelAlign.TopBottom -> {
                    midX = xc + (rOut + lineLength1) * cos(info.centerAngleRad).toFloat()
                    midY = if (isTop) {
                        centerY - maxRadius - lineLength1
                    } else {
                        centerY + maxRadius + lineLength1
                    }
                }
                else -> {
                    midX = xc + (rOut + lineLength1) * cos(info.centerAngleRad).toFloat()
                    midY = yc + (rOut + lineLength1) * sin(info.centerAngleRad).toFloat()
                }
            }

            val text = "${info.slice.name} (${String.format("%.1f", percent)}%)"
            
            // 限制单行文字最大宽度为容器网格宽度的 28% (最低不低于 60dp)，超出自动 Ellipsis 截断防止挤爆容器
            val maxLabelWidth = (grid.width * 0.28f).coerceAtLeast(with(density) { 60.dp.toPx() }).toInt()
            val textLayout = textMeasurer.measure(
                text = text,
                style = labelStyle,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                maxLines = 1,
                constraints = androidx.compose.ui.unit.Constraints(maxWidth = maxLabelWidth)
            )
            val textWidth = textLayout.size.width.toFloat()
            val textHeight = textLayout.size.height.toFloat()

            // 水平折线终点
            val dir = if (isLeft) -1f else 1f
            val endX = midX + lineLength2 * dir
            val endY = midY

            LabelLayoutItem(
                info = info,
                isLeft = isLeft,
                startPt = Offset(startX, startY),
                midPt = Offset(midX, midY),
                endPt = Offset(endX, endY),
                textY = endY - textHeight / 2f,
                textHeight = textHeight,
                textWidth = textWidth,
                text = text,
                textLayout = textLayout
            )
        }

        // 计算单侧最大可容纳的标签行数，防止过密重叠溢出边界
        val itemHeight = with(density) { 22.dp.toPx() }
        val maxAllowed = (grid.height / itemHeight).toInt().coerceAtLeast(2)

        // 分左右侧进行防碰撞推挤调整，并按占比大小保留前 maxAllowed 个
        val leftGroup = labelItems.filter { it.isLeft }.sortedBy { it.textY }
        val rightGroup = labelItems.filter { !it.isLeft }.sortedBy { it.textY }

        val finalLeftGroup = if (leftGroup.size <= maxAllowed) {
            leftGroup
        } else {
            val kept = leftGroup.sortedByDescending { it.info.slice.value }.take(maxAllowed)
            leftGroup.filter { it in kept }
        }

        val finalRightGroup = if (rightGroup.size <= maxAllowed) {
            rightGroup
        } else {
            val kept = rightGroup.sortedByDescending { it.info.slice.value }.take(maxAllowed)
            rightGroup.filter { it in kept }
        }

        adjustLabelsY(finalLeftGroup, lineLength1, lineLength2)
        adjustLabelsY(finalRightGroup, lineLength1, lineLength2)

        // 循环绘制所有被保留下来的项目
        val finalDrawItems = finalLeftGroup + finalRightGroup
        finalDrawItems.forEach { item ->
            val lineColor = style.pieOptions.labelLineColor ?: item.info.slice.color

            // 1. 画折线
            drawLine(
                color = lineColor,
                start = item.startPt,
                end = item.midPt,
                strokeWidth = lineWidth
            )
            drawLine(
                color = lineColor,
                start = item.midPt,
                end = item.endPt,
                strokeWidth = lineWidth
            )

            // 2. 画文本
            val textX = if (item.isLeft) {
                item.endPt.x - item.textWidth - 4.dp.toPx()
            } else {
                item.endPt.x + 4.dp.toPx()
            }

            drawText(
                textLayoutResult = item.textLayout,
                topLeft = Offset(textX, item.textY)
            )
        }
    }

    /**
     * Y 轴排序文字重叠调整算法
     */
    private fun adjustLabelsY(group: List<LabelLayoutItem>, lineLength1: Float, lineLength2: Float) {
        if (group.isEmpty()) return

        val minGap = with(density) { 16.dp.toPx() } // 文字行间最小间距
        val bottomLimit = grid.bottom - minGap
        val topLimit = grid.top + minGap

        // 1. 正向推挤 (从上至下)
        for (i in 1 until group.size) {
            val prev = group[i - 1]
            val curr = group[i]
            if (curr.textY < prev.textY + minGap) {
                curr.textY = prev.textY + minGap
            }
        }

        // 2. 溢出反向推挤 (自下而上)
        if (group.last().textY > bottomLimit) {
            group.last().textY = bottomLimit
            for (i in (group.size - 2) downTo 0) {
                val next = group[i + 1]
                val curr = group[i]
                if (curr.textY > next.textY - minGap) {
                    curr.textY = next.textY - minGap
                }
            }
        }

        // 3. 顶部越界夹逼校验
        if (group.first().textY < topLimit) {
            group.first().textY = topLimit
            for (i in 1 until group.size) {
                val prev = group[i - 1]
                val curr = group[i]
                if (curr.textY < prev.textY + minGap) {
                    curr.textY = prev.textY + minGap
                }
            }
        }

        // 4. 重对齐引导线端点 (加入防穿透圆盘算法)
        val rSafeMargin = with(density) { 8.dp.toPx() } // 安全向外偏置
        group.forEach { item ->
            val newEndY = item.textY + item.textHeight / 2f
            val rOut = item.info.outerRadius
            
            // 计算为了不穿过圆盘，在当前 Y 轴高度下，折角点 x 距离圆心 centerX 的最小水平距离 minDx
            val dy = newEndY - centerY
            val rSafe = rOut + rSafeMargin
            val minDx = if (rSafe * rSafe > dy * dy) {
                kotlin.math.sqrt(rSafe * rSafe - dy * dy)
            } else {
                0f
            }
            
            // 调整折角点 midPt.x，确保它在安全圆盘范围外
            var newMidX = item.midPt.x
            if (style.pieOptions.labelAlign == io.github.composechart.core.style.PieLabelAlign.None) {
                if (item.isLeft) {
                    val maxMidX = centerX - minDx
                    if (newMidX > maxMidX) {
                        newMidX = maxMidX
                    }
                } else {
                    val minMidX = centerX + minDx
                    if (newMidX < minMidX) {
                        newMidX = minMidX
                    }
                }
            }
            
            item.midPt = Offset(newMidX, newEndY)
            
            // 保持水平段长度 lineLength2，同步更新 endPt
            val dir = if (item.isLeft) -1f else 1f
            item.endPt = Offset(newMidX + lineLength2 * dir, newEndY)
        }
    }

    private class LabelLayoutItem(
        val info: SliceDrawInfo,
        val isLeft: Boolean,
        val startPt: Offset,
        var midPt: Offset, // 改为 var 以支持在防重叠微调时重对齐其 Y 坐标
        var endPt: Offset,
        var textY: Float,
        val textHeight: Float,
        val textWidth: Float,
        val text: String,
        val textLayout: androidx.compose.ui.text.TextLayoutResult
    )
}
