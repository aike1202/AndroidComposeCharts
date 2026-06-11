package io.github.composechart.showroom.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.composechart.charts.bar.BarSeries
import io.github.composechart.charts.bar.BarValue
import io.github.composechart.charts.line.LinePoint
import io.github.composechart.charts.line.LineSeries
import io.github.composechart.charts.mixed.MixedChart
import io.github.composechart.charts.mixed.MixedChartData
import io.github.composechart.core.state.ViewportMode
import io.github.composechart.core.state.rememberViewportState
import io.github.composechart.core.style.ChartStyle
import io.github.composechart.core.style.IndicatorStyle
import io.github.composechart.core.style.TitleOptions
import io.github.composechart.core.style.TooltipOptions

/**
 * 混合折柱双 Y 轴图表 Demo
 */
@Composable
fun DemoMixedChart(style: ChartStyle) {
    var barWidthRatio by remember { mutableFloatStateOf(0.4f) }
    var cornerRadiusValue by remember { mutableFloatStateOf(8f) }
    var tooltipIndicator by remember { mutableStateOf(IndicatorStyle.Shadow) }

    var useScrollableMode by remember { mutableStateOf(false) }
    var visibleCount by remember { mutableFloatStateOf(6f) }
    var enablePan by remember { mutableStateOf(true) }
    var enableZoom by remember { mutableStateOf(true) }

    val viewportState = rememberViewportState()
    viewportState.viewportMode = if (useScrollableMode) ViewportMode.Scrollable(visibleCount.toInt()) else ViewportMode.Fit
    viewportState.enablePan = enablePan
    viewportState.enableZoom = enableZoom

    val labels = listOf("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月")

    val data = MixedChartData(
        xLabels = labels,
        barSeries = listOf(
            BarSeries(
                name = "蒸发量",
                values = listOf(2.0f, 4.9f, 7.0f, 23.2f, 25.6f, 76.7f, 135.6f, 162.2f, 32.6f, 20.0f, 6.4f, 3.3f).map { BarValue(it) },
                color = Color(0xFF5470C6),
                barWidthRatio = barWidthRatio,
                gradientBrush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF8098FF), Color(0xFF5470C6))
                ),
                cornerRadius = CornerRadius(cornerRadiusValue, cornerRadiusValue),
                yAxisIndex = 0 // 绑定左轴 Y1
            ),
            BarSeries(
                name = "降水量",
                values = listOf(2.6f, 5.9f, 9.0f, 26.4f, 28.7f, 70.7f, 175.6f, 182.2f, 48.7f, 18.8f, 6.0f, 2.3f).map { BarValue(it) },
                color = Color(0xFF91CC75),
                barWidthRatio = barWidthRatio,
                gradientBrush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFB5EA9A), Color(0xFF91CC75))
                ),
                cornerRadius = CornerRadius(cornerRadiusValue, cornerRadiusValue),
                yAxisIndex = 0 // 绑定左轴 Y1
            )
        ),
        lineSeries = listOf(
            LineSeries(
                name = "平均温度",
                points = listOf(2.0f, 2.2f, 3.3f, 4.5f, 6.3f, 10.2f, 20.3f, 23.4f, 23.0f, 16.5f, 12.0f, 6.2f)
                    .mapIndexed { idx, v -> LinePoint(idx.toFloat(), v) },
                color = Color(0xFFFAC858),
                isSmooth = true,
                yAxisIndex = 1 // 绑定右轴 Y2
            )
        )
    )

    val customStyle = style.copy(
        titleOptions = TitleOptions(
            text = "雨量蒸发温度变化混合图",
            subtext = "双 Y 轴混合，左右刻度完全对齐重合",
            textStyle = style.titleOptions.textStyle.copy(fontSize = 16.sp)
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
        ) {
            MixedChart(
                data = data,
                style = customStyle,
                viewportState = viewportState,
                tooltipOptions = TooltipOptions(
                    enabled = true,
                    indicatorStyle = tooltipIndicator,
                    indicatorColor = Color.LightGray.copy(alpha = 0.15f)
                ),
                modifier = Modifier.fillMaxSize()
            )
        }

        ControlPanel {
            ControlSlider(
                label = "柱体圆角 (cornerRadius)",
                value = cornerRadiusValue,
                valueRange = 0f..20f,
                onValueChange = { cornerRadiusValue = it },
                valueFormatter = { String.format("%.0f px", it) }
            )

            ControlSlider(
                label = "柱子占网格间距比例 (barWidthRatio)",
                value = barWidthRatio,
                valueRange = 0.2f..0.8f,
                onValueChange = { barWidthRatio = it },
                valueFormatter = { String.format("%.2f", it) }
            )

            ControlSelector(
                label = "手势聚焦指示器样式 (IndicatorStyle)",
                options = listOf(IndicatorStyle.Shadow, IndicatorStyle.Line),
                selectedOption = tooltipIndicator,
                onOptionSelected = { tooltipIndicator = it },
                optionLabel = {
                    when (it) {
                        IndicatorStyle.Shadow -> "阴影背景槽"
                        IndicatorStyle.Line -> "垂直指示虚线"
                        else -> "无"
                    }
                }
            )

            ControlSwitch(label = "只看一部分、滑动查看 (Scrollable Mode)", checked = useScrollableMode, onCheckedChange = { useScrollableMode = it })
            if (useScrollableMode) {
                ControlSlider(
                    label = "可视范围月份数 (visibleCount)",
                    value = visibleCount,
                    valueRange = 3f..12f,
                    onValueChange = { visibleCount = it },
                    valueFormatter = { String.format("%d 个月", it.toInt()) }
                )
            }
            ControlSwitch(label = "启用拖拽平移 (Enable Pan)", checked = enablePan, onCheckedChange = { enablePan = it })
            ControlSwitch(label = "启用捏合缩放 (Enable Zoom)", checked = enableZoom, onCheckedChange = { enableZoom = it })
        }
    }
}
