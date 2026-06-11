package io.github.composechart.charts.gauge

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.composechart.core.style.ChartStyle
import io.github.composechart.core.style.TitleOptions

/**
 * 极简极炫环形进度与仪表盘组件 (GaugeChart)
 */
@Composable
fun GaugeChart(
    data: GaugeChartData,
    modifier: Modifier = Modifier,
    style: ChartStyle = ChartStyle.Default,
    options: GaugeOptions = GaugeOptions.Default
) {
    val textMeasurer = rememberTextMeasurer()
    val isDark = style.backgroundColor != Color.Transparent && style.backgroundColor.red < 0.5f

    // 1. 测算当前数值占整个刻度盘范围的比例 fraction
    val range = data.max - data.min
    val targetFraction = if (range > 0f) {
        ((data.value - data.min) / range).coerceIn(0f, 1f)
    } else {
        0.5f
    }

    // 2. 使用弹性物理 spring 动画让指针摆动拟真，伴有细微的回弹微颤效果
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy, // 偏弹性
            stiffness = Spring.StiffnessLow                // 偏柔和偏缓
        ),
        label = "gauge_needle_swing"
    )

    Column(
        modifier = modifier
            .background(style.backgroundColor)
            .padding(8.dp)
    ) {
        // ================= 1. 渲染主标题及副标题 =================
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
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(style.titleOptions.itemGap))
                    Text(
                        text = style.titleOptions.subtext,
                        style = style.titleOptions.subtextStyle
                    )
                }
            }
        }

        // ================= 2. 渲染核心 Canvas 图表主体与下方文字叠层 =================
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // 实例化渲染器并直绘 Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val renderer = GaugeChartRenderer(
                    data = data,
                    options = options,
                    animatedFraction = animatedFraction,
                    style = style,
                    textMeasurer = textMeasurer,
                    isDark = isDark
                )

                // 2.1 绘制圆环、刻度与旋转三角形指针
                renderer.draw(this)

                // 2.2 绘制正下方的数值与指标名称
                val widthPx = size.width
                val heightPx = size.height
                val center = Offset(widthPx / 2f, heightPx * 0.48f)
                val radius = minOf(widthPx, heightPx) * 0.42f

                // 动态获取指针当前占比对应的色彩作为大字颜色，使视觉更为联动一体
                val currentColor = renderer.getCurrentColor(animatedFraction)

                // 测量大字（当前数值 + 单位）
                val valStr = String.format("%.1f", data.value).removeSuffix(".0") + data.unit
                val valTextStyle = options.valueTextStyle ?: androidx.compose.ui.text.TextStyle(
                    color = currentColor,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                val valLayout = textMeasurer.measure(valStr, style = valTextStyle)

                // 测量小字（指标说明）
                val nameTextStyle = options.nameTextStyle ?: androidx.compose.ui.text.TextStyle(
                    color = if (isDark) Color(0xFF8E8E93) else Color(0xFF666666),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
                val nameLayout = textMeasurer.measure(data.name, style = nameTextStyle)

                // 放置在圆心正下方空隙
                val firstY = center.y + radius * 0.3f
                if (options.valueAboveName) {
                    val valY = firstY
                    val nameY = valY + valLayout.size.height + 4.dp.toPx()

                    drawText(
                        textLayoutResult = valLayout,
                        topLeft = Offset(center.x - valLayout.size.width / 2f, valY)
                    )
                    if (data.name.isNotEmpty()) {
                        drawText(
                            textLayoutResult = nameLayout,
                            topLeft = Offset(center.x - nameLayout.size.width / 2f, nameY)
                        )
                    }
                } else {
                    val nameY = firstY
                    val valY = nameY + nameLayout.size.height + 4.dp.toPx()

                    if (data.name.isNotEmpty()) {
                        drawText(
                            textLayoutResult = nameLayout,
                            topLeft = Offset(center.x - nameLayout.size.width / 2f, nameY)
                        )
                    }
                    drawText(
                        textLayoutResult = valLayout,
                        topLeft = Offset(center.x - valLayout.size.width / 2f, valY)
                    )
                }
            }
        }
    }
}

