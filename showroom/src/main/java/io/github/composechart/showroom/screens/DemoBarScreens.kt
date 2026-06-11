package io.github.composechart.showroom.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import io.github.composechart.charts.bar.BarChart
import io.github.composechart.charts.bar.BarChartData
import io.github.composechart.charts.bar.BarSeries
import io.github.composechart.charts.bar.BarValue
import io.github.composechart.core.state.ViewportMode
import io.github.composechart.core.state.rememberViewportState
import io.github.composechart.core.style.ChartStyle
import io.github.composechart.core.style.IndicatorStyle
import io.github.composechart.core.style.TitleOptions
import io.github.composechart.core.style.TooltipOptions

/**
 * Demo 4: 并列双柱状图对比
 */
@Composable
fun DemoBarComparison(style: ChartStyle) {
    var barWidthRatio by remember { mutableFloatStateOf(0.45f) }
    var cornerRadiusValue by remember { mutableFloatStateOf(16f) }
    var showBackground by remember { mutableStateOf(true) }
    var tooltipIndicator by remember { mutableStateOf(IndicatorStyle.Shadow) }

    var useScrollableMode by remember { mutableStateOf(false) }
    var visibleCount by remember { mutableFloatStateOf(3f) }
    var enablePan by remember { mutableStateOf(true) }
    var enableZoom by remember { mutableStateOf(true) }

    val viewportState = rememberViewportState()
    viewportState.viewportMode = if (useScrollableMode) ViewportMode.Scrollable(visibleCount.toInt()) else ViewportMode.Fit
    viewportState.enablePan = enablePan
    viewportState.enableZoom = enableZoom

    val labels = listOf("手机", "电脑", "平板", "穿戴", "配件")
    
    val data = BarChartData(
        xLabels = labels,
        series = listOf(
            BarSeries(
                name = "2024年销量",
                values = listOf(320f, 220f, 180f, 290f, 150f).map { BarValue(it) },
                color = Color(0xFF5470C6),
                barWidthRatio = barWidthRatio,
                gradientBrush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF8098FF), Color(0xFF5470C6))
                ),
                cornerRadius = CornerRadius(cornerRadiusValue, cornerRadiusValue),
                showBackground = showBackground,
                backgroundColor = Color.LightGray.copy(alpha = 0.15f)
            ),
            BarSeries(
                name = "2025年销量",
                values = listOf(390f, 260f, 210f, 340f, 190f).map { BarValue(it) },
                color = Color(0xFF91CC75),
                barWidthRatio = barWidthRatio,
                gradientBrush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFB5EA9A), Color(0xFF91CC75))
                ),
                cornerRadius = CornerRadius(cornerRadiusValue, cornerRadiusValue),
                showBackground = showBackground,
                backgroundColor = Color.LightGray.copy(alpha = 0.15f)
            )
        )
    )

    val customStyle = style.copy(
        titleOptions = TitleOptions(
            text = "智能硬件销量对比",
            subtext = "支持顶部圆角、高颜值渐变填充及背景槽渲染",
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
                .height(360.dp)
        ) {
            BarChart(
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
            ControlSwitch(label = "显示背景柱槽 (showBackground)", checked = showBackground, onCheckedChange = { showBackground = it })
            
            ControlSlider(
                label = "柱体圆角弧度 (cornerRadius)",
                value = cornerRadiusValue,
                valueRange = 0f..40f,
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
                    label = "可视范围点数 (visibleCount)",
                    value = visibleCount,
                    valueRange = 2f..5f,
                    onValueChange = { visibleCount = it },
                    valueFormatter = { String.format("%d 个", it.toInt()) }
                )
            }
            ControlSwitch(label = "启用拖拽平移 (Enable Pan)", checked = enablePan, onCheckedChange = { enablePan = it })
            ControlSwitch(label = "启用捏合缩放 (Enable Zoom)", checked = enableZoom, onCheckedChange = { enableZoom = it })
        }
    }
}

/**
 * Demo 5: 正负堆叠柱状图
 */
@Composable
fun DemoBarStack(style: ChartStyle) {
    var barWidthRatio by remember { mutableFloatStateOf(0.4f) }
    var cornerRadiusValue by remember { mutableFloatStateOf(8f) }
    var animateProgress by remember { mutableFloatStateOf(1.0f) }

    var useScrollableMode by remember { mutableStateOf(false) }
    var visibleCount by remember { mutableFloatStateOf(3f) }
    var enablePan by remember { mutableStateOf(true) }
    var enableZoom by remember { mutableStateOf(true) }

    val viewportState = rememberViewportState()
    viewportState.viewportMode = if (useScrollableMode) ViewportMode.Scrollable(visibleCount.toInt()) else ViewportMode.Fit
    viewportState.enablePan = enablePan
    viewportState.enableZoom = enableZoom

    val labels = listOf("第一季度", "第二季度", "第三季度", "第四季度")
    
    val data = BarChartData(
        xLabels = labels,
        series = listOf(
            BarSeries(
                name = "主营收入",
                values = listOf(420f, 550f, 490f, 610f).map { BarValue(it) },
                color = Color(0xFF3BA272),
                barWidthRatio = barWidthRatio,
                gradientBrush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF67D4A4), Color(0xFF3BA272))
                ),
                stack = "finance",
                cornerRadius = CornerRadius(cornerRadiusValue, cornerRadiusValue)
            ),
            BarSeries(
                name = "其他收入",
                values = listOf(80f, 120f, 90f, 150f).map { BarValue(it) },
                color = Color(0xFFFAC858),
                barWidthRatio = barWidthRatio,
                gradientBrush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFDF8A), Color(0xFFFAC858))
                ),
                stack = "finance",
                cornerRadius = CornerRadius(cornerRadiusValue, cornerRadiusValue)
            ),
            BarSeries(
                name = "退货扣减",
                values = listOf(-60f, -90f, -50f, -110f).map { BarValue(it) },
                color = Color(0xFFEE6666),
                barWidthRatio = barWidthRatio,
                gradientBrush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFEE6666), Color(0xFFFF9E9E))
                ),
                stack = "finance",
                cornerRadius = CornerRadius(cornerRadiusValue, cornerRadiusValue)
            ),
            BarSeries(
                name = "运营成本",
                values = listOf(-220f, -280f, -240f, -310f).map { BarValue(it) },
                color = Color(0xFF73C0DE),
                barWidthRatio = barWidthRatio,
                gradientBrush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF73C0DE), Color(0xFFA5E3FB))
                ),
                stack = "finance",
                cornerRadius = CornerRadius(cornerRadiusValue, cornerRadiusValue)
            )
        )
    )

    val customStyle = style.copy(
        titleOptions = TitleOptions(
            text = "季度营收财务看板 (双向堆叠)",
            subtext = "单柱多项财务指标堆叠，支持正数与负数双向折算",
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
                .height(360.dp)
        ) {
            BarChart(
                data = data,
                style = customStyle,
                viewportState = viewportState,
                tooltipOptions = TooltipOptions(
                    enabled = true,
                    indicatorStyle = IndicatorStyle.Shadow,
                    indicatorColor = Color.LightGray.copy(alpha = 0.15f)
                ),
                modifier = Modifier.fillMaxSize()
            )
        }

        ControlPanel {
            ControlSlider(
                label = "堆叠柱体圆角 (cornerRadius)",
                value = cornerRadiusValue,
                valueRange = 0f..20f,
                onValueChange = { cornerRadiusValue = it },
                valueFormatter = { String.format("%.0f px", it) }
            )

            ControlSlider(
                label = "堆叠柱体宽度比例 (barWidthRatio)",
                value = barWidthRatio,
                valueRange = 0.2f..0.7f,
                onValueChange = { barWidthRatio = it },
                valueFormatter = { String.format("%.2f", it) }
            )

            ControlSwitch(label = "只看一部分、滑动查看 (Scrollable Mode)", checked = useScrollableMode, onCheckedChange = { useScrollableMode = it })
            if (useScrollableMode) {
                ControlSlider(
                    label = "可视范围点数 (visibleCount)",
                    value = visibleCount,
                    valueRange = 2f..4f,
                    onValueChange = { visibleCount = it },
                    valueFormatter = { String.format("%d 个", it.toInt()) }
                )
            }
            ControlSwitch(label = "启用拖拽平移 (Enable Pan)", checked = enablePan, onCheckedChange = { enablePan = it })
            ControlSwitch(label = "启用捏合缩放 (Enable Zoom)", checked = enableZoom, onCheckedChange = { enableZoom = it })
        }
    }
}

/**
 * Demo 6: 水平条形图
 */
@Composable
fun DemoHorizontalBar(style: ChartStyle) {
    var barWidthRatio by remember { mutableFloatStateOf(0.45f) }
    var cornerRadiusValue by remember { mutableFloatStateOf(12f) }
    var showBackground by remember { mutableStateOf(true) }

    var useScrollableMode by remember { mutableStateOf(false) }
    var visibleCount by remember { mutableFloatStateOf(3f) }
    var enablePan by remember { mutableStateOf(true) }
    var enableZoom by remember { mutableStateOf(true) }

    val viewportState = rememberViewportState()
    viewportState.viewportMode = if (useScrollableMode) ViewportMode.Scrollable(visibleCount.toInt()) else ViewportMode.Fit
    viewportState.enablePan = enablePan
    viewportState.enableZoom = enableZoom

    val labels = listOf("北京市", "上海市", "广州市", "深圳市", "杭州市", "成都市")
    
    val data = BarChartData(
        xLabels = labels,
        series = listOf(
            BarSeries(
                name = "空气质量指数 (AQI)",
                values = listOf(55f, 48f, 65f, 42f, 58f, 72f).map { BarValue(it) },
                color = Color(0xFF73C0DE),
                barWidthRatio = barWidthRatio,
                gradientBrush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF73C0DE), Color(0xFFA5E3FB))
                ),
                cornerRadius = CornerRadius(cornerRadiusValue, cornerRadiusValue),
                showBackground = showBackground,
                backgroundColor = Color.LightGray.copy(alpha = 0.15f)
            )
        )
    )

    val customStyle = style.copy(
        titleOptions = TitleOptions(
            text = "城市空气质量指数排名",
            subtext = "水平条形图旋转排布，支持横向比例与背景槽渲染",
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
                .height(360.dp)
        ) {
            BarChart(
                data = data,
                style = customStyle,
                horizontal = true,
                viewportState = viewportState,
                tooltipOptions = TooltipOptions(
                    enabled = true,
                    indicatorStyle = IndicatorStyle.Shadow,
                    indicatorColor = Color.LightGray.copy(alpha = 0.15f)
                ),
                modifier = Modifier.fillMaxSize()
            )
        }

        ControlPanel {
            ControlSwitch(label = "显示水平背景横槽 (showBackground)", checked = showBackground, onCheckedChange = { showBackground = it })

            ControlSlider(
                label = "条体边缘圆角 (cornerRadius)",
                value = cornerRadiusValue,
                valueRange = 0f..25f,
                onValueChange = { cornerRadiusValue = it },
                valueFormatter = { String.format("%.0f px", it) }
            )

            ControlSlider(
                label = "条体占用槽宽比例 (barWidthRatio)",
                value = barWidthRatio,
                valueRange = 0.2f..0.8f,
                onValueChange = { barWidthRatio = it },
                valueFormatter = { String.format("%.2f", it) }
            )

            ControlSwitch(label = "只看一部分、滑动查看 (Scrollable Mode)", checked = useScrollableMode, onCheckedChange = { useScrollableMode = it })
            if (useScrollableMode) {
                ControlSlider(
                    label = "可视范围点数 (visibleCount)",
                    value = visibleCount,
                    valueRange = 2f..6f,
                    onValueChange = { visibleCount = it },
                    valueFormatter = { String.format("%d 个", it.toInt()) }
                )
            }
            ControlSwitch(label = "启用拖拽平移 (Enable Pan)", checked = enablePan, onCheckedChange = { enablePan = it })
            ControlSwitch(label = "启用捏合缩放 (Enable Zoom)", checked = enableZoom, onCheckedChange = { enableZoom = it })
        }
    }
}
