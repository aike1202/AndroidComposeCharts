package io.github.composechart.charts.bar3d

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.composechart.core.style.ChartStyle
import kotlin.math.cos
import kotlin.math.sin

/**
 * 3D 柱体图渲染引擎，负责三维坐标旋转投影、底盒与墙网格直绘、深度排序与立体面着色
 */
class Bar3DChartRenderer(
    val data: Bar3DChartData,
    val options: Bar3DOptions,
    val yaw: Float,               // 偏航角，绕 Z 轴旋转
    val pitch: Float,             // 俯仰角，视角上下倾斜
    val style: ChartStyle,
    val textMeasurer: TextMeasurer,
    val isDark: Boolean,
    val zoom: Float = 1.0f,        // 视角缩放比例
    val selectedPoint: Bar3DPoint? = null // 当前选中的柱体，用于聚焦高亮效果
) {
    data class ProjectedBarInfo(
        val pt: Bar3DPoint,
        val topCenter: Offset,
        val bottomCenter: Offset,
        val radiusPx: Float
    )
    val projectedBars = mutableListOf<ProjectedBarInfo>()

    // 虚拟 3D 视盒尺寸
    private val boxX = 300f
    private val boxY = 240f
    private val boxZ = 180f
    private val cameraDistance = 650f

    // 辅助三维坐标结构
    private data class Point3D(val x: Float, val y: Float, val z: Float)

    /**
     * 辅助色彩混合函数，为柱面提供 flat shading 光影强度
     */
    private fun blendWithBlack(color: Color, factor: Float): Color {
        return Color(
            red = color.red * (1f - factor),
            green = color.green * (1f - factor),
            blue = color.blue * (1f - factor),
            alpha = color.alpha
        )
    }

    /**
     * 将 3D 空间点旋转并投射到 2D 屏幕坐标
     */
    private fun rotateAndProject(pt: Point3D, center: Offset): Offset {
        // 绕中心进行旋转。由于 Z 轴垂直朝上，Z 坐标中心取为 boxZ / 2
        val dx = pt.x
        val dy = pt.y
        val dz = pt.z - (boxZ / 2f)

        val yawRad = Math.toRadians(yaw.toDouble())
        val cosY = cos(yawRad).toFloat()
        val sinY = sin(yawRad).toFloat()
        
        // 1. 水平偏航旋转（绕 Z 轴）
        val x1 = dx * cosY - dy * sinY
        val y1 = dx * sinY + dy * cosY
        val z1 = dz

        val pitchRad = Math.toRadians(pitch.toDouble())
        val cosP = cos(pitchRad).toFloat()
        val sinP = sin(pitchRad).toFloat()

        // 2. 垂直俯仰旋转（绕 X 轴）
        val x2 = x1
        val y2 = y1 * cosP - z1 * sinP
        val z2 = y1 * sinP + z1 * cosP

        // 3. 透视投影计算 (Perspective Projection)
        // 假定相机位于 (0, -cameraDistance, 0)，面向 Y 轴正方向
        val depth = cameraDistance + y2
        val scale = if (depth > 10f) cameraDistance / depth else 1f

        // Z 轴朝上，因此屏幕 Y 坐标需取负
        val screenX = center.x + x2 * scale * zoom
        val screenY = center.y - z2 * scale * zoom

        return Offset(screenX, screenY)
    }

    /**
     * 根据数值 Z 计算对应热力色彩渐变
     */
    fun getColorForValue(zValue: Float): Color {
        val colors = options.visualMapColors
        if (colors.isEmpty()) return Color.Gray
        if (colors.size == 1) return colors.first()

        val fraction = ((zValue - data.zMin) / (data.zMax - data.zMin)).coerceIn(0f, 1f)
        val segments = colors.size - 1
        val scaled = fraction * segments
        val idx = scaled.toInt().coerceIn(0, segments - 1)
        val localF = scaled - idx
        
        return lerp(colors[idx], colors[idx + 1], localF)
    }

    /**
     * 绘制 3D 图表主体
     */
    fun draw(drawScope: DrawScope) = with(drawScope) {
        projectedBars.clear()
        
        val widthPx = size.width
        val heightPx = size.height
        // 视图投影中心坐标
        val center = Offset(widthPx / 2f, heightPx * 0.45f)

        val textStyle = TextStyle(
            color = options.labelColor ?: if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
            fontSize = 8.sp
        )

        // 1. 计算八个箱体角点以定位 3D 底盒与墙壁
        val xMin = -boxX / 2f
        val xMax = boxX / 2f
        val yMin = -boxY / 2f
        val yMax = boxY / 2f
        val zMin = 0f
        val zMax = boxZ

        // 投影底面四个角
        val f00 = rotateAndProject(Point3D(xMin, yMin, zMin), center)
        val f10 = rotateAndProject(Point3D(xMax, yMin, zMin), center)
        val f11 = rotateAndProject(Point3D(xMax, yMax, zMin), center)
        val f01 = rotateAndProject(Point3D(xMin, yMax, zMin), center)

        // 投影顶面四个角
        val r00 = rotateAndProject(Point3D(xMin, yMin, zMax), center)
        val r10 = rotateAndProject(Point3D(xMax, yMin, zMax), center)
        val r11 = rotateAndProject(Point3D(xMax, yMax, zMax), center)
        val r01 = rotateAndProject(Point3D(xMin, yMax, zMax), center)

        // ================= 1. 绘制底面 (Floor Grid) 与背景墙 (Back Walls) =================
        // 1.1 绘制浅色背景底面
        val floorPath = Path().apply {
            moveTo(f00.x, f00.y)
            lineTo(f10.x, f10.y)
            lineTo(f11.x, f11.y)
            lineTo(f01.x, f01.y)
            close()
        }
        drawPath(
            path = floorPath,
            color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.4f) else Color(0xFFF1F5F9).copy(alpha = 0.5f),
            style = Fill
        )

        // 1.2 绘制 Y = yMax（后方墙壁）与 X = xMin（左侧墙壁）网格背景
        val backWallPath = Path().apply {
            moveTo(f01.x, f01.y)
            lineTo(f11.x, f11.y)
            lineTo(r11.x, r11.y)
            lineTo(r01.x, r01.y)
            close()
        }
        drawPath(
            path = backWallPath,
            color = if (isDark) Color(0xFF334155).copy(alpha = 0.2f) else Color(0xFFE2E8F0).copy(alpha = 0.3f),
            style = Fill
        )

        val leftWallPath = Path().apply {
            moveTo(f00.x, f00.y)
            lineTo(f01.x, f01.y)
            lineTo(r01.x, r01.y)
            lineTo(r00.x, r00.y)
            close()
        }
        drawPath(
            path = leftWallPath,
            color = if (isDark) Color(0xFF334155).copy(alpha = 0.15f) else Color(0xFFE2E8F0).copy(alpha = 0.2f),
            style = Fill
        )

        // 1.3 绘制底网格线和后墙网格线
        val xCount = data.xAxisLabels.size
        val yCount = data.yAxisLabels.size
        val zCount = 5

        // 底面平行于 Y 轴网格线
        for (i in 0..xCount) {
            val ratio = i.toFloat() / xCount
            val xCoord = xMin + ratio * boxX
            val pStart = rotateAndProject(Point3D(xCoord, yMin, 0f), center)
            val pEnd = rotateAndProject(Point3D(xCoord, yMax, 0f), center)
            drawLine(options.gridColor, pStart, pEnd, strokeWidth = 0.5.dp.toPx())
        }
        // 底面平行于 X 轴网格线
        for (j in 0..yCount) {
            val ratio = j.toFloat() / yCount
            val yCoord = yMin + ratio * boxY
            val pStart = rotateAndProject(Point3D(xMin, yCoord, 0f), center)
            val pEnd = rotateAndProject(Point3D(xMax, yCoord, 0f), center)
            drawLine(options.gridColor, pStart, pEnd, strokeWidth = 0.5.dp.toPx())
        }
        // 后方与左侧侧墙 Z 轴等高刻度线
        for (k in 0..zCount) {
            val ratio = k.toFloat() / zCount
            val zCoord = ratio * boxZ
            
            // 后墙横线
            val p1 = rotateAndProject(Point3D(xMin, yMax, zCoord), center)
            val p2 = rotateAndProject(Point3D(xMax, yMax, zCoord), center)
            drawLine(options.gridColor, p1, p2, strokeWidth = 0.5.dp.toPx())

            // 左墙横线
            val p3 = rotateAndProject(Point3D(xMin, yMin, zCoord), center)
            val p4 = rotateAndProject(Point3D(xMin, yMax, zCoord), center)
            drawLine(options.gridColor, p3, p4, strokeWidth = 0.5.dp.toPx())
        }

        // ================= 2. 绘制轴标签 (Axis Labels) =================
        // 2.1 绘制 X 轴标签（在底盒前边缘 Y = yMin）
        for (i in 0 until xCount step 2) {
            val ratio = (i + 0.5f) / xCount
            val xCoord = xMin + ratio * boxX
            val labelPt = rotateAndProject(Point3D(xCoord, yMin - 8f, 0f), center)
            val labelText = data.xAxisLabels.getOrNull(i) ?: ""
            val layout = textMeasurer.measure(labelText, style = textStyle)
            drawText(layout, topLeft = Offset(labelPt.x - layout.size.width / 2f, labelPt.y - layout.size.height / 2f))
        }

        // 2.2 绘制 Y 轴标签（在底盒右边缘 X = xMax）
        for (j in 0 until yCount) {
            val ratio = (j + 0.5f) / yCount
            val yCoord = yMin + ratio * boxY
            val labelPt = rotateAndProject(Point3D(xMax + 10f, yCoord, 0f), center)
            val labelText = data.yAxisLabels.getOrNull(j) ?: ""
            val layout = textMeasurer.measure(labelText, style = textStyle)
            drawText(layout, topLeft = Offset(labelPt.x - layout.size.width / 2f, labelPt.y - layout.size.height / 2f))
        }

        // 2.3 绘制 Z 轴刻度文本（在后左拐角 X = xMin, Y = yMax）
        for (k in 0..zCount) {
            val ratio = k.toFloat() / zCount
            val zCoord = ratio * boxZ
            val zVal = data.zMin + ratio * (data.zMax - data.zMin)
            val labelPt = rotateAndProject(Point3D(xMin - 8f, yMax + 8f, zCoord), center)
            val labelText = String.format("%.0f", zVal)
            val layout = textMeasurer.measure(labelText, style = textStyle)
            drawText(layout, topLeft = Offset(labelPt.x - layout.size.width, labelPt.y - layout.size.height / 2f))
        }

        // ================= 3. 画家算法：3D 柱体排序与绘制 (Painter's Algorithm) =================
        // 每个格网的实际跨度大小
        val gridStepX = boxX / xCount
        val gridStepY = boxY / yCount
        
        // 柱体实际物理半径（考虑 barWidthRatio）
        val radX = (gridStepX * options.barWidthRatio) / 2f
        val radY = (gridStepY * options.barWidthRatio) / 2f

        // 计算每一个柱体在旋转后三维坐标系里的深度 Y。
        // 由于相机视点朝向 +Y，因此旋转变换后 Y 值越大的点，深度越深，离相机越远。
        val columnCenterDepths = data.points.map { pt ->
            // 计算柱心 3D 物理坐标
            val cx = xMin + (pt.xIndex + 0.5f) * gridStepX
            val cy = yMin + (pt.yIndex + 0.5f) * gridStepY
            val zValNorm = ((pt.zValue - data.zMin) / (data.zMax - data.zMin)).coerceIn(0f, 1f)
            val cz = zValNorm * boxZ / 2f // 柱心 Z 轴中值点
            
            // 绕偏航角 yaw 和俯仰角 pitch 计算旋转坐标点
            val dx = cx
            val dy = cy
            val dz = cz - (boxZ / 2f)

            val yawRad = Math.toRadians(yaw.toDouble())
            val cosY = cos(yawRad).toFloat()
            val sinY = sin(yawRad).toFloat()
            val x1 = dx * cosY - dy * sinY
            val y1 = dx * sinY + dy * cosY

            val pitchRad = Math.toRadians(pitch.toDouble())
            val cosP = cos(pitchRad).toFloat()
            val sinP = sin(pitchRad).toFloat()
            
            // 得到旋转后的 Y 坐标深度
            val ry = y1 * cosP - dz * sinP

            pt to ry
        }

        // 按旋转深度降序排列（深度大的即 ry 最大的，先绘制；ry 最小的即离相机最近的，后绘制）
        val sortedPoints = columnCenterDepths.sortedByDescending { it.second }.map { it.first }

        for (pt in sortedPoints) {
            val zValNorm = ((pt.zValue - data.zMin) / (data.zMax - data.zMin)).coerceIn(0f, 1f)
            val h = zValNorm * boxZ
            if (h <= 0.05f) continue // 数值极小或为 0 时跳过绘制

            val rawColor = pt.color ?: getColorForValue(pt.zValue)
            val baseColor = if (selectedPoint != null && pt != selectedPoint) {
                rawColor.copy(alpha = 0.2f)
            } else {
                rawColor
            }

            // 计算柱体的 3D 八个顶点
            val cx = xMin + (pt.xIndex + 0.5f) * gridStepX
            val cy = yMin + (pt.yIndex + 0.5f) * gridStepY

            // 底部 4 个角点 (Z = 0)
            val b00 = rotateAndProject(Point3D(cx - radX, cy - radY, 0f), center)
            val b10 = rotateAndProject(Point3D(cx + radX, cy - radY, 0f), center)
            val b11 = rotateAndProject(Point3D(cx + radX, cy + radY, 0f), center)
            val b01 = rotateAndProject(Point3D(cx - radX, cy + radY, 0f), center)

            // 顶部 4 个角点 (Z = h)
            val t00 = rotateAndProject(Point3D(cx - radX, cy - radY, h), center)
            val t10 = rotateAndProject(Point3D(cx + radX, cy - radY, h), center)
            val t11 = rotateAndProject(Point3D(cx + radX, cy + radY, h), center)
            val t01 = rotateAndProject(Point3D(cx - radX, cy + radY, h), center)

            // 缓存投影后柱顶、柱底中心点以及物理半径，用于交互 Tooltip 精确命中判定
            val topCenter = rotateAndProject(Point3D(cx, cy, h), center)
            val bottomCenter = rotateAndProject(Point3D(cx, cy, 0f), center)
            
            // 计算此柱体投影后的 2D 物理半径
            val yawRad = Math.toRadians(yaw.toDouble())
            val cosY = cos(yawRad).toFloat()
            val sinY = sin(yawRad).toFloat()
            val x1 = cx * cosY - cy * sinY
            val y1 = cx * sinY + cy * cosY
            
            val pitchRad = Math.toRadians(pitch.toDouble())
            val cosP = cos(pitchRad).toFloat()
            val sinP = sin(pitchRad).toFloat()
            
            val depth = cameraDistance + (y1 * cosP - (h / 2f - boxZ / 2f) * sinP)
            val scale = if (depth > 10f) cameraDistance / depth else 1f
            val radiusPx = radX * scale * zoom
            
            projectedBars.add(ProjectedBarInfo(pt, topCenter, bottomCenter, radiusPx))

            // ================= 4. 根据偏航角 yaw 映射可见侧面并着色绘制 =================
            // 任何视角从上往下看（pitch > 0），Z 轴上方的顶面总是可见的
            val topPath = Path().apply {
                moveTo(t00.x, t00.y)
                lineTo(t10.x, t10.y)
                lineTo(t11.x, t11.y)
                lineTo(t01.x, t01.y)
                close()
            }
            drawPath(path = topPath, color = baseColor, style = Fill)
            
            // 顶面配白色高亮细描边，使得网格柱子间分界更分明
            drawPath(path = topPath, color = Color.White.copy(alpha = 0.25f), style = Stroke(width = 0.5.dp.toPx()))

            // 侧边表面着色（光照衰减强度：正侧面 15% 衰减，旁侧面 30% 衰减）
            val yawNormalized = (yaw % 360f + 360f) % 360f // 归一化为 0..360 度
            
            // 根据水平视角象限，只绘制面向镜头的 2 个侧面
            if (yawNormalized >= 270f || yawNormalized < 0f) {
                // 第一象限视角（偏左侧）：可见前侧面 (Y轴最小值) 与右侧面 (X轴最大值)
                val frontPath = Path().apply {
                    moveTo(b00.x, b00.y)
                    lineTo(t00.x, t00.y)
                    lineTo(t10.x, t10.y)
                    lineTo(b10.x, b10.y)
                    close()
                }
                drawPath(path = frontPath, color = blendWithBlack(baseColor, 0.15f), style = Fill)

                val rightPath = Path().apply {
                    moveTo(b10.x, b10.y)
                    lineTo(t10.x, t10.y)
                    lineTo(t11.x, t11.y)
                    lineTo(b11.x, b11.y)
                    close()
                }
                drawPath(path = rightPath, color = blendWithBlack(baseColor, 0.30f), style = Fill)

            } else if (yawNormalized >= 0f && yawNormalized < 90f) {
                // 第二象限视角（偏右侧）：可见前侧面 (Y轴最小值) 与左侧面 (X轴最小值)
                val frontPath = Path().apply {
                    moveTo(b00.x, b00.y)
                    lineTo(t00.x, t00.y)
                    lineTo(t10.x, t10.y)
                    lineTo(b10.x, b10.y)
                    close()
                }
                drawPath(path = frontPath, color = blendWithBlack(baseColor, 0.15f), style = Fill)

                val leftPath = Path().apply {
                    moveTo(b01.x, b01.y)
                    lineTo(t01.x, t01.y)
                    lineTo(t00.x, t00.y)
                    lineTo(b00.x, b00.y)
                    close()
                }
                drawPath(path = leftPath, color = blendWithBlack(baseColor, 0.30f), style = Fill)

            } else if (yawNormalized >= 90f && yawNormalized < 180f) {
                // 第三象限视角（偏后右）：可见后侧面 (Y轴最大值) 与左侧面 (X轴最小值)
                val backPath = Path().apply {
                    moveTo(b01.x, b01.y)
                    lineTo(t01.x, t01.y)
                    lineTo(t11.x, t11.y)
                    lineTo(b11.x, b11.y)
                    close()
                }
                drawPath(path = backPath, color = blendWithBlack(baseColor, 0.15f), style = Fill)

                val leftPath = Path().apply {
                    moveTo(b01.x, b01.y)
                    lineTo(t01.x, t01.y)
                    lineTo(t00.x, t00.y)
                    lineTo(b00.x, b00.y)
                    close()
                }
                drawPath(path = leftPath, color = blendWithBlack(baseColor, 0.30f), style = Fill)

            } else {
                // 第四象限视角（偏后左）：可见后侧面 (Y轴最大值) 与右侧面 (X轴最大值)
                val backPath = Path().apply {
                    moveTo(b01.x, b01.y)
                    lineTo(t01.x, t01.y)
                    lineTo(t11.x, t11.y)
                    lineTo(b11.x, b11.y)
                    close()
                }
                drawPath(path = backPath, color = blendWithBlack(baseColor, 0.15f), style = Fill)

                val rightPath = Path().apply {
                    moveTo(b10.x, b10.y)
                    lineTo(t10.x, t10.y)
                    lineTo(t11.x, t11.y)
                    lineTo(b11.x, b11.y)
                    close()
                }
                drawPath(path = rightPath, color = blendWithBlack(baseColor, 0.30f), style = Fill)
            }

            // 如果此柱子当前被选中，绘制一个亮色高光发光线框包围盒以突出显示
            if (pt == selectedPoint) {
                val highlightColor = Color.White.copy(alpha = 0.9f)
                val strokeW = 1.5.dp.toPx()
                
                // 顶面四边棱
                drawLine(highlightColor, t00, t10, strokeWidth = strokeW)
                drawLine(highlightColor, t10, t11, strokeWidth = strokeW)
                drawLine(highlightColor, t11, t01, strokeWidth = strokeW)
                drawLine(highlightColor, t01, t00, strokeWidth = strokeW)

                // 侧面四条立边
                drawLine(highlightColor, b00, t00, strokeWidth = strokeW)
                drawLine(highlightColor, b10, t10, strokeWidth = strokeW)
                drawLine(highlightColor, b11, t11, strokeWidth = strokeW)
                drawLine(highlightColor, b01, t01, strokeWidth = strokeW)
            }
        }

        // ================= 5. 绘制顶盖外盒网格线框架 (Outline Box Border) =================
        // 连接前柱、后柱之后，画出 3D 盒顶面外边线，使空间网格更完整清晰
        drawLine(options.gridColor, r00, r10, strokeWidth = 1.dp.toPx())
        drawLine(options.gridColor, r10, r11, strokeWidth = 1.dp.toPx())
        drawLine(options.gridColor, r11, r01, strokeWidth = 1.dp.toPx())
        drawLine(options.gridColor, r01, r00, strokeWidth = 1.dp.toPx())
        
        // 绘制立柱盒边缘骨架边线
        drawLine(options.gridColor, f00, r00, strokeWidth = 1.dp.toPx())
        drawLine(options.gridColor, f10, r10, strokeWidth = 1.dp.toPx())
        drawLine(options.gridColor, f11, r11, strokeWidth = 1.dp.toPx())
        drawLine(options.gridColor, f01, r01, strokeWidth = 1.dp.toPx())

        // ================= 6. 绘制渐变色卡对照图例 (Visual Map Legend) =================
        if (options.showVisualMap && options.visualMapColors.isNotEmpty()) {
            val legendW = 120.dp.toPx()
            val legendH = 8.dp.toPx()
            val legendX = 24.dp.toPx() // 左下角排布
            val legendY = heightPx - 20.dp.toPx()

            val brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = options.visualMapColors.reversed(),
                startY = legendY - legendW,
                endY = legendY
            )
            // 绘制左下角纵向渐变色卡
            drawRoundRect(
                brush = brush,
                topLeft = Offset(legendX, legendY - legendW),
                size = Size(legendH, legendW),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                style = Fill
            )

            // 绘制色卡上下限数值文本
            val maxStr = String.format("%.0f", data.zMax)
            val minStr = String.format("%.0f", data.zMin)
            
            val maxLayout = textMeasurer.measure(maxStr, style = textStyle)
            val minLayout = textMeasurer.measure(minStr, style = textStyle)
            
            drawText(maxLayout, topLeft = Offset(legendX + legendH + 6.dp.toPx(), legendY - legendW))
            drawText(minLayout, topLeft = Offset(legendX + legendH + 6.dp.toPx(), legendY - minLayout.size.height.toFloat()))
        }
    }
}
