package io.github.composechart.charts.bar

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * 柱状图/条形图整体数据结构
 */
data class BarChartData(
    val xLabels: List<String>,
    val series: List<BarSeries>
)

/**
 * 柱状图系列配置参数
 */
data class BarSeries(
    val name: String,
    val values: List<BarValue>,
    val color: Color,
    val gradientBrush: Brush? = null, // 优先级高于 color
    val cornerRadius: CornerRadius = CornerRadius(4f, 4f), // 顶部圆角半径 (物理像素)
    // 柱体间距与宽度比例
    val barWidthRatio: Float = 0.6f, // 占类目区间宽度的比例 (0.0 - 1.0)
    val barGapRatio: Float = 0.2f, // 多系列并列柱子之间的间隔比例
    // 背景柱槽
    val showBackground: Boolean = false,
    val backgroundColor: Color = Color.LightGray.copy(alpha = 0.2f),
    val yAxisIndex: Int = 0,
    val stack: String? = null, // 堆叠分组标识。若不为空，相同分组的柱体纵向累加绘制。
    // 阴影/发光效果配置
    val shadowColor: Color? = null,
    val shadowBlur: Float = 0f,
    val shadowOffset: Offset = Offset.Zero
)

/**
 * 柱状图单个元素的值
 */
data class BarValue(
    val value: Float,
    val baseValue: Float? = null // 若不为 null，柱子绘制在 [baseValue, value] 区间；若为 null，从 0 开始
)
