package io.github.composechart.charts.gauge

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 仪表盘数据承载
 */
data class GaugeChartData(
    val value: Float,           // 当前数值
    val min: Float = 0f,        // 刻度盘最小值
    val max: Float = 100f,      // 刻度盘最大值
    val name: String = "",      // 指标名称（如："健康状态"）
    val unit: String = ""       // 数值单位（如："%"、"km/h"）
)

/**
 * 仪表盘视觉选项配置
 */
data class GaugeOptions(
    // 圆弧起止角度（0度为正右侧，顺时针为正。135f 即左下方起始，270f 跨度到右下方 45f）
    val startAngle: Float = 135f,
    val sweepAngle: Float = 270f,
    
    // 圆环底弧粗细
    val axisLineWidth: Dp = 12.dp,
    // 圆环分段阈值配置。Float 取值范围在 0.0f..1.0f 之间，如 0.3f 代表前 30% 区间
    val axisLineColors: List<Pair<Float, Color>> = listOf(
        0.3f to Color(0xFF91CC75), // 绿色
        0.7f to Color(0xFFFAC858), // 黄色
        1.0f to Color(0xFFEE6666)  // 红色
    ),
    val axisBgColor: Color = Color.LightGray.copy(alpha = 0.25f), // 进度色环背景底色

    // 刻度短线
    val showTicks: Boolean = true,
    val tickCount: Int = 10,                 // 主刻度大区间等分数
    val tickLength: Dp = 8.dp,
    val tickWidth: Dp = 1.5.dp,
    val tickColor: Color = Color.Gray.copy(alpha = 0.6f),
    
    // 子刻度短线（小等分线）
    val subTickCount: Int = 5,               // 主刻度之间细分小区间数
    val subTickLength: Dp = 4.dp,
    val subTickWidth: Dp = 0.8.dp,
    val subTickColor: Color = Color.Gray.copy(alpha = 0.4f),

    // 刻度数字
    val showTickLabels: Boolean = true,
    val tickLabelTextStyle: TextStyle? = null, // 若为空则内部自适应计算

    // 指针配置
    val pointerWidth: Dp = 6.dp,             // 三角底座半宽或宽的映射
    val pointerLengthRatio: Float = 0.62f,    // 指针长度占半径的比例
    val pointerColor: Color? = null,         // 指针填充颜色，若为空则自动随当前值对应的区间进度色，更灵动

    // 圆心轴承圆圈装饰
    val centerCircleRadius: Dp = 10.dp,
    val centerCircleColor: Color? = null,     // 轴承核心装饰圈颜色

    // 数值与标签排版
    val valueTextStyle: TextStyle? = null,
    val nameTextStyle: TextStyle? = null,
    val valueAboveName: Boolean = true,      // 数值是否在指标名称的上方 (如果为 false，则指标名称在上方)
    val customLabels: List<String>? = null,   // 自定义刻度标签字符列表 (若非空则对应显示在各个大刻度处)
    val pointerType: PointerType = PointerType.Triangle, // 指针样式（常规长角或贴附内轨的小三角）
    val showPointer: Boolean = true,          // 是否显示指针
    val isProgress: Boolean = false          // 是否为进度仪表盘模式 (彩色进度弧只绘制到当前数值占比处)
) {
    companion object {
        val Default = GaugeOptions()
    }
}

/**
 * 仪表盘指针类型
 */
enum class PointerType {
    Triangle,      // 常规从圆心延伸向外的长尖角指针
    ShortTriangle  // 贴附在刻度轨道内侧的悬空短三角形指针
}
