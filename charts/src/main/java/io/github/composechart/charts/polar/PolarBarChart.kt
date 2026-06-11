package io.github.composechart.charts.polar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import io.github.composechart.core.data.PolarCoordinateMapper
import io.github.composechart.core.plot.PolarCoordinate
import io.github.composechart.core.state.ViewportState
import io.github.composechart.core.style.ChartStyle
import io.github.composechart.core.style.LegendOptions
import io.github.composechart.core.style.LegendPosition

/**
 * 纯 Compose Canvas 绘制的极坐标柱状图/圆环图组件。
 */
@Composable
fun PolarBarChart(
    data: PolarChartData,
    modifier: Modifier = Modifier,
    style: ChartStyle = ChartStyle.Default,
    legendOptions: LegendOptions = LegendOptions.Default,
    horizontal: Boolean = false // false = Radial 扇区模式；true = Tangential 同心圆环模式
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    // 入场生长动画
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animationProgress.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }

    Column(
        modifier = modifier
            .background(style.backgroundColor)
            .padding(8.dp)
    ) {
        // ================= 1. 渲染标题 =================
        if (style.titleOptions.show && style.titleOptions.text.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalAlignment = when (style.titleOptions.alignment) {
                    Alignment.CenterHorizontally -> Alignment.CenterHorizontally
                    Alignment.End -> Alignment.End
                    else -> Alignment.Start
                }
            ) {
                Text(
                    text = style.titleOptions.text,
                    style = style.titleOptions.textStyle
                )
                if (style.titleOptions.subtext.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(style.titleOptions.itemGap))
                    Text(
                        text = style.titleOptions.subtext,
                        style = style.titleOptions.subtextStyle
                    )
                }
            }
        }

        // ================= 2. 渲染主 Canvas 图表 =================
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val widthPx = constraints.maxWidth.toFloat()
            val heightPx = constraints.maxHeight.toFloat()

            val leftMarginPx = with(density) { style.gridOptions.left.toPx() }
            val rightMarginPx = with(density) { style.gridOptions.right.toPx() }
            val topMarginPx = with(density) { style.gridOptions.top.toPx() }
            val bottomMarginPx = with(density) { style.gridOptions.bottom.toPx() }

            val gridRect = Rect(
                left = leftMarginPx,
                top = topMarginPx,
                right = widthPx - rightMarginPx,
                bottom = heightPx - bottomMarginPx
            )

            // 计算极值
            val maxRawVal = data.series.flatMap { it.values }.maxOfOrNull { it.value } ?: 10f
            val visibleMaxVal = if (maxRawVal <= 0f) 10f else maxRawVal

            // Nice Numbers 极值规整
            val niceInterval = io.github.composechart.core.util.AxisIntervalCalculator.calculate(
                min = 0f,
                max = visibleMaxVal,
                splitNumber = style.polarOptions.radiusStepNumber
            )

            // 构建极坐标映射算子
            val mapper = PolarCoordinateMapper(
                gridRect = gridRect,
                polarOptions = style.polarOptions,
                minRadiusVal = 0f,
                maxRadiusVal = niceInterval.max,
                outerPaddingPx = with(density) { 36.dp.toPx() } // 给外圈文字预留 36dp
            )

            // 构建极轴背景绘制器
            val polarPlotter = PolarCoordinate(
                mapper = mapper,
                style = style,
                textMeasurer = textMeasurer,
                density = density,
                xLabels = data.xLabels,
                rTicks = niceInterval.ticks
            )

            // 构建极轴柱体渲染器
            val polarRenderer = PolarBarChartRenderer(
                mapper = mapper,
                style = style,
                textMeasurer = textMeasurer,
                density = density,
                allSeries = data.series,
                horizontal = horizontal,
                animationProgress = animationProgress.value
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                // 绘制极坐标网格背景环线及射线
                polarPlotter.drawBackground(this)

                // 绘制极轴柱体
                polarRenderer.draw(this)

                // 绘制极轴刻度及角度分类标签
                polarPlotter.drawAxesAndLabels(this)
            }
        }
    }
}
