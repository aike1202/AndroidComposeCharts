package io.github.composechart.charts.calendar

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.size
import androidx.compose.ui.layout.onGloballyPositioned
import io.github.composechart.core.style.ChartStyle
import java.util.Calendar

/**
 * CalendarChart 日历热力图与坐标系组件
 */
@Composable
fun CalendarChart(
    data: CalendarChartData,
    modifier: Modifier = Modifier,
    style: ChartStyle = ChartStyle.Default,
    options: CalendarOptions = CalendarOptions.Default
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val isDark = style.backgroundColor != Color.Transparent && style.backgroundColor.red < 0.5f

    // 1. 启动时的淡入扫掠扫过动画
    var targetFraction by remember { mutableStateOf(0f) }
    LaunchedEffect(data) {
        targetFraction = 1f
    }
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing),
        label = "calendar_sweeping_load"
    )

    // 2. 交互手势与 Tooltip 选中态管理
    var selectedDay by remember { mutableStateOf<CalendarDayData?>(null) }
    var tapOffset by remember { mutableStateOf(Offset.Zero) }
    var boxWidth by remember { mutableStateOf(0f) }
    var boxHeight by remember { mutableStateOf(0f) }

    // 3. 计算日历网格的精确周数及 Dp 尺寸，以支持 scroll 控制
    val cellSize = options.cellSize
    val cellGap = options.cellGap

    val colCount = remember(data, options) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, data.year)
        cal.set(Calendar.MONTH, Calendar.JANUARY)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val jan1DayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
        val adjustedJan1 = (jan1DayOfWeek - options.firstDayOfWeek + 7) % 7
        val dayCount = cal.getActualMaximum(Calendar.DAY_OF_YEAR)
        (adjustedJan1 + (dayCount - 1)) / 7 + 1
    }

    val canvasWidth = remember(colCount, cellSize, cellGap, options.orientation) {
        if (options.orientation == CalendarOrientation.Horizontal) {
            32.dp + (cellSize + cellGap) * colCount + 16.dp
        } else {
            42.dp + (cellSize + cellGap) * 7 + 16.dp
        }
    }

    val canvasHeight = remember(colCount, cellSize, cellGap, options.orientation, options.showVisualMap) {
        val bottomMargin = if (options.showVisualMap) 50.dp else 16.dp
        if (options.orientation == CalendarOrientation.Horizontal) {
            24.dp + (cellSize + cellGap) * 7 + bottomMargin
        } else {
            24.dp + (cellSize + cellGap) * colCount + bottomMargin
        }
    }

    // 实例化一个临时的渲染器以获取布局大小并执行点击判定
    val renderer = remember(data, options, animatedFraction, style, isDark) {
        CalendarChartRenderer(
            data = data,
            options = options,
            animatedFraction = animatedFraction,
            style = style,
            textMeasurer = textMeasurer,
            isDark = isDark
        )
    }

    val scrollState = rememberScrollState()
    val scrollModifier = if (options.orientation == CalendarOrientation.Horizontal) {
        Modifier.horizontalScroll(scrollState)
    } else {
        Modifier.verticalScroll(scrollState)
    }

    Column(
        modifier = modifier
            .background(style.backgroundColor)
            .padding(8.dp)
    ) {
        // ================= 1. 渲染标题与副标题 =================
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

        // ================= 2. 渲染核心 Canvas 网格与交互浮层 =================
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .then(scrollModifier),
            contentAlignment = Alignment.TopStart
        ) {
            Box(
                modifier = Modifier
                    .size(canvasWidth, canvasHeight)
                    .onGloballyPositioned { coordinates ->
                        boxWidth = coordinates.size.width.toFloat()
                        boxHeight = coordinates.size.height.toFloat()
                    }
                    .pointerInput(data, options) {
                        detectTapGestures(
                            onTap = { offset ->
                                // 在渲染器中寻找包含此点击坐标的单元格
                                val match = renderer.cellRects.find { it.second.contains(offset) }
                                if (match != null) {
                                    selectedDay = match.first
                                    tapOffset = offset
                                } else {
                                    selectedDay = null
                                }
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    renderer.draw(this)
                }

                // ================= 3. 悬浮浮动 Tooltip =================
                selectedDay?.let { day ->
                    val tooltipW = with(density) { 120.dp.toPx() }
                    val tooltipH = with(density) { 54.dp.toPx() }
                    
                    Box(
                        modifier = Modifier
                            .offset {
                                // 自适应居中偏上显示，防止跑出视图边界
                                val x = (tapOffset.x - tooltipW / 2f)
                                    .coerceIn(4.dp.toPx(), boxWidth - tooltipW - 4.dp.toPx())
                                val y = (tapOffset.y - tooltipH - 8.dp.toPx())
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
                                text = day.date,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = day.tooltip ?: "数值: ${String.format("%.1f", day.value).removeSuffix(".0")}",
                                fontSize = 9.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                            )
                        }
                    }
                }
            }
        }
    }
}
