package io.github.composechart.charts.boxplot

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 盒须图数据承载
 */
data class BoxplotChartData(
    val xLabels: List<String>,
    val series: List<BoxplotSeries>
)

/**
 * 盒须图系列属性配置
 */
data class BoxplotSeries(
    val name: String,
    val points: List<BoxplotPoint>,
    val color: Color,
    val fillColor: Color? = null,              // 箱体内部填充色，若为 null 则使用主色半透明淡化
    val boxWidthRatio: Float = 0.45f,          // 箱子宽度占网格间距的比例 (0f-1f)
    val whiskerWidthRatio: Float = 0.22f,      // 最大/最小水平短端盖宽度比例
    val outliers: List<BoxplotOutlier> = emptyList() // 属于该系列的异常值散点
)

/**
 * 盒须图五个核心统计特征值点 (Five-number Summary)
 */
data class BoxplotPoint(
    val min: Float,     // 最小值 (下边缘)
    val q1: Float,      // 第一四分位数/下四分位数
    val median: Float,  // 中位数
    val q3: Float,      // 第三四分位数/上四分位数
    val max: Float      // 最大值 (上边缘)
)

/**
 * 异常值数据点
 */
data class BoxplotOutlier(
    val xIndex: Int,    // 属于哪个类目索引的异常值
    val value: Float,   // 异常值的具体数值
    val name: String? = null // 提示框中显示的详细名字
)
