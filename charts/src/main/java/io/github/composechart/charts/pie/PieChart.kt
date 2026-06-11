package io.github.composechart.charts.pie

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.composechart.core.data.CoordinateMapper
import io.github.composechart.core.style.ChartStyle
import io.github.composechart.core.style.LegendIconShape
import io.github.composechart.core.style.LegendOptions
import io.github.composechart.core.style.LegendPosition
import io.github.composechart.core.style.LegendSelectMode
import io.github.composechart.core.style.TitleOptions
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt

/**
 * 原生圆形比例图表组件 (PieChart)，支持实心饼图、空心环形图及南丁格尔玫瑰图
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PieChart(
    data: PieChartData,
    modifier: Modifier = Modifier,
    style: ChartStyle = ChartStyle.Default,
    legendOptions: LegendOptions = LegendOptions.Default,
    onSliceSelected: ((slice: PieSlice) -> Unit)? = null,
    centerContent: (@Composable () -> Unit)? = null // 环形图中心叠加插槽
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    // 管理系列（扇区）点击过滤
    val hiddenSlices = remember { mutableStateListOf<String>() }

    // 选中扇区索引与外弹弹出动画
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val selectedOffsetAnim = remember { Animatable(0f) }

    // 当过滤状态改变时，清除选中项
    LaunchedEffect(hiddenSlices.size) {
        selectedIndex = null
    }

    // 监听选中状态并驱动动画
    LaunchedEffect(selectedIndex) {
        selectedOffsetAnim.snapTo(0f)
        selectedOffsetAnim.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
        )
    }

    // 初始顺时针旋转展开动画
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animationProgress.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    val visibleSlices = data.slices.filter { it.name !in hiddenSlices }
    val totalValue = visibleSlices.sumOf { it.value.toDouble() }.toFloat()

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
            RenderPieLegend(
                slices = data.slices,
                hiddenList = hiddenSlices,
                options = legendOptions,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ================= 3. 渲染核心 Canvas =================
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
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

                val mapper = CoordinateMapper(gridRect = gridRect, viewportState = io.github.composechart.core.state.ViewportState())
                val renderer = remember(style) {
                    PieChartRenderer(
                        mapper = mapper,
                        style = style,
                        textMeasurer = textMeasurer,
                        density = density
                    )
                }

                // 物理尺寸转换与选中偏移参数
                val selectedOffsetMaxPx = with(density) { style.pieOptions.selectedOffset.toPx() }
                val currentOffsetPx = selectedOffsetAnim.value * selectedOffsetMaxPx

                // 圆心物理坐标，用于点击交互碰撞识别
                val centerX = gridRect.left + gridRect.width / 2f
                val centerY = gridRect.top + gridRect.height / 2f

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(data.slices, hiddenSlices.size, renderer.sliceDrawInfos) {
                            detectTapGestures { offset ->
                                val dx = offset.x - centerX
                                val dy = offset.y - centerY
                                val distance = sqrt(dx * dx + dy * dy)

                                // 角度归一化映射到 [startAngle, startAngle + 360f) 的单周期区间
                                val startAngle = style.pieOptions.startAngle
                                var angleDegrees = Math
                                    .toDegrees(atan2(dy.toDouble(), dx.toDouble()))
                                    .toFloat()
                                while (angleDegrees < startAngle) {
                                    angleDegrees += 360f
                                }
                                while (angleDegrees >= startAngle + 360f) {
                                    angleDegrees -= 360f
                                }

                                // 碰撞检测
                                var foundIndex: Int? = null
                                renderer.sliceDrawInfos.forEach { info ->
                                    val start = info.startAngle
                                    val end = start + info.sweepAngle
                                    if (distance in info.innerRadius..info.outerRadius &&
                                        angleDegrees in start..end
                                    ) {
                                        foundIndex = info.index
                                    }
                                }

                                if (foundIndex != null) {
                                    selectedIndex = if (selectedIndex == foundIndex) null else foundIndex
                                    selectedIndex?.let { idx ->
                                        onSliceSelected?.invoke(data.slices[idx])
                                    }
                                } else {
                                    selectedIndex = null
                                }
                            }
                        }
                ) {
                    val animProgress = animationProgress.value

                    val widthPx = size.width
                    val heightPx = size.height
                    val leftMarginPx = style.gridOptions.left.toPx()
                    val rightMarginPx = style.gridOptions.right.toPx()
                    val topMarginPx = style.gridOptions.top.toPx()
                    val bottomMarginPx = style.gridOptions.bottom.toPx()

                    val currentGridRect = Rect(
                        left = leftMarginPx,
                        top = topMarginPx,
                        right = widthPx - rightMarginPx,
                        bottom = heightPx - bottomMarginPx
                    )

                    // 在 Canvas 重绘时以最准确的 Canvas 物理尺寸进行计算以实现完美的状态响应动画
                    renderer.calculateSlices(
                        slices = data.slices,
                        hiddenList = hiddenSlices,
                        selectedIndex = selectedIndex,
                        selectedOffsetPx = currentOffsetPx,
                        animProgress = animProgress,
                        gridRect = currentGridRect
                    )

                    // 绘制扇区 Path
                    renderer.drawSlices(this)

                    // 绘制引导线与标签文字
                    renderer.drawLabelsAndLines(this, totalValue)
                }
            }

            // ================= 4. 环形图中心 Overlay Composable 容器 =================
            if (centerContent != null && style.pieOptions.innerRadiusRatio > 0f) {
                Box(
                    modifier = Modifier.align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    centerContent()
                }
            }
        }

        // ================= 5. 渲染底部图例 =================
        if (legendOptions.show && legendOptions.position == LegendPosition.Bottom) {
            RenderPieLegend(
                slices = data.slices,
                hiddenList = hiddenSlices,
                options = legendOptions,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * 渲染饼图专用的图例 FlowRow
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RenderPieLegend(
    slices: List<PieSlice>,
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
        slices.forEachIndexed { index, slice ->
            val isHidden = slice.name in hiddenList
            Row(
                modifier = Modifier
                    .padding(horizontal = options.itemGap / 2, vertical = 2.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(enabled = options.selectMode != LegendSelectMode.None) {
                        if (options.selectMode == LegendSelectMode.Multiple) {
                            if (isHidden) {
                                (hiddenList as MutableList<String>).remove(slice.name)
                            } else {
                                (hiddenList as MutableList<String>).add(slice.name)
                            }
                        } else if (options.selectMode == LegendSelectMode.Single) {
                            (hiddenList as MutableList<String>).clear()
                            slices.forEach {
                                if (it.name != slice.name) {
                                    (hiddenList as MutableList<String>).add(it.name)
                                }
                            }
                        }
                    }
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val indicatorColor = if (isHidden) Color.LightGray else slice.color
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
                    text = slice.name,
                    style = options.textStyle.copy(
                        color = options.textStyle.color.copy(alpha = textAlpha)
                    )
                )
            }
        }
    }
}
