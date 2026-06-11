package io.github.composechart.core.gesture

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import io.github.composechart.core.data.CoordinateMapper
import io.github.composechart.core.state.InteractionState
import io.github.composechart.core.state.ViewportState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 复合手势分发器，协调单指平移、双指捏合缩放与长按 400ms 锁定激活 Tooltip 扫掠手势。
 * 使用 composed {} 包装并以 Unit 作为 key 来配置 pointerInput，防止 recompose 重建导致手势中断。
 */
fun Modifier.chartGestureDetector(
    mapper: CoordinateMapper,
    viewportState: ViewportState,
    interactionState: InteractionState,
    hapticFeedback: HapticFeedback,
    horizontal: Boolean = false
): Modifier = this.composed {
    val currentMapper by rememberUpdatedState(mapper)
    val currentViewportState by rememberUpdatedState(viewportState)
    val currentInteractionState by rememberUpdatedState(interactionState)
    val currentHapticFeedback by rememberUpdatedState(hapticFeedback)
    val currentHorizontal by rememberUpdatedState(horizontal)

    pointerInput(Unit) {
        coroutineScope {
            while (isActive) {
                // 1. 等待第一个指针按下事件
                val down = awaitPointerEventScope {
                    awaitFirstDown(requireUnconsumed = false)
                }

                // 2. 开启协程，用于计时 400ms 长按检测
                var longPressTriggered = false
                val longPressJob = launch {
                    delay(400)
                    longPressTriggered = true
                    // 触发震动反馈
                    currentHapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    // 激活扫掠 Tooltip 状态并更新物理像素和数据空间坐标
                    currentInteractionState.isTooltipActive = true
                    val initialVal = if (!currentHorizontal) {
                        currentMapper.toDataX(down.position.x)
                    } else {
                        currentMapper.toDataY(down.position.y)
                    }
                    currentInteractionState.tooltipDataX = initialVal
                    currentInteractionState.tooltipScreenOffset = down.position
                }

                // 3. 拦截后续移动与抬起手势，进行手势防冲突转换
                awaitPointerEventScope {
                    var dragAmountAccumulated = Offset.Zero
                    val touchSlop = viewConfiguration.touchSlop
                    var prevPosition = down.position
                    
                    // 引入滑动方向锁定状态：0 = 未决定，1 = 水平滑动，2 = 垂直滑动
                    var dragDirection = 0

                    while (true) {
                        val event = awaitPointerEvent()
                        val changes = event.changes

                        // 3.1 所有手指抬起，重置 Tooltip 扫掠状态并结束当前循环
                        if (changes.all { !it.pressed }) {
                            longPressJob.cancel()
                            if (currentInteractionState.isTooltipActive) {
                                currentInteractionState.isTooltipActive = false
                                currentInteractionState.tooltipDataX = null
                                currentInteractionState.tooltipScreenOffset = null
                            }
                            break
                        }

                        // 3.2 手指滑动判断：如果尚未触发长按且移动量超过 touchSlop，则取消长按监听，并锁定滑动方向
                        val activeChange = changes.firstOrNull { it.pressed }
                        if (activeChange != null && !longPressTriggered) {
                            val diff = activeChange.position - prevPosition
                            dragAmountAccumulated += diff
                            
                            if (dragDirection == 0 && dragAmountAccumulated.getDistance() > touchSlop) {
                                longPressJob.cancel()
                                dragDirection = if (kotlin.math.abs(dragAmountAccumulated.x) > kotlin.math.abs(dragAmountAccumulated.y)) {
                                    1 // 确立为水平滑动
                                } else {
                                    2 // 确立为垂直滑动
                                }
                            }
                        }

                        // 3.3 如果处于 Tooltip 状态，消费所有事件以避免父列表（ViewPager / ScrollView）拦截
                        if (currentInteractionState.isTooltipActive) {
                            changes.forEach { it.consume() }
                        }

                        // 3.4 根据状态机执行逻辑
                        if (currentInteractionState.isTooltipActive) {
                            // 【状态 A：扫掠 Tooltip 锁定激活】
                            val pointer = changes.firstOrNull { it.pressed }
                            if (pointer != null) {
                                val currentVal = if (!currentHorizontal) {
                                    currentMapper.toDataX(pointer.position.x)
                                } else {
                                    currentMapper.toDataY(pointer.position.y)
                                }
                                currentInteractionState.tooltipDataX = currentVal
                                currentInteractionState.tooltipScreenOffset = pointer.position
                            }
                        } else {
                            // 【状态 B：常规缩放与平移】
                            val pressedCount = changes.count { it.pressed }
                            val isCategoryDrag = if (!currentHorizontal) dragDirection == 1 else dragDirection == 2
                            
                            if (pressedCount == 1) {
                                // 单指滑动
                                if (isCategoryDrag) {
                                    // 确立为类目轴滑动时，消费所有事件变化，防止父滚动视图拦截当前拖动流
                                    changes.forEach { it.consume() }
                                    
                                    val pointer = changes.first { it.pressed }
                                    if (currentViewportState.enablePan && pointer.previousPressed) {
                                        val delta = if (!currentHorizontal) {
                                            currentMapper.toDataX(pointer.position.x) - currentMapper.toDataX(pointer.previousPosition.x)
                                        } else {
                                            currentMapper.toDataY(pointer.position.y) - currentMapper.toDataY(pointer.previousPosition.y)
                                        }
                                        // 拖拽平移视口：手指反向平移
                                        currentViewportState.pan(-delta)
                                    }
                                }
                            } else if (pressedCount >= 2) {
                                // 双指缩放，同样消费所有事件防止被父组件拦截
                                changes.forEach { it.consume() }
                                
                                val activePointers = changes.filter { it.pressed }
                                val p1 = activePointers[0]
                                val p2 = activePointers[1]

                                if (currentViewportState.enableZoom && p1.previousPressed && p2.previousPressed) {
                                    val currDist = (p1.position - p2.position).getDistance()
                                    val prevDist = (p1.previousPosition - p2.previousPosition).getDistance()

                                    if (prevDist > 0f && currDist > 0f) {
                                        val scale = currDist / prevDist
                                        val centroid = (p1.position + p2.position) / 2f
                                        val focus = if (!currentHorizontal) {
                                            currentMapper.toDataX(centroid.x)
                                        } else {
                                            currentMapper.toDataY(centroid.y)
                                        }
                                        currentViewportState.zoom(scale, focus)
                                    }
                                }
                            }
                        }

                        if (activeChange != null) {
                            prevPosition = activeChange.position
                        }
                    }
                }
            }
        }
    }
}
