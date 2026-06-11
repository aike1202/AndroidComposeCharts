package io.github.composechart.charts.line

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 折线图整体数据结构
 */
data class LineChartData(
    val xLabels: List<String>,
    val series: List<LineSeries>
)

/**
 * 折线系列属性配置
 */
data class LineSeries(
    val name: String,
    val points: List<LinePoint>,
    val color: Color,
    val isSmooth: Boolean = false,
    val lineWidth: Dp = 2.dp,
    val lineStyle: LineStyleType = LineStyleType.Solid,
    // 面积图配置
    val drawArea: Boolean = false,
    val areaBrush: Brush? = null,
    // 数据折点样式
    val symbol: SymbolType = SymbolType.Circle,
    val symbolSize: Dp = 4.dp,
    val symbolColor: Color? = null,
    // 坐标轴绑定与堆叠
    val yAxisIndex: Int = 0, // 0: 左轴, 1: 右轴
    val xAxisIndex: Int = 0, // 0: 底部 X 轴, 1: 顶部 X2 轴
    val showEndLabel: Boolean = false, // 是否在折线末梢贴附展示系列名称和值
    val showSymbolLabel: Boolean = false, // 是否在折线拐点内置展示数值（排名）
    val stack: String? = null, // 堆叠分组标识
    // ECharts 高级特性参数
    val stepType: StepType = StepType.None,
    val connectNulls: Boolean = false,
    val markPoints: List<MarkPoint> = emptyList(),
    val markLines: List<MarkLine> = emptyList()
)

/**
 * 数据点
 */
data class LinePoint(
    val x: Float, // 数据点 X 值 (对应类目索引)
    val y: Float?  // 数据点 Y 值 (为空代表数据缺失)
)

enum class StepType {
    None,   // 常规折线
    Start,  // 阶梯从起点拐弯
    Middle, // 阶梯从中间拐弯
    End     // 阶梯从终点拐弯
}

enum class LineStyleType {
    Solid,
    Dashed,
    Dotted
}

enum class SymbolType {
    Circle,
    Square,
    Diamond,
    None
}

data class MarkPoint(
    val type: MarkPointType,
    val label: String? = null
)

enum class MarkPointType {
    Max, Min, Custom
}

data class MarkLine(
    val type: MarkLineType,
    val value: Float? = null,
    val label: String? = null
)

enum class MarkLineType {
    Average, Constant
}

data class VisualMapRange(
    val min: Float,
    val max: Float,
    val color: Color
)

data class RangeAreaOptions(
    val upperSeriesIndex: Int,
    val lowerSeriesIndex: Int,
    val fillColor: Color
)

/**
 * 警戒色带标注区域配置
 */
data class MarkArea(
    val startX: Float? = null,
    val endX: Float? = null,
    val startY: Float? = null,
    val endY: Float? = null,
    val color: Color,
    val label: String? = null,
    val labelColor: Color = Color(0xFF888888)
)

/**
 * 获取可见范围数据点的首尾索引值（支持数值轴与类目轴）
 */
fun List<LinePoint>.getVisibleRange(leftX: Float, rightX: Float): Pair<Int, Int> {
    if (this.isEmpty()) return Pair(0, -1)
    val left = this.indexOfFirst { it.x >= leftX }
    if (left == -1) return Pair(this.size, this.size - 1)
    val right = this.indexOfLast { it.x <= rightX }
    if (right == -1) return Pair(0, -1)
    return Pair(left, right)
}

