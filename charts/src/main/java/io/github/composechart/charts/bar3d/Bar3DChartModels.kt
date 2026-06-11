package io.github.composechart.charts.bar3d

import androidx.compose.ui.graphics.Color

/**
 * 3D 柱体三维点数据结构
 */
data class Bar3DPoint(
    val xIndex: Int,          // X 轴类目索引
    val yIndex: Int,          // Y 轴类目索引
    val zValue: Float,         // Z 轴高度数值
    val color: Color? = null   // 自定义此柱体颜色，若为空则自动根据 visualMapColors 渐变色系及 Z 值决定
)

/**
 * 3D 柱体数据集
 */
data class Bar3DChartData(
    val xAxisLabels: List<String>,    // X 轴标签（如 24 小时：0a, 1a, ..., 11p）
    val yAxisLabels: List<String>,    // Y 轴标签（如 7 天：Monday, ..., Sunday）
    val points: List<Bar3DPoint>,     // 3D 散点列表
    val zMin: Float = 0f,             // Z 轴高度最小值限制，用于高度归一化
    val zMax: Float = 12f             // Z 轴高度最大值限制，用于高度归一化
)

/**
 * 3D 柱体样式配置
 */
data class Bar3DOptions(
    val initialYaw: Float = -45f,             // 初始水平偏航角（度数，绕 Z 轴旋转）
    val initialPitch: Float = 30f,            // 初始垂直俯仰角（度数，视角倾斜度）
    val initialZoom: Float = 1.0f,            // 初始视角缩放比例 (0.5 .. 2.5)
    val barWidthRatio: Float = 0.5f,          // 柱体实际宽度占对应格网宽度的比例 (0.0 .. 1.0)
    val gridColor: Color = Color.Gray.copy(alpha = 0.25f), // 3D 底层坐标盒网格线颜色
    
    // 视觉热力值映射色带（从 Z 轴最小值到最大值渐变）
    val visualMapColors: List<Color> = listOf(
        Color(0xFF313695), // 深蓝
        Color(0xFF4575B4), // 蓝
        Color(0xFF74ADD1), // 浅蓝
        Color(0xFFABD9E9), // 极浅蓝
        Color(0xFFE0F3F8), // 亮蓝
        Color(0xFFFFFFBF), // 浅黄
        Color(0xFFFEE090), // 黄
        Color(0xFFFDAE61), // 橙黄
        Color(0xFFF46D43), // 橙红
        Color(0xFFD73027), // 红
        Color(0xFFA50026)  // 深红
    ),
    val showVisualMap: Boolean = true,        // 是否在图表下方渲染热力对照指示色带
    val labelColor: Color? = null             // 轴文本标签文字颜色，默认自动适配明暗主题
) {
    companion object {
        val Default = Bar3DOptions()
    }
}
