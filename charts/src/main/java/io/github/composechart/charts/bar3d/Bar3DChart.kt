package io.github.composechart.charts.bar3d

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.composechart.core.style.ChartStyle

/**
 * 3D 柱状图（打卡图/热力柱）组件
 * 支持单指滑动控制偏航角（Yaw）和俯仰角（Pitch）旋转图表，双指捏合进行缩放 (Zoom)
 * 点击柱体自动计算 2D 命中展示打卡详情 Tooltip
 */
@Composable
fun Bar3DChart(
    data: Bar3DChartData,
    modifier: Modifier = Modifier,
    style: ChartStyle = ChartStyle.Default,
    options: Bar3DOptions = Bar3DOptions()
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val isDark = style.backgroundColor != Color.Transparent && style.backgroundColor.red < 0.5f

    // 水平偏航角和垂直俯仰角以及缩放状态
    var yaw by remember { mutableFloatStateOf(options.initialYaw) }
    var pitch by remember { mutableFloatStateOf(options.initialPitch) }
    var zoom by remember { mutableFloatStateOf(options.initialZoom) }

    // 交互点击状态
    var selectedPoint by remember { mutableStateOf<Bar3DPoint?>(null) }
    var tapOffset by remember { mutableStateOf(Offset.Zero) }

    // 记录布局物理宽高以防 Tooltip 弹出时溢出屏幕
    var boxWidth by remember { mutableStateOf(0f) }
    var boxHeight by remember { mutableStateOf(0f) }

    // 实例化当前视角的 3D 渲染器
    val renderer = remember(data, options, yaw, pitch, zoom, selectedPoint, style, isDark) {
        Bar3DChartRenderer(
            data = data,
            options = options,
            yaw = yaw,
            pitch = pitch,
            style = style,
            textMeasurer = textMeasurer,
            isDark = isDark,
            zoom = zoom,
            selectedPoint = selectedPoint
        )
    }

    Column(
        modifier = modifier
            .background(style.backgroundColor)
            .padding(8.dp)
    ) {
        // ================= 1. 标题区 =================
        if (style.titleOptions.show && style.titleOptions.text.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalAlignment = when (style.titleOptions.alignment) {
                    Alignment.CenterHorizontally -> Alignment.CenterHorizontally
                    Alignment.End -> Alignment.End
                    else -> Alignment.Start
                }
            ) {
                Text(
                    text = style.titleOptions.text,
                    style = style.titleOptions.textStyle
                )
                if (style.titleOptions.subtext.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(style.titleOptions.itemGap))
                    Text(
                        text = style.titleOptions.subtext,
                        style = style.titleOptions.subtextStyle
                    )
                }
            }
        }

        // ================= 2. 核心 3D 渲染 Canvas =================
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onGloballyPositioned { coords ->
                    boxWidth = coords.size.width.toFloat()
                    boxHeight = coords.size.height.toFloat()
                }
                // 双手势处理：同时挂载缩放/平移/旋转手势和轻触手势
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoomAmount, rotation ->
                        // 绕 Z 轴偏航旋转
                        yaw += pan.x * 0.4f
                        // 绕 X 轴俯仰旋转（限制在 12..80 度，避免翻转产生反面绘制面混乱）
                        pitch = (pitch - pan.y * 0.4f).coerceIn(12f, 80f)
                        // 双指缩放因子
                        zoom = (zoom * zoomAmount).coerceIn(0.5f, 2.5f)
                        // 滑动旋转中隐藏 Tooltip，保持流畅重绘
                        selectedPoint = null
                    }
                }
                .pointerInput(data, options, yaw, pitch, zoom) {
                    detectTapGestures(
                        onTap = { offset ->
                            // 辅助算法：计算点 P 到线段 AB 的最短 2D 屏幕距离
                            fun getDistanceToSegment(p: Offset, a: Offset, b: Offset): Float {
                                val ab = b - a
                                val ap = p - a
                                val abLenSq = ab.x * ab.x + ab.y * ab.y
                                if (abLenSq < 1f) return (p - a).getDistance()
                                val t = ((ap.x * ab.x + ap.y * ab.y) / abLenSq).coerceIn(0f, 1f)
                                val closest = a + ab * t
                                return (p - closest).getDistance()
                            }

                            var closestPt: Bar3DPoint? = null
                            var foundTopCenter: Offset? = null

                            // 反向遍历 (即从最近的柱子往最远的柱子遍历，优先捕捉最上层未遮挡的点击)
                            for (i in renderer.projectedBars.indices.reversed()) {
                                val barInfo = renderer.projectedBars[i]
                                val dist = getDistanceToSegment(offset, barInfo.bottomCenter, barInfo.topCenter)
                                
                                // 判定阈值：柱子投影粗细的 1.4 倍，或者是保底的点击防空阈值 (18.dp)
                                val tolerance = (barInfo.radiusPx * 1.4f).coerceAtLeast(with(density) { 18.dp.toPx() })
                                if (dist < tolerance) {
                                    closestPt = barInfo.pt
                                    foundTopCenter = barInfo.topCenter
                                    break
                                }
                            }

                            if (closestPt != null) {
                                selectedPoint = closestPt
                                tapOffset = foundTopCenter ?: offset
                            } else {
                                selectedPoint = null
                            }
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                renderer.draw(this)
            }

            // ================= 3. 高保真 Tooltip 弹窗 =================
            selectedPoint?.let { pt ->
                val tooltipW = with(density) { 140.dp.toPx() }
                val tooltipH = with(density) { 56.dp.toPx() }

                Box(
                    modifier = Modifier
                        .offset {
                            // 自适应避让视界框边缘，保证不被截断
                            val x = (tapOffset.x - tooltipW / 2f)
                                .coerceIn(4.dp.toPx(), boxWidth - tooltipW - 4.dp.toPx())
                            val y = (tapOffset.y - tooltipH - 12.dp.toPx())
                                .coerceIn(4.dp.toPx(), boxHeight - tooltipH - 4.dp.toPx())
                            IntOffset(x.toInt(), y.toInt())
                        }
                        .background(
                            color = if (isDark) Color(0xFF1E293B) else Color.White,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column {
                        Text(
                            text = "${data.yAxisLabels.getOrNull(pt.yIndex) ?: ""} (${data.xAxisLabels.getOrNull(pt.xIndex) ?: ""})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "打卡值: ${pt.zValue.toInt()}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = renderer.getColorForValue(pt.zValue)
                        )
                    }
                }
            }
        }
    }
}
