package io.github.composechart.charts.funnel

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 漏斗图数据承载
 */
data class FunnelChartData(
    val slices: List<FunnelSlice>
)

/**
 * 漏斗图各阶段块数据
 */
data class FunnelSlice(
    val name: String,         // 阶段名称 (例如 "展现", "点击", "生成订单")
    val value: Float,         // 数值
    val color: Color? = null  // 当前块填充色，若为 null 则使用全局调色板自动分配
)

/**
 * 漏斗对齐方式
 */
enum class FunnelAlign {
    Left,   // 靠左对齐 (梯形左侧垂直，右侧倾斜)
    Center, // 居中对称 (梯形双侧倾斜，经典漏斗)
    Right   // 靠右对齐 (梯形右侧垂直，左侧倾斜)
}

/**
 * 数据排序规则
 */
enum class FunnelSort {
    Descending, // 降序排列（经典漏斗形）
    Ascending,  // 升序排列（金字塔形）
    None        // 不排序，保持原始输入顺序
}

/**
 * 漏斗图细节视觉配置
 */
data class FunnelOptions(
    val align: FunnelAlign = FunnelAlign.Center,
    val sort: FunnelSort = FunnelSort.Descending,
    
    // 梯形层级垂直间隔
    val gap: Dp = 4.dp,
    // 顶点倒圆角大小
    val cornerRadius: Dp = 4.dp,
    // 最窄部分的最小宽度占比 (0f-1f)，防止底部过窄不可见
    val minWidthRatio: Float = 0.08f,
    // 最大部分的最大宽度占可用屏幕宽度的比例 (0f-1f)
    val maxWidthRatio: Float = 0.85f,

    // 是否在两侧引线显示阶段名称与数值
    val showLabels: Boolean = true,
    val labelTextStyle: TextStyle? = null,

    // 是否在交界空隙处绘制流失转化率
    val showConversion: Boolean = true,
    val conversionTextStyle: TextStyle? = null
) {
    companion object {
        val Default = FunnelOptions()
    }
}
