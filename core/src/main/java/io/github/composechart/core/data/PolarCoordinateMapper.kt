package io.github.composechart.core.data

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import io.github.composechart.core.style.PolarOptions
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * 极坐标系物理映射算子，负责将数据空间中的极坐标 (radiusValue, anglePercent) 转换为屏幕上的 Offset 物理坐标。
 */
class PolarCoordinateMapper(
    val gridRect: Rect,
    val polarOptions: PolarOptions,
    val minRadiusVal: Float,
    val maxRadiusVal: Float,
    val outerPaddingPx: Float = 100f // 预留给外部标签排版的物理像素间距
) {
    // 圆心物理坐标
    val centerX = gridRect.left + gridRect.width * polarOptions.centerX
    val centerY = gridRect.top + gridRect.height * polarOptions.centerY

    // 最大物理半径
    val maxRadiusPx = (min(gridRect.width, gridRect.height) / 2f - outerPaddingPx).coerceAtLeast(0f)

    /**
     * 将极坐标数据点 (r, anglePct) 映射到屏幕物理坐标 Offset
     * @param r 数据半径值
     * @param anglePct 角度轴的百分比位置 (0.0f - 1.0f)
     */
    fun toScreenPoint(r: Float, anglePct: Float): Offset {
        val radiusRange = maxRadiusVal - minRadiusVal
        val radiusPx = if (radiusRange > 0f) {
            ((r - minRadiusVal) / radiusRange).coerceIn(0f, 1f) * maxRadiusPx
        } else 0f

        val totalSweep = polarOptions.endAngle - polarOptions.startAngle
        // 将极坐标转换为数学物理角度，并转换为弧度
        val angleDegree = polarOptions.startAngle + anglePct * totalSweep
        val angleRad = Math.toRadians(angleDegree.toDouble())

        val px = centerX + radiusPx * cos(angleRad).toFloat()
        val py = centerY + radiusPx * sin(angleRad).toFloat()
        return Offset(px, py)
    }

    /**
     * 将给定的物理半径大小转换成屏幕物理半径像素
     */
    fun getPhysicalRadius(r: Float): Float {
        val radiusRange = maxRadiusVal - minRadiusVal
        return if (radiusRange > 0f) {
            ((r - minRadiusVal) / radiusRange).coerceIn(0f, 1f) * maxRadiusPx
        } else 0f
    }

    /**
     * 将给定的角度轴百分比转换成屏幕物理角度度数
     */
    fun getPhysicalAngle(anglePct: Float): Float {
        val totalSweep = polarOptions.endAngle - polarOptions.startAngle
        return polarOptions.startAngle + anglePct * totalSweep
    }
}
