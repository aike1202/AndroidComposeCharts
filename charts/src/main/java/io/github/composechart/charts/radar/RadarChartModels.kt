package io.github.composechart.charts.radar

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 雷达图整体数据源结构
 */
data class RadarChartData(
    val indicators: List<RadarIndicator>,
    val series: List<RadarSeries>
)

/**
 * 雷达图单个射线维度的定义 (包含指标名称和该轴的最大/最小值限制)
 */
data class RadarIndicator(
    val name: String,
    val max: Float,
    val min: Float = 0f
)

/**
 * 雷达图覆盖数据系列配置 (支持多系列对比，填充色及描边设置)
 */
data class RadarSeries(
    val name: String,
    val values: List<Float>,       // 长度应与 indicators 保持一致
    val color: Color,
    val fillColor: Color? = null,  // 半透明填充色 (若为空则默认 color.copy(alpha = 0.2f))
    val strokeWidth: Dp = 2.dp
)
