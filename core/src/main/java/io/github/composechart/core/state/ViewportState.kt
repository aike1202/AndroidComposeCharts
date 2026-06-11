package io.github.composechart.core.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * 视口显示模式配置，明确区分“全景填充模式”与“只显部分、滑动查看模式”。
 */
sealed interface ViewportMode {
    /**
     * 全景填充模式（默认）：展示全部数据到可见固定的宽度内。
     */
    object Fit : ViewportMode

    /**
     * 滚动模式：可见范围内初始只显示指定数量的数据点，剩下的需要滑动查看。
     * @property visibleCount 初始可见的数据项数量（大于等于1）
     */
    data class Scrollable(val visibleCount: Int) : ViewportMode
}

/**
 * 管理图表可见数据区间的数据状态类，可驱动图表的平移（Pan）与缩放（Zoom）绘制。
 */
class ViewportState(
    initialMinX: Float = 0f,
    initialMaxX: Float = 100f,
    initialMinY: Float = 0f,
    initialMaxY: Float = 100f,
    initialViewportMode: ViewportMode = ViewportMode.Fit,
    initialEnablePan: Boolean = true,
    initialEnableZoom: Boolean = true
) {
    var minX by mutableFloatStateOf(initialMinX)
    var maxX by mutableFloatStateOf(initialMaxX)
    var minY by mutableFloatStateOf(initialMinY)
    var maxY by mutableFloatStateOf(initialMaxY)
    var viewportMode by mutableStateOf<ViewportMode>(initialViewportMode)
    var enablePan by mutableStateOf(initialEnablePan)
    var enableZoom by mutableStateOf(initialEnableZoom)
    
    private var lastAppliedViewportMode: ViewportMode? = null

    /**
     * 根据数据特征和当前视口模式初始化 X 轴范围。
     */
    fun initializeRange(
        isCategory: Boolean,
        categorySize: Int,
        numericMin: Float = 0f,
        numericMax: Float = 100f,
        allSortedXValues: List<Float> = emptyList(),
        categoryPadding: Float = 0f
    ) {
        val isDefaultRange = minX == 0f && maxX == 100f
        val modeChanged = viewportMode != lastAppliedViewportMode

        // 只有在范围为默认 [0f, 100f]（首屏首次加载），或者视口模式/可见个数被明确调节改变时，才允许重新初始化；
        // 这既能保证 Slider 滑动调节 visibleCount 实时生效，又能保证在同模式滑动时不会重置用户的手势偏置位置。
        if (!isDefaultRange && !modeChanged) return

        lastAppliedViewportMode = viewportMode

        when (val mode = viewportMode) {
            is ViewportMode.Fit -> {
                if (isCategory) {
                    if (categorySize > 0) {
                        minX = -categoryPadding
                        maxX = (categorySize - 1).toFloat() + categoryPadding
                    }
                } else {
                    minX = numericMin
                    maxX = numericMax
                }
            }
            is ViewportMode.Scrollable -> {
                if (isCategory) {
                    if (categorySize > 0) {
                        minX = -categoryPadding
                        val limitMax = (mode.visibleCount - 1).coerceAtLeast(0).coerceAtMost(categorySize - 1).toFloat()
                        maxX = limitMax + categoryPadding
                    }
                } else {
                    if (allSortedXValues.isNotEmpty()) {
                        val minVal = allSortedXValues.first()
                        val maxVal = if (mode.visibleCount <= allSortedXValues.size) {
                            allSortedXValues[mode.visibleCount - 1]
                        } else {
                            allSortedXValues.last()
                        }
                        minX = minVal
                        maxX = maxVal
                    } else {
                        minX = numericMin
                        maxX = numericMax
                    }
                }
            }
        }
    }

    /**
     * 以 [focusX] 为焦点进行 X 轴方向缩放。
     * [scale] 大于 1.0f 时表示放大（可见范围缩窄），小于 1.0f 时表示缩小（可见范围拓宽）。
     */
    fun zoom(scale: Float, focusX: Float) {
        if (scale <= 0f || scale == 1.0f) return
        val currentWidth = maxX - minX
        if (currentWidth <= 0f) return

        val newWidth = currentWidth / scale
        val ratio = if (currentWidth == 0f) 0.5f else (focusX - minX) / currentWidth

        minX = focusX - newWidth * ratio
        maxX = minX + newWidth
    }

    /**
     * 在 X 轴方向平移 [deltaX] 距离。
     */
    fun pan(deltaX: Float) {
        minX += deltaX
        maxX += deltaX
    }

    /**
     * 规整并限制视口可见边界及缩放跨度，防止过度平移产生大面积空白，并防止过度缩放。
     */
    fun constrain(minLimitX: Float, maxLimitX: Float, minSpanX: Float = 2f) {
        val maxSpanX = (maxLimitX - minLimitX).coerceAtLeast(minSpanX)
        var currentSpan = maxX - minX

        // 1. 规整缩放跨度
        if (currentSpan > maxSpanX) {
            currentSpan = maxSpanX
        }
        if (currentSpan < minSpanX) {
            currentSpan = minSpanX
        }

        // 2. 规整平移位置（防左右出界）
        if (minX < minLimitX) {
            minX = minLimitX
            maxX = minX + currentSpan
        }
        if (maxX > maxLimitX) {
            maxX = maxLimitX
            minX = (maxX - currentSpan).coerceAtLeast(minLimitX)
        }
    }
}

/**
 * 在 Composable 中创建并记住 [ViewportState] 的便捷方法。
 */
@Composable
fun rememberViewportState(
    initialMinX: Float = 0f,
    initialMaxX: Float = 100f,
    initialMinY: Float = 0f,
    initialMaxY: Float = 100f,
    viewportMode: ViewportMode = ViewportMode.Fit,
    enablePan: Boolean = true,
    enableZoom: Boolean = true
): ViewportState {
    return remember {
        ViewportState(
            initialMinX = initialMinX,
            initialMaxX = initialMaxX,
            initialMinY = initialMinY,
            initialMaxY = initialMaxY,
            initialViewportMode = viewportMode,
            initialEnablePan = enablePan,
            initialEnableZoom = enableZoom
        )
    }
}
