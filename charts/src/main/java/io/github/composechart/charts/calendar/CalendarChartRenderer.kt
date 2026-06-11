package io.github.composechart.charts.calendar

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 日历图渲染引擎，计算星期与月份坐标，并执行网格和标签绘制
 */
class CalendarChartRenderer(
    val data: CalendarChartData,
    val options: CalendarOptions,
    val animatedFraction: Float,
    val style: ChartStyle,
    val textMeasurer: TextMeasurer,
    val isDark: Boolean
) {
    // 单元格的位置缓存，用于交互检测
    val cellRects = mutableListOf<Pair<CalendarDayData, Rect>>()

    // 数据范围，用于归一化上色
    private var minVal = 0f
    private var maxVal = 100f
    private val dayCount: Int
    private val adjustedJan1: Int
    private val daysList = mutableListOf<DayInfo>()

    init {
        // 1. 获取该年份的天数与 1 月 1 日的星期几
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, data.year)
        cal.set(Calendar.MONTH, Calendar.JANUARY)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        
        // java.util.Calendar.DAY_OF_WEEK 范围 1 (Sunday) 到 7 (Saturday)
        // 转换为 0 (Sunday) 到 6 (Saturday)
        val jan1DayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
        
        // 调整 1 月 1 日对应的星期索引（基于 options.firstDayOfWeek）
        adjustedJan1 = (jan1DayOfWeek - options.firstDayOfWeek + 7) % 7
        
        // 获取整年的总天数
        dayCount = cal.getActualMaximum(Calendar.DAY_OF_YEAR)

        // 2. 计算最大最小值
        if (data.days.isNotEmpty()) {
            minVal = data.days.minOf { it.value }
            maxVal = data.days.maxOf { it.value }
            if (maxVal == minVal) {
                maxVal = minVal + 1f
            }
        }

        // 3. 构建整年 365/366 天的逻辑坐标
        val dayDataMap = data.days.associateBy { it.date }
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        val loopCal = cal.clone() as Calendar
        for (d in 1..dayCount) {
            loopCal.set(Calendar.DAY_OF_YEAR, d)
            val dateStr = sdf.format(loopCal.time)
            
            // 物理星期，0 为周日，6 为周六
            val physicalWd = loopCal.get(Calendar.DAY_OF_WEEK) - 1
            // 逻辑星期索引（基于 firstDayOfWeek 偏移）
            val logicalWd = (physicalWd - options.firstDayOfWeek + 7) % 7
            // 计算逻辑周数（列索引）
            val logicalWeek = (adjustedJan1 + (d - 1)) / 7
            
            val month = loopCal.get(Calendar.MONTH)
            val dayOfMonth = loopCal.get(Calendar.DAY_OF_MONTH)
            
            val matchingData = dayDataMap[dateStr] ?: CalendarDayData(dateStr, 0f)
            
            daysList.add(DayInfo(
                dayOfYear = d,
                dateStr = dateStr,
                month = month,
                dayOfMonth = dayOfMonth,
                logicalWd = logicalWd,
                logicalWeek = logicalWeek,
                data = matchingData
            ))
        }
    }

    /**
     * 计算数据对应的颜色插值
     */
    fun getColorForValue(value: Float): Color {
        val colors = options.visualMapColors
        if (colors.isEmpty()) return Color.Gray
        if (colors.size == 1) return colors.first()
        
        val fraction = ((value - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)
        val segments = colors.size - 1
        val scaled = fraction * segments
        val idx = scaled.toInt().coerceIn(0, segments - 1)
        val localF = scaled - idx
        
        return lerp(colors[idx], colors[idx + 1], localF)
    }

    /**
     * 绘制整个日历与附属图层
     */
    fun draw(drawScope: DrawScope) = with(drawScope) {
        cellRects.clear()
        
        val density = drawScope.density
        val widthPx = size.width
        val heightPx = size.height
        
        val cellSizePx = options.cellSize.toPx()
        val cellGapPx = options.cellGap.toPx()
        
        val maxCol = daysList.maxOf { it.logicalWeek }
        val colCount = maxCol + 1

        val gridW = colCount * (cellSizePx + cellGapPx) - cellGapPx
        val gridH = 7 * (cellSizePx + cellGapPx) - cellGapPx

        // 确定网格左上角偏移
        val labelColor = options.labelColor ?: if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155)
        val textStyle = TextStyle(color = labelColor, fontSize = 9.sp)
        
        val gridLeft: Float
        val gridTop: Float
        
        if (options.orientation == CalendarOrientation.Horizontal) {
            gridLeft = 32.dp.toPx()
            gridTop = 24.dp.toPx()
        } else {
            gridLeft = 42.dp.toPx()
            gridTop = 24.dp.toPx()
        }

        val emptyColor = options.emptyCellColor ?: if (isDark) Color(0xFF27272A) else Color(0xFFF1F3F5)

        // ================= 1. 绘制方格单元格 (Cells) =================
        for (i in daysList.indices) {
            val dayInfo = daysList[i]
            
            // 单元格网格行列定位
            val col = dayInfo.logicalWeek
            val row = dayInfo.logicalWd
            
            val cX: Float
            val cY: Float
            
            if (options.orientation == CalendarOrientation.Horizontal) {
                cX = gridLeft + col * (cellSizePx + cellGapPx)
                cY = gridTop + row * (cellSizePx + cellGapPx)
            } else {
                cX = gridLeft + row * (cellSizePx + cellGapPx)
                cY = gridTop + col * (cellSizePx + cellGapPx)
            }

            val rect = Rect(cX, cY, cX + cellSizePx, cY + cellSizePx)
            cellRects.add(dayInfo.data to rect)

            // 计算该方格的淡入/缩放动画逻辑（波浪向后延迟展现）
            val cellStart = (i.toFloat() / dayCount) * 0.7f
            val cellAnim = ((animatedFraction - cellStart) / 0.3f).coerceIn(0f, 1f)
            
            if (cellAnim > 0f) {
                val scale = 0.5f + 0.5f * cellAnim
                val color = if (dayInfo.data.value > 0f || dayDataMapExists(dayInfo.dateStr)) {
                    getColorForValue(dayInfo.data.value)
                } else {
                    emptyColor
                }
                
                // 绘制带缩放与透明度的圆角方格
                val cellCenter = rect.center
                val scaledW = rect.width * scale
                val scaledH = rect.height * scale
                val scaledRect = Rect(
                    cellCenter.x - scaledW / 2f,
                    cellCenter.y - scaledH / 2f,
                    cellCenter.x + scaledW / 2f,
                    cellCenter.y + scaledH / 2f
                )
                
                drawRoundRect(
                    color = color.copy(alpha = cellAnim),
                    topLeft = scaledRect.topLeft,
                    size = scaledRect.size,
                    cornerRadius = CornerRadius(options.cellCornerRadius.toPx()),
                    style = Fill
                )
                
                // 绘制单元格文本标签（如有自定义农历/春节等标注）
                if (dayInfo.data.label != null && cellAnim > 0.5f) {
                    val labelStyle = TextStyle(
                        color = if (isDark) Color.White else Color(0xFF1E293B),
                        fontSize = 8.sp
                    )
                    val labelLayout = textMeasurer.measure(dayInfo.data.label, style = labelStyle)
                    drawText(
                        textLayoutResult = labelLayout,
                        topLeft = Offset(
                            cellCenter.x - labelLayout.size.width / 2f,
                            cellCenter.y - labelLayout.size.height / 2f
                        )
                    )
                }
            }
        }

        // ================= 2. 绘制星期表头 (Weekday Labels) =================
        val defaultWeekdays = if (options.firstDayOfWeek == 0) {
            listOf("日", "一", "二", "三", "四", "五", "六")
        } else {
            listOf("一", "二", "三", "四", "五", "六", "日")
        }
        val weekdays = options.weekdayLabels ?: defaultWeekdays

        for (row in 0..6) {
            val label = weekdays.getOrNull(row) ?: ""
            if (label.isEmpty()) continue
            
            val labelLayout = textMeasurer.measure(label, style = textStyle)
            val lX: Float
            val lY: Float
            
            if (options.orientation == CalendarOrientation.Horizontal) {
                lX = gridLeft - labelLayout.size.width - 6.dp.toPx()
                lY = gridTop + row * (cellSizePx + cellGapPx) + cellSizePx / 2f - labelLayout.size.height / 2f
            } else {
                lX = gridLeft + row * (cellSizePx + cellGapPx) + cellSizePx / 2f - labelLayout.size.width / 2f
                lY = gridTop - labelLayout.size.height - 4.dp.toPx()
            }
            
            drawText(textLayoutResult = labelLayout, topLeft = Offset(lX, lY))
        }

        // ================= 3. 绘制月份表头 (Month Labels) =================
        val defaultMonths = listOf("一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月")
        val months = options.monthLabels ?: defaultMonths

        val monthGroup = daysList.groupBy { it.month }
        for (m in 0..11) {
            val label = months.getOrNull(m) ?: ""
            if (label.isEmpty()) continue
            
            val daysInMonth = monthGroup[m] ?: continue
            val startWeek = daysInMonth.minOf { it.logicalWeek }
            val endWeek = daysInMonth.maxOf { it.logicalWeek }
            val midWeek = (startWeek + endWeek) / 2f
            
            val labelLayout = textMeasurer.measure(label, style = textStyle)
            val lX: Float
            val lY: Float
            
            if (options.orientation == CalendarOrientation.Horizontal) {
                lX = gridLeft + midWeek * (cellSizePx + cellGapPx) + cellSizePx / 2f - labelLayout.size.width / 2f
                lY = gridTop - labelLayout.size.height - 4.dp.toPx()
            } else {
                lX = gridLeft - labelLayout.size.width - 6.dp.toPx()
                lY = gridTop + midWeek * (cellSizePx + cellGapPx) + cellSizePx / 2f - labelLayout.size.height / 2f
            }
            
            drawText(textLayoutResult = labelLayout, topLeft = Offset(lX, lY))
        }

        // ================= 4. 绘制底部色卡指示器 (Visual Map Legend) =================
        if (options.showVisualMap && options.visualMapColors.isNotEmpty()) {
            val legendW = 140.dp.toPx()
            val legendH = 8.dp.toPx()
            
            val legendX = widthPx / 2f - legendW / 2f
            val legendY = heightPx - 20.dp.toPx()

            // 绘制渐变色卡
            val brush = androidx.compose.ui.graphics.Brush.horizontalGradient(options.visualMapColors)
            drawRoundRect(
                brush = brush,
                topLeft = Offset(legendX, legendY),
                size = Size(legendW, legendH),
                cornerRadius = CornerRadius(4.dp.toPx()),
                style = Fill
            )

            // 绘制色卡两端文字
            val minText = String.format("%.0f", minVal)
            val maxText = String.format("%.0f", maxVal)
            
            val minLayout = textMeasurer.measure(minText, style = textStyle)
            val maxLayout = textMeasurer.measure(maxText, style = textStyle)
            
            drawText(
                textLayoutResult = minLayout,
                topLeft = Offset(
                    legendX - minLayout.size.width - 6.dp.toPx(),
                    legendY + legendH / 2f - minLayout.size.height / 2f
                )
            )
            drawText(
                textLayoutResult = maxLayout,
                topLeft = Offset(
                    legendX + legendW + 6.dp.toPx(),
                    legendY + legendH / 2f - maxLayout.size.height / 2f
                )
            )
        }
    }

    private fun dayDataMapExists(date: String): Boolean {
        return data.days.any { it.date == date }
    }
}

/**
 * 逻辑计算承载实体
 */
private data class DayInfo(
    val dayOfYear: Int,
    val dateStr: String,
    val month: Int,
    val dayOfMonth: Int,
    val logicalWd: Int,
    val logicalWeek: Int,
    val data: CalendarDayData
)
