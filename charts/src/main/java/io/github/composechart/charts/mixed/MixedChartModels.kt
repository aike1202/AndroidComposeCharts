package io.github.composechart.charts.mixed

import io.github.composechart.charts.bar.BarSeries
import io.github.composechart.charts.line.LineSeries

/**
 * 折线柱状混合图表整体数据模型
 */
data class MixedChartData(
    val xLabels: List<String>,
    val lineSeries: List<LineSeries> = emptyList(),
    val barSeries: List<BarSeries> = emptyList()
)
