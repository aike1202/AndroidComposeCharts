package io.github.composechart.charts.polar

import androidx.compose.ui.graphics.Color

/**
 * 极坐标柱状图整体数据结构
 */
data class PolarChartData(
    val xLabels: List<String>,
    val series: List<PolarBarSeries>
)

/**
 * 极坐标系柱状图系列配置参数
 */
data class PolarBarSeries(
    val name: String,
    val values: List<PolarBarValue>,
    val color: Color,
    val labelPosition: PolarLabelPosition = PolarLabelPosition.Middle
)

/**
 * 极坐标柱状图单个元素的值
 */
data class PolarBarValue(
    val value: Float
)

/**
 * 极坐标柱体标签显示位置
 */
enum class PolarLabelPosition {
    Middle, // 标签居中显示
    End,    // 标签置于顶端/末端显示
    None    // 不显示标签
}
