package io.github.composechart.charts.scatter

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 散点图整体数据结构
 */
data class ScatterChartData(
    val series: List<ScatterSeries>,
    // 可选类目标签：若提供，则 X 轴为类目轴（点的 X 值代表索引）；若为 null，则 X 轴为数值轴（点使用真实的 X 坐标）
    val xLabels: List<String>? = null
)

/**
 * 散点系列属性配置
 */
data class ScatterSeries(
    val name: String,
    val points: List<ScatterPoint>,
    val color: Color,
    // 默认散点形状
    val symbol: ScatterSymbolType = ScatterSymbolType.Circle,
    // 默认散点半径/大小
    val symbolSize: Dp = 8.dp,
    // 边框描边
    val strokeWidth: Dp = 1.dp,
    val strokeColor: Color? = null,
    // 涟漪特效配置
    val effectScatter: Boolean = false,
    val effectOptions: EffectOptions = EffectOptions()
)

/**
 * 数据点
 */
data class ScatterPoint(
    val x: Float,           // 数据点 X 轴坐标（数值轴：实际数值；类目轴：类目索引值）
    val y: Float,           // 数据点 Y 轴坐标
    val value: Float? = null, // 数据点第三维度数值（例如：气泡图的大小、颜色强弱等）
    val name: String? = null  // 数据点独立标示名称，可用于 Tooltip 展示
)

/**
 * 散点图形类型
 */
enum class ScatterSymbolType {
    Circle,   // 圆形
    Square,   // 正方形
    Triangle, // 三角形
    Diamond   // 菱形
}

/**
 * 视觉映射（VisualMap）组件，支持根据数据点 value (第三维度) 映射为散点的大小、颜色渐变和透明度
 */
data class ScatterVisualMap(
    val min: Float,                  // 源数据映射的最小值限制
    val max: Float,                  // 源数据映射的最大值限制
    val minSize: Dp = 6.dp,          // 映射后散点的最小大小
    val maxSize: Dp = 36.dp,         // 映射后散点的最大大小
    val colorRange: List<Color> = emptyList(), // 颜色映射渐变，如 [绿色, 黄色, 红色]，若为空则使用系列自身的 color
    val alphaRange: Pair<Float, Float>? = null // 透明度映射区间，如 0.3f 到 1.0f，若为空则不映射透明度
)

/**
 * 涟漪动画特效配置
 */
data class EffectOptions(
    val rippleRadiusRatio: Float = 2.2f, // 涟漪外扩最大半径与原散点半径的倍数
    val rippleCount: Int = 2,           // 涟漪圈数
    val durationMs: Int = 1800          // 涟漪扩散一周期的毫秒数
)
