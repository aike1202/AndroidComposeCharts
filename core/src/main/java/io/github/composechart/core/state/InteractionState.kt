package io.github.composechart.core.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

/**
 * 维护图表交互行为的响应式状态类（如扫掠光标与长按浮窗信息）。
 */
class InteractionState {
    /**
     * Tooltip 是否正处于扫掠激活状态。
     */
    var isTooltipActive by mutableStateOf(false)

    /**
     * 当前扫掠对齐的 X 轴数据空间坐标值。
     */
    var tooltipDataX by mutableStateOf<Float?>(null)

    /**
     * 当前触控点在屏幕上的物理像素坐标 Offset。
     */
    var tooltipScreenOffset by mutableStateOf<Offset?>(null)
}

/**
 * 在 Composable 中创建并记住 [InteractionState] 的便捷方法。
 */
@Composable
fun rememberInteractionState(): InteractionState {
    return remember { InteractionState() }
}
