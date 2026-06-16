package io.github.composechart.core.style

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * ECharts 风格经典配色板（浅色/默认）
 */
val DefaultColors = listOf(
    Color(0xFF5470C6),
    Color(0xFF91CC75),
    Color(0xFFFAC858),
    Color(0xFFEE6666),
    Color(0xFF73C0DE),
    Color(0xFF3BA272),
    Color(0xFFFC8452),
    Color(0xFF9A60B4),
    Color(0xFFEA7CCC)
)

/**
 * ECharts 风格暗黑配色板
 */
val DarkColors = listOf(
    Color(0xFF4992FF),
    Color(0xFF7CFFB2),
    Color(0xFFFDDD60),
    Color(0xFFFF6E76),
    Color(0xFF58D9F9),
    Color(0xFF05C091),
    Color(0xFFFF8A45),
    Color(0xFF8D7FEC),
    Color(0xFFEA7CCC)
)

/**
 * 统一图表的全局设计系统。内置网格、图例、标题、坐标轴等的样式。
 */
data class ChartStyle(
    val backgroundColor: Color = Color.Transparent,
    val titleOptions: TitleOptions = TitleOptions(),
    val legendOptions: LegendOptions = LegendOptions(),
    val gridOptions: GridOptions = GridOptions(),
    val xAxisOptions: AxisOptions = AxisOptions(showGridLines = false), // X轴默认不绘制垂直网格
    val yAxisOptions: AxisOptions = AxisOptions(showGridLines = true),  // Y轴默认绘制水平网格
    val y2AxisOptions: AxisOptions = AxisOptions(show = false),         // 副 Y 轴配置 (默认不显示)
    val x2AxisOptions: AxisOptions = AxisOptions(show = false),         // 顶部 X2 轴配置 (默认不显示)
    val colorPalette: List<Color> = DefaultColors,
    val pieOptions: PieOptions = PieOptions(),
    val radarOptions: RadarOptions = RadarOptions(),
    val polarOptions: PolarOptions = PolarOptions()
) {
    companion object {
        val Light = ChartStyle()
        val Dark = ChartStyle(
            backgroundColor = Color(0xFF1B1B1D),
            titleOptions = TitleOptions(
                textStyle = TextStyle(color = Color(0xFFEEEEEE), fontSize = 15.sp, fontWeight = FontWeight.Bold),
                subtextStyle = TextStyle(color = Color(0xFFB0B0B0), fontSize = 11.sp)
            ),
            legendOptions = LegendOptions(
                textStyle = TextStyle(color = Color(0xFFCCCCCC), fontSize = 11.sp)
            ),
            xAxisOptions = AxisOptions(
                showGridLines = false,
                lineColor = Color(0xFF6E7078),
                tickColor = Color(0xFF6E7078),
                labelTextStyle = TextStyle(color = Color(0xFFB0B3C0), fontSize = 10.sp)
            ),
            yAxisOptions = AxisOptions(
                showGridLines = true,
                lineColor = Color(0xFF6E7078),
                tickColor = Color(0xFF6E7078),
                gridLineColor = Color(0xFF3A3A3D),
                labelTextStyle = TextStyle(color = Color(0xFFB0B3C0), fontSize = 10.sp)
            ),
            colorPalette = DarkColors
        )
        val Default = Light
    }
}

/**
 * 标题选项配置
 */
data class TitleOptions(
    val show: Boolean = true,
    val text: String = "",
    val subtext: String = "",
    val textStyle: TextStyle = TextStyle(color = Color(0xFF333333), fontSize = 15.sp, fontWeight = FontWeight.Bold),
    val subtextStyle: TextStyle = TextStyle(color = Color(0xFF999999), fontSize = 11.sp),
    val itemGap: Dp = 4.dp, // 主标题与副标题之间的垂直间距
    val padding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    val alignment: Alignment.Horizontal = Alignment.Start
)

/**
 * 图例选项配置
 */
data class LegendOptions(
    val show: Boolean = true,
    val position: LegendPosition = LegendPosition.Top, // Top, Bottom, Left, Right
    val alignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    val itemGap: Dp = 12.dp, // 图例项之间的水平或垂直间隔
    val itemWidth: Dp = 14.dp,
    val itemHeight: Dp = 10.dp,
    val iconShape: LegendIconShape = LegendIconShape.RoundRect, // RoundRect, Circle, Rect, Line, Diamond
    val textStyle: TextStyle = TextStyle(color = Color(0xFF666666), fontSize = 11.sp),
    val selectMode: LegendSelectMode = LegendSelectMode.Multiple // 允许多选过滤、单选或禁用过滤
) {
    companion object {
        val Default = LegendOptions()
    }
}

enum class LegendPosition { Top, Bottom, Left, Right }
enum class LegendIconShape { RoundRect, Circle, Rect, Line, Diamond }
enum class LegendSelectMode { Multiple, Single, None }

/**
 * 坐标轴细节配置
 */
data class AxisOptions(
    val show: Boolean = true,
    val onZero: Boolean = false, // 坐标轴线是否锁定在零值像素线上
    val inverse: Boolean = false, // 是否反转坐标轴 (主要用于 Y 轴反转，即排名/凹凸图)
    val name: String = "", // 坐标轴单位/名称，如 "温度 (℃)"
    val nameTextStyle: TextStyle = TextStyle(color = Color(0xFF888888), fontSize = 10.sp),
    // 轴线 (Axis Line)
    val showLine: Boolean = true,
    val lineColor: Color = Color(0xFF757575),
    val lineWidth: Dp = 1.dp,
    // 刻度线 (Axis Ticks)
    val showTicks: Boolean = true,
    val tickColor: Color = Color(0xFF757575),
    val tickLength: Dp = 4.dp,
    // 刻度标签文本 (Axis Labels)
    val showLabels: Boolean = true,
    val labelTextStyle: TextStyle = TextStyle(color = Color(0xFF757575), fontSize = 10.sp),
    val labelRotate: Float = 0f, // 标签旋转度数（如 -45f）
    val labelOnZero: Boolean = true, // 零轴标签是否跟着零线走（若为false，则不管零轴在哪都固定在网格边缘，防止重叠）
    val labelFormatter: ((value: Float) -> String)? = null, // 标签自定义文字格式化回调
    // 坐标网格分割线 (Split Line)
    val showGridLines: Boolean = true,
    val gridLineColor: Color = Color(0xFFE0E0E0),
    val gridLineWidth: Dp = 0.5.dp,
    val gridLineStyle: GridLineStyle = GridLineStyle.Solid, // Solid, Dashed
    // 交替分割区域背景色 (Split Area)
    val showSplitArea: Boolean = false,
    val splitAreaColors: List<Color> = listOf(Color.Transparent, Color(0xFFF9F9F9).copy(alpha = 0.4f))
)

enum class GridLineStyle { Solid, Dashed }

/**
 * 图表绘制网格布局边距
 */
data class GridOptions(
    val left: Dp = 40.dp,   // 预留给左轴标签的 Margin
    val top: Dp = 48.dp,    // 预留给 Title/Legend 的 Margin
    val right: Dp = 24.dp,  // 预留给右侧辅助轴的 Margin
    val bottom: Dp = 40.dp, // 预留给 X 轴标签的 Margin
    val containLabel: Boolean = true // 是否根据坐标轴标签文本长度自动扩展 Grid 边距
)

/**
 * 扫掠提示框选项
 */
data class TooltipOptions(
    val enabled: Boolean = true,
    val trigger: TriggerType = TriggerType.Axis, // Axis (轴对齐磁吸) 或 Item (数据项对齐)
    val indicatorStyle: IndicatorStyle = IndicatorStyle.Line, // Line(垂直线), Cross(十字光标), Shadow(阴影块)
    val indicatorColor: Color = Color(0xFF5470C6).copy(alpha = 0.4f),
    val backgroundColor: Color = Color.White.copy(alpha = 0.9f),
    val borderColor: Color = Color(0xFFE0E0E0),
    val borderWidth: Dp = 1.dp,
    val cornerRadius: Dp = 4.dp,
    val textStyle: TextStyle = TextStyle(color = Color(0xFF333333), fontSize = 12.sp),
    val shadowRadius: Dp = 6.dp
) {
    companion object {
        val Default = TooltipOptions()
    }
}

enum class TriggerType { Axis, Item }
enum class IndicatorStyle { Line, Cross, Shadow }

/**
 * 缩放滑块 DataZoom 选项
 */
data class DataZoomOptions(
    val height: Dp = 36.dp,
    val backgroundColor: Color = Color(0xFFF4F4F4),
    val borderColor: Color = Color(0xFFE0E0E0),
    val fillerColor: Color = Color(0xFF5470C6).copy(alpha = 0.2f), // 选中滑动区域颜色
    val handleColor: Color = Color(0xFFFFFFFF), // 左右把手颜色
    val handleStrokeColor: Color = Color(0xFF5470C6)
)

/**
 * 饼图/环形图/玫瑰图专属样式配置
 */
data class PieOptions(
    val roseType: RoseType = RoseType.None,
    val innerRadiusRatio: Float = 0f, // 内圆半径占比 (0f-1f), 0f 为实心饼图, >0f 为环形图
    val outerRadiusRatio: Float = 0.8f, // 外圆半径占比 (0f-1f), 用于调节饼图圈圈的大小
    val padAngle: Float = 0f,         // 扇区空隙角度 (度数, 0f-15f)
    val cornerRadius: Dp = 0.dp,      // 扇区圆角半径
    val selectedOffset: Dp = 10.dp,   // 触控选中扇区外弹平移的物理距离
    val showLabel: Boolean = true,    // 是否显示边缘引导折线与文字
    val labelLineLength1: Dp = 15.dp, // 引导线斜段长度
    val labelLineLength2: Dp = 10.dp, // 引导线水平段长度
    val labelLineColor: Color? = null, // 引导折线颜色，若为空则默认跟随对应扇区颜色
    val labelLineWidth: Dp = 1.dp,
    val startAngle: Float = -90f,      // 饼图/环形图起始扫掠角度 (默认正上方 -90f)
    val maxAngleSweep: Float = 360f,   // 饼图/环形图最大扫掠角度 range
    val roundCap: Boolean = false,     // 是否为环形图的端面开启首尾半圆圆角
    val borderWidth: Dp = 0.dp,        // 边框描边宽度 (配合 cornerRadius 实现网页版防重叠圆角)
    val borderColor: Color? = null,     // 边框描边颜色，为 null 时自动采用图表背景色
    val labelAlign: PieLabelAlign = PieLabelAlign.None // 标签排列对齐模式 (None 放射, Edge 左右垂直对齐)
) {
    companion object {
        val Default = PieOptions()
    }
}

enum class RoseType {
    None,   // 普通饼图
    Radius, // 南丁格尔玫瑰图 (夹角等分，半径代表数值大小)
    Area    // 南丁格尔玫瑰图 (夹角按比例分配，半径代表数值大小)
}

enum class PieLabelAlign {
    None,      // 放射模式 (默认，引线斜段角度跟随扇区中心角度)
    LeftRight, // 左右边缘垂直对齐 (折角点垂直对齐至左右两侧，屏蔽上下方文字)
    TopBottom  // 上下边缘水平对齐 (折角点水平对齐至上下两侧，屏蔽左右侧文字)
}

/**
 * 雷达图专属样式配置
 */
data class RadarOptions(
    val shape: RadarShape = RadarShape.Polygon,
    val splitNumber: Int = 5,             // 同心分段层数
    val gridColor: Color = Color(0xFFD3D3D3), // 网格背景线颜色
    val gridLineWidth: Dp = 0.8.dp,
    val axisLineColor: Color = Color(0xFFD3D3D3), // 发散辅助射线颜色
    val axisLineWidth: Dp = 0.8.dp,
    val drawScaleLabel: Boolean = true,   // 是否在12点钟射线绘制刻度值
    val symbolSize: Dp = 4.dp             // 数据点小圈半径
) {
    companion object {
        val Default = RadarOptions()
    }
}

enum class RadarShape {
    Polygon, // 蛛网折线多边形
    Circle   // 蛛网圆形
}

/**
 * 极坐标系专属样式配置
 */
data class PolarOptions(
    val show: Boolean = true,
    val centerX: Float = 0.5f, // 相对图表网格中心的比例 (0.0f - 1.0f)
    val centerY: Float = 0.5f,
    val startAngle: Float = 90f, // 极坐标 0 度对应的物理角度，默认 90f (12点钟方向)
    val endAngle: Float = 360f + 90f, // 极坐标终止角度，默认 360f + 90f。若 endAngle - startAngle != 360f 则为非闭合扇形极轴
    val radiusStepNumber: Int = 5, // 径向轴网格环线层数
    val gridColor: Color = Color(0xFFD3D3D3), // 网格背景线颜色
    val gridLineWidth: Dp = 0.8.dp,
    val axisLineColor: Color = Color(0xFFD3D3D3), // 放射轴线颜色
    val axisLineWidth: Dp = 0.8.dp,
    val drawRadiusLabel: Boolean = true, // 是否绘制径向轴刻度值
    val drawAngleLabel: Boolean = true  // 是否在最外圈绘制角度分类标签
) {
    companion object {
        val Default = PolarOptions()
    }
}
