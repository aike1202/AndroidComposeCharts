package io.github.composechart.core.util

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round

/**
 * 刻度划分计算结果
 */
data class AxisIntervalResult(
    val min: Float,
    val max: Float,
    val tickSpacing: Float,
    val ticks: List<Float>
)

/**
 * Nice Numbers 规整化步长划分计算器。
 * 能够将任意浮点数区间 [min, max] 自动规整为步长为 0.1, 0.2, 0.5, 1, 2, 5, 10 等具有高度易读性的等分区间。
 */
object AxisIntervalCalculator {

    /**
     * 根据输入的最大最小值及目标等分段数，计算规整后的步长、起点、终点和刻度列表。
     */
    fun calculate(min: Float, max: Float, splitNumber: Int = 5): AxisIntervalResult {
        if (splitNumber <= 0) {
            return AxisIntervalResult(min, max, 0f, emptyList())
        }

        // 极端情况：最大最小值完全一致（无跨度）
        if (min == max) {
            val spacing = if (min == 0f) 1f else Math.abs(min) * 0.2f
            val niceMin = min - spacing * (splitNumber / 2)
            val niceMax = min + spacing * (splitNumber - splitNumber / 2)
            val ticks = (0..splitNumber).map {
                roundToSpacing(niceMin + it * spacing, spacing)
            }
            return AxisIntervalResult(niceMin, niceMax, spacing, ticks)
        }

        val range = max - min
        val rawTickSpacing = range / splitNumber
        val exponent = floor(log10(rawTickSpacing.toDouble())).toFloat()
        val fraction = rawTickSpacing / 10f.pow(exponent)

        // 选择最贴近的 Nice 有效数
        val niceFraction = when {
            fraction < 1.5f -> 1.0f
            fraction < 3.0f -> 2.0f
            fraction < 7.0f -> 5.0f
            else -> 10.0f
        }

        val tickSpacing = niceFraction * 10f.pow(exponent)
        val niceMin = floor(min / tickSpacing) * tickSpacing
        val niceMax = ceil(max / tickSpacing) * tickSpacing

        val tickCount = round((niceMax - niceMin) / tickSpacing).toInt() + 1
        val ticks = (0 until tickCount).map {
            roundToSpacing(niceMin + it * tickSpacing, tickSpacing)
        }

        return AxisIntervalResult(niceMin, niceMax, tickSpacing, ticks)
    }

    /**
     * 强行计算与指定 ticks 数量 (分段数) 刚好对齐的 Nice 刻度结果。
     * 用于双 Y 轴图表中将副轴 (Y2) 刻度网格线与主轴 (Y1) 完美重合。
     * @param targetSegmentCount 目标分段数 (即 ticks.size - 1)
     */
    fun calculateWithSegmentCount(min: Float, max: Float, targetSegmentCount: Int): AxisIntervalResult {
        if (targetSegmentCount <= 0) {
            return calculate(min, max)
        }

        // 极端情况
        if (min == max) {
            val spacing = if (min == 0f) 1f else Math.abs(min) * 0.2f
            val niceMin = min - spacing * (targetSegmentCount / 2)
            val niceMax = niceMin + targetSegmentCount * spacing
            val ticks = (0..targetSegmentCount).map {
                roundToSpacing(niceMin + it * spacing, spacing)
            }
            return AxisIntervalResult(niceMin, niceMax, spacing, ticks)
        }

        val range = max - min
        val rawSpacing = range / targetSegmentCount
        val exponent = floor(log10(rawSpacing.toDouble())).toFloat()
        val fraction = rawSpacing / 10f.pow(exponent)

        // 定义规整有效数列表，供升档匹配使用
        val niceFractions = listOf(1.0f, 1.2f, 1.5f, 2.0f, 2.5f, 3.0f, 4.0f, 5.0f, 6.0f, 8.0f, 10.0f)

        // 寻找能覆盖 max 的最小 Nice 步长
        var selectedSpacing = 1.0f
        var success = false

        // 寻找当前量级 exponent 下的合适步长
        for (f in niceFractions) {
            val spacing = f * 10f.pow(exponent)
            val niceMin = floor(min / spacing) * spacing
            val niceMax = niceMin + targetSegmentCount * spacing
            if (niceMax >= max) {
                selectedSpacing = spacing
                success = true
                break
            }
        }

        // 如果当前量级无法覆盖（极罕见），升档到下一个 10 倍量级
        if (!success) {
            val nextSpacing = 1.0f * 10f.pow(exponent + 1)
            selectedSpacing = nextSpacing
        }

        val niceMin = floor(min / selectedSpacing) * selectedSpacing
        val niceMax = niceMin + targetSegmentCount * selectedSpacing
        val ticks = (0..targetSegmentCount).map {
            roundToSpacing(niceMin + it * selectedSpacing, selectedSpacing)
        }

        return AxisIntervalResult(niceMin, niceMax, selectedSpacing, ticks)
    }

    /**
     * 解决浮点累加造成的类似 0.7999999 等非规整浮点数问题，根据步长精度进行四舍五入。
     */
    private fun roundToSpacing(value: Float, spacing: Float): Float {
        if (spacing == 0f) return value
        // 计算应该保留的小数位数（取步长数量级的负值并增加 2 位冗余保护）
        val scale = 10f.pow(ceil(-log10(spacing.toDouble())).toInt().coerceAtLeast(0) + 2)
        return round(value * scale) / scale
    }
}
