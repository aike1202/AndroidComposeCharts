package io.github.composechart.charts.kline

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 金融 K 线蜡烛图数据源
 */
data class KLineChartData(
    val entries: List<KLineEntry>
)

/**
 * 单个 K 线周期的数据实体
 */
data class KLineEntry(
    val time: String,   // 时间/日期标签
    val open: Float,    // 开盘价
    val close: Float,   // 收盘价
    val high: Float,    // 最高价
    val low: Float,     // 最低价
    val volume: Float   // 成交量
)

/**
 * K 线视觉样式配置
 */
data class KLineStyle(
    val upColor: Color = Color(0xFFEE6666),       // 阳线颜色 (默认红)
    val downColor: Color = Color(0xFF3BA272),     // 阴线颜色 (默认绿)
    val upFilled: Boolean = false,                // 阳线是否填充实心 (国规默认空心)
    val downFilled: Boolean = true,               // 阴线是否填充实心 (国规默认实心)
    val candleWidthRatio: Float = 0.7f,           // 烛体占据区间的物理宽度比例
    val shadowLineWidth: Dp = 1.dp,               // 上下影线宽度
    // MA均线颜色配置
    val ma5Color: Color = Color(0xFF5470C6),
    val ma10Color: Color = Color(0xFFFAC858),
    val ma20Color: Color = Color(0xFF9A60B4),
    val maLineWidth: Dp = 1.dp
) {
    companion object {
        val Default = KLineStyle()
        
        // 国际习惯：绿涨红跌，均为实心
        val International = KLineStyle(
            upColor = Color(0xFF3BA272),
            downColor = Color(0xFFEE6666),
            upFilled = true,
            downFilled = true
        )
    }
}
