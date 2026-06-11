package io.github.composechart.core.data

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import io.github.composechart.core.state.ViewportState

/**
 * 数学映射计算核心引擎，负责数据物理值与屏幕像素位置（Canvas 坐标系）的双向转换。
 *
 * @param gridRect 代表绘图网格区域（已扣除四周 Margin 的物理像素 Rect）。
 * @param viewportState 当前可见的数据视口区间状态。
 */
class CoordinateMapper(
    val gridRect: Rect,
    private val viewportState: ViewportState,
    val isYInversed: Boolean = false
) {
    /**
     * 将数据点 X 坐标值转换为屏幕 X 轴物理像素值。
     */
    fun toScreenX(x: Float): Float {
        val dx = viewportState.maxX - viewportState.minX
        if (dx == 0f || gridRect.width == 0f) {
            return gridRect.left
        }
        return gridRect.left + ((x - viewportState.minX) / dx) * gridRect.width
    }

    /**
     * 将数据点 Y 坐标值转换为屏幕 Y 轴物理像素值。
     * 注意：屏幕 Y 轴向下为正方向，与数据直角坐标轴方向相反。
     */
    fun toScreenY(y: Float): Float {
        val dy = viewportState.maxY - viewportState.minY
        if (dy == 0f || gridRect.height == 0f) {
            return gridRect.bottom
        }
        return if (isYInversed) {
            gridRect.top + ((y - viewportState.minY) / dy) * gridRect.height
        } else {
            gridRect.bottom - ((y - viewportState.minY) / dy) * gridRect.height
        }
    }

    /**
     * 将数据坐标对 (x, y) 转换为屏幕物理偏移 Offset。
     */
    fun toScreenOffset(x: Float, y: Float): Offset {
        return Offset(toScreenX(x), toScreenY(y))
    }

    /**
     * 将包含可空 Y 值的数据转换为屏幕物理偏移 Offset（当 Y 为 null 时返回 null）。
     */
    fun toScreenOffset(x: Float, y: Float?): Offset? {
        if (y == null) return null
        return Offset(toScreenX(x), toScreenY(y))
    }

    /**
     * 将副 Y 轴（Y2）的数据值转换为屏幕 Y 轴物理像素值。
     */
    fun toScreenY2(y: Float, y2Min: Float, y2Max: Float): Float {
        val dy = y2Max - y2Min
        if (dy == 0f || gridRect.height == 0f) {
            return gridRect.bottom
        }
        return gridRect.bottom - ((y - y2Min) / dy) * gridRect.height
    }

    /**
     * 将屏幕 Y 轴物理像素位置反转为副 Y 轴（Y2）的数据值。
     */
    fun toDataY2(screenY: Float, y2Min: Float, y2Max: Float): Float {
        val h = gridRect.height
        if (h == 0f) {
            return y2Min
        }
        val dy = y2Max - y2Min
        return y2Min + ((gridRect.bottom - screenY) / h) * dy
    }

    /**
     * 将屏幕 X 轴物理像素位置反转为数据坐标中的 X 值（支持线性外推）。
     */
    fun toDataX(screenX: Float): Float {
        val w = gridRect.width
        if (w == 0f) {
            return viewportState.minX
        }
        val dx = viewportState.maxX - viewportState.minX
        return viewportState.minX + ((screenX - gridRect.left) / w) * dx
    }

    /**
     * 将屏幕 Y 轴物理像素位置反转为数据坐标中的 Y 值（支持线性外推）。
     */
    fun toDataY(screenY: Float): Float {
        val h = gridRect.height
        if (h == 0f) {
            return viewportState.minY
        }
        val dy = viewportState.maxY - viewportState.minY
        return if (isYInversed) {
            viewportState.minY + ((screenY - gridRect.top) / h) * dy
        } else {
            viewportState.minY + ((gridRect.bottom - screenY) / h) * dy
        }
    }
}
