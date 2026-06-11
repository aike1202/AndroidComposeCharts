package io.github.composechart.charts.calendar

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 极简日历图每日数据
 */
data class CalendarDayData(
    val date: String,             // 日期，格式为 "yyyy-MM-dd"
    val value: Float,             // 当前数值，用于热力图着色或数据映射
    val label: String? = null,    // 附加标注文本（如农历“初一”或特定图标字符）
    val tooltip: String? = null   // 自定义 Tooltip 显示文本，若为空则默认显示日期与数值
)

/**
 * 日历图数据承载类
 */
data class CalendarChartData(
    val year: Int,                     // 目标年份（如 2017）
    val days: List<CalendarDayData>    // 每日数据列表
)

/**
 * 排布方向枚举
 */
enum class CalendarOrientation {
    Horizontal, // 横向日历：行代表星期，列代表周数（约53列）
    Vertical    // 纵向日历：行代表周数，列代表星期（7列）
}

/**
 * 日历图配置选项
 */
data class CalendarOptions(
    val firstDayOfWeek: Int = 0,             // 每周第一天：0 代表周日，1 代表周一
    val orientation: CalendarOrientation = CalendarOrientation.Horizontal,
    val cellSize: Dp = 16.dp,                // 单元格方格大小
    val cellGap: Dp = 2.dp,                  // 单元格之间间隙
    val cellCornerRadius: Dp = 2.dp,         // 单元格圆角
    val emptyCellColor: Color? = null,       // 空置（无数据或跨年填充）单元格底色，默认自适应明暗
    
    // 视觉热力值映射色带配置（从最小热力值到最大热力值的渐变色）
    val visualMapColors: List<Color> = listOf(
        Color(0xFFE2E8F0), // 浅灰（基底无值）
        Color(0xFF91CC75), // 绿色
        Color(0xFFFAC858), // 黄色
        Color(0xFFEE6666)  // 红色
    ),
    val showVisualMap: Boolean = true,       // 是否在图表下方渲染视觉色阶对照图例
    
    val weekdayLabels: List<String>? = null, // 星期标题，如 ["日", "一", "二", "三", "四", "五", "六"]
    val monthLabels: List<String>? = null,   // 月份标题，如 ["一月", "二月", ..., "十二月"]
    
    // 字体样式或颜色
    val labelColor: Color? = null            // 标签文本文字颜色，默认自动根据明暗主题适配
) {
    companion object {
        val Default = CalendarOptions()
    }
}
