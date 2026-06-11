package io.github.composechart.charts.pie

import androidx.compose.ui.graphics.Color

/**
 * 饼图/环形图/玫瑰图整体数据结构
 */
data class PieChartData(
    val slices: List<PieSlice>
)

/**
 * 饼图单扇区数据模型
 */
data class PieSlice(
    val name: String,
    val value: Float,
    val color: Color
)
