package io.github.composechart.charts.radar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.composechart.core.data.CoordinateMapper
import io.github.composechart.core.state.ViewportState
import io.github.composechart.core.style.ChartStyle
import io.github.composechart.core.style.LegendIconShape
import io.github.composechart.core.style.LegendOptions
import io.github.composechart.core.style.LegendPosition
import io.github.composechart.core.style.LegendSelectMode
import io.github.composechart.core.style.TitleOptions

/**
 * 原生极坐标多维属性雷达图组件 (RadarChart)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RadarChart(
    data: RadarChartData,
    modifier: Modifier = Modifier,
    style: ChartStyle = ChartStyle.Default,
    legendOptions: LegendOptions = LegendOptions.Default
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    // 管理数据系列点击多选联动过滤
    val hiddenSeriesNames = remember { mutableStateListOf<String>() }

    // 初始雷达向外爆破生长插值动画
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animationProgress.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    val visibleSeries = data.series.filter { it.name !in hiddenSeriesNames }

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

        // ================= 2. 渲染顶部图例 =================
        if (legendOptions.show && legendOptions.position == LegendPosition.Top) {
            RenderRadarLegend(
                seriesList = data.series,
                hiddenList = hiddenSeriesNames,
                options = legendOptions,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ================= 3. 核心 Canvas 雷达图 =================
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

            val mapper = CoordinateMapper(gridRect = gridRect, viewportState = ViewportState())
            val renderer = remember(mapper, style, animationProgress.value) {
                RadarChartRenderer(
                    mapper = mapper,
                    style = style,
                    textMeasurer = textMeasurer,
                    density = density,
                    animationProgress = animationProgress.value
                )
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                renderer.draw(this, data.indicators, visibleSeries)
            }
        }

        // ================= 4. 渲染底部图例 =================
        if (legendOptions.show && legendOptions.position == LegendPosition.Bottom) {
            RenderRadarLegend(
                seriesList = data.series,
                hiddenList = hiddenSeriesNames,
                options = legendOptions,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * 渲染雷达图专用的图例 FlowRow
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RenderRadarLegend(
    seriesList: List<RadarSeries>,
    hiddenList: List<String>,
    options: LegendOptions,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.padding(vertical = 4.dp, horizontal = 12.dp),
        horizontalArrangement = when (options.alignment) {
            Alignment.Start -> androidx.compose.foundation.layout.Arrangement.Start
            Alignment.End -> androidx.compose.foundation.layout.Arrangement.End
            else -> androidx.compose.foundation.layout.Arrangement.Center
        }
    ) {
        seriesList.forEach { series ->
            val isHidden = series.name in hiddenList
            Row(
                modifier = Modifier
                    .padding(horizontal = options.itemGap / 2, vertical = 2.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(enabled = options.selectMode != LegendSelectMode.None) {
                        if (options.selectMode == LegendSelectMode.Multiple) {
                            if (isHidden) {
                                (hiddenList as MutableList<String>).remove(series.name)
                            } else {
                                (hiddenList as MutableList<String>).add(series.name)
                            }
                        } else if (options.selectMode == LegendSelectMode.Single) {
                            (hiddenList as MutableList<String>).clear()
                            seriesList.forEach {
                                if (it.name != series.name) {
                                    (hiddenList as MutableList<String>).add(it.name)
                                }
                            }
                        }
                    }
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val indicatorColor = if (isHidden) Color.LightGray else series.color
                val textAlpha = if (isHidden) 0.4f else 1.0f

                Box(
                    modifier = Modifier
                        .size(options.itemWidth, options.itemHeight)
                        .clip(
                            if (options.iconShape == LegendIconShape.Circle) RoundedCornerShape(50)
                            else RoundedCornerShape(2.dp)
                        )
                        .background(indicatorColor)
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = series.name,
                    style = options.textStyle.copy(
                        color = options.textStyle.color.copy(alpha = textAlpha)
                    )
                )
            }
        }
    }
}
