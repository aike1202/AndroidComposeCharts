package io.github.composechart.core.plot

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.composechart.core.data.CoordinateMapper
import io.github.composechart.core.style.ChartStyle
import io.github.composechart.core.style.IndicatorStyle
import io.github.composechart.core.style.TooltipOptions
import io.github.composechart.core.style.TriggerType

/**
 * Tooltip 浮窗项数据载体
 */
data class TooltipInfo(
    val title: String,
    val items: List<TooltipItem>
)

data class TooltipItem(
    val seriesName: String,
    val value: String,
    val color: Color
)

/**
 * Canvas 直绘 Tooltip 组件，负责辅助光标十字线、数据发光指示点、悬浮文字卡片的碰撞检测与渲染。
 */
class TooltipRenderer(
    private val textMeasurer: TextMeasurer,
    private val style: ChartStyle,
    private val tooltipOptions: TooltipOptions
) {

    /**
     * 执行具体的 Canvas 绘制。
     *
     * @param drawScope 绘图 Scope
     * @param mapper 物理像素转换映射器
     * @param tooltipInfo 卡片需要绘制的数据信息
     * @param indicatorPoints 各个系列当前扫掠点的物理像素坐标列表（发光原点位置）
     * @param touchOffset 当前触控的物理像素坐标
     */
    fun draw(
        drawScope: DrawScope,
        mapper: CoordinateMapper,
        tooltipInfo: TooltipInfo,
        indicatorPoints: List<Offset>,
        touchOffset: Offset,
        itemSpacing: Float = 0f,
        horizontal: Boolean = false
    ) = with(drawScope) {
        if (!tooltipOptions.enabled) return@with

        val grid = mapper.gridRect

        // 确定十字辅助线的 X 轴坐标（磁吸对齐到首个指示点 X 轴上，无指示点则使用触控坐标 X）
        val sweepX = if (indicatorPoints.isNotEmpty()) {
            indicatorPoints.first().x.coerceIn(grid.left, grid.right)
        } else {
            touchOffset.x.coerceIn(grid.left, grid.right)
        }

        // ================= 1. 绘制垂直/水平指示光标辅助线 =================
        if (tooltipOptions.trigger == TriggerType.Axis) {
            if (tooltipOptions.indicatorStyle == IndicatorStyle.Shadow && itemSpacing > 0f) {
                // 绘制柱状图类目块阴影背景区
                if (!horizontal) {
                    drawRect(
                        color = tooltipOptions.indicatorColor,
                        topLeft = Offset(sweepX - itemSpacing / 2f, grid.top),
                        size = Size(itemSpacing, grid.height)
                    )
                } else {
                    val sweepY = if (indicatorPoints.isNotEmpty()) indicatorPoints.first().y else touchOffset.y
                    drawRect(
                        color = tooltipOptions.indicatorColor,
                        topLeft = Offset(grid.left, sweepY - itemSpacing / 2f),
                        size = Size(grid.width, itemSpacing)
                    )
                }
            } else {
                val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

                // 1.1 垂直虚线
                drawLine(
                    color = tooltipOptions.indicatorColor,
                    start = Offset(sweepX, grid.top),
                    end = Offset(sweepX, grid.bottom),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = pathEffect
                )

                // 1.2 如果是十字光标，绘制水平虚线
                if (tooltipOptions.indicatorStyle == IndicatorStyle.Cross) {
                    val sweepY = touchOffset.y.coerceIn(grid.top, grid.bottom)
                    drawLine(
                        color = tooltipOptions.indicatorColor,
                        start = Offset(grid.left, sweepY),
                        end = Offset(grid.right, sweepY),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = pathEffect
                    )
                }
            }
        }

        // ================= 2. 绘制多层发光指示圆点 =================
        for (i in indicatorPoints.indices) {
            val pt = indicatorPoints[i]
            val item = tooltipInfo.items.getOrNull(i) ?: continue
            if (pt.x in (grid.left - 1f)..(grid.right + 1f) &&
                pt.y in (grid.top - 1f)..(grid.bottom + 1f)
            ) {
                // 第一层：最外层半透明发光环
                drawCircle(
                    color = item.color.copy(alpha = 0.25f),
                    radius = 7.dp.toPx(),
                    center = pt
                )
                // 第二层：中间白底实心隔离圈
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = pt
                )
                // 第三层：最内层数据系列实色点
                drawCircle(
                    color = item.color,
                    radius = 2.5.dp.toPx(),
                    center = pt
                )
            }
        }

        // ================= 3. 气泡尺寸物理测量 =================
        // 测量标题（通常加粗）
        val titleLayout = textMeasurer.measure(
            text = tooltipInfo.title,
            style = tooltipOptions.textStyle.copy(fontWeight = FontWeight.Bold)
        )
        // 测量项目行
        val itemLayouts = tooltipInfo.items.map { item ->
            textMeasurer.measure(
                text = "   ${item.seriesName}: ${item.value}", // 前留空位绘制彩色圆圈
                style = tooltipOptions.textStyle
            )
        }

        val padX = 12.dp.toPx()
        val padY = 10.dp.toPx()
        val lineGap = 4.dp.toPx()

        val maxItemWidth = itemLayouts.maxOfOrNull { it.size.width } ?: 0
        val bubbleWidth = maxOf(titleLayout.size.width, maxItemWidth) + padX * 2f + 8.dp.toPx()

        val titleHeight = titleLayout.size.height
        val itemsHeightSum = itemLayouts.sumOf { it.size.height }
        val itemGaps = if (itemLayouts.isNotEmpty()) (itemLayouts.size) * lineGap else 0f
        val bubbleHeight = titleHeight + itemsHeightSum + itemGaps + padY * 2f + 4.dp.toPx()

        // ================= 4. 碰撞逃逸计算 =================
        // 气泡默认画在指示线右方 12.dp 处，垂直方向以触摸 Y 为中心对齐
        var drawX = sweepX + 12.dp.toPx()
        var drawY = touchOffset.y.coerceIn(grid.top, grid.bottom) - bubbleHeight / 2f

        // 右边溢出碰撞检测 -> 翻转至左侧绘制
        if (drawX + bubbleWidth > grid.right) {
            drawX = sweepX - bubbleWidth - 12.dp.toPx()
        }
        // 左边界防越界保护
        if (drawX < grid.left + 4.dp.toPx()) {
            drawX = grid.left + 4.dp.toPx()
        }

        // 上下越界夹逼限制
        if (drawY < grid.top + 4.dp.toPx()) {
            drawY = grid.top + 4.dp.toPx()
        }
        if (drawY + bubbleHeight > grid.bottom - 4.dp.toPx()) {
            drawY = grid.bottom - bubbleHeight - 4.dp.toPx()
        }

        // ================= 5. 渲染发光气泡卡片 =================
        // 5.1 画投影层（向下方偏移 2dp 的极淡黑影，表现微立体浮空质感）
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.08f),
            topLeft = Offset(drawX + 2.dp.toPx(), drawY + 2.dp.toPx()),
            size = Size(bubbleWidth, bubbleHeight),
            cornerRadius = CornerRadius(tooltipOptions.cornerRadius.toPx(), tooltipOptions.cornerRadius.toPx())
        )
        // 5.2 画毛玻璃质感背景卡片
        drawRoundRect(
            color = tooltipOptions.backgroundColor,
            topLeft = Offset(drawX, drawY),
            size = Size(bubbleWidth, bubbleHeight),
            cornerRadius = CornerRadius(tooltipOptions.cornerRadius.toPx(), tooltipOptions.cornerRadius.toPx())
        )
        // 5.3 画边框
        drawRoundRect(
            color = tooltipOptions.borderColor,
            topLeft = Offset(drawX, drawY),
            size = Size(bubbleWidth, bubbleHeight),
            cornerRadius = CornerRadius(tooltipOptions.cornerRadius.toPx(), tooltipOptions.cornerRadius.toPx()),
            style = Stroke(width = tooltipOptions.borderWidth.toPx())
        )

        // ================= 6. 渲染内容及文本 =================
        // 6.1 渲染标题
        drawText(
            textLayoutResult = titleLayout,
            topLeft = Offset(drawX + padX, drawY + padY)
        )

        // 6.2 渲染数据行列表
        var currentY = drawY + padY + titleHeight + lineGap * 1.5f
        for (i in itemLayouts.indices) {
            val layout = itemLayouts[i]
            val item = tooltipInfo.items[i]

            // 在行首偏右空隙处绘制带色圆圈，大小与文本垂直中心对齐
            val dotCenter = Offset(
                drawX + padX + 5.dp.toPx(),
                currentY + layout.size.height / 2f
            )
            drawCircle(
                color = item.color,
                radius = 3.5.dp.toPx(),
                center = dotCenter
            )

            // 绘制数据行文本
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(drawX + padX, currentY)
            )

            currentY += layout.size.height + lineGap
        }
    }
}
