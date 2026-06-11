package io.github.composechart.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class AxisIntervalCalculatorTest {

    private val delta = 0.0001f

    @Test
    fun testIntegerInterval() {
        val result = AxisIntervalCalculator.calculate(0f, 100f, 5)

        assertEquals(0f, result.min, delta)
        assertEquals(100f, result.max, delta)
        assertEquals(20f, result.tickSpacing, delta)
        assertEquals(6, result.ticks.size)
        assertEquals(0f, result.ticks[0], delta)
        assertEquals(20f, result.ticks[1], delta)
        assertEquals(40f, result.ticks[2], delta)
        assertEquals(60f, result.ticks[3], delta)
        assertEquals(80f, result.ticks[4], delta)
        assertEquals(100f, result.ticks[5], delta)
    }

    @Test
    fun testFloatInterval() {
        val result = AxisIntervalCalculator.calculate(0.1f, 1.0f, 5)

        // 跨度 0.9f，期望 5 段，原始步长 0.18f
        // 指数部分 exponent = -1 (即 10^-1 = 0.1)，有效数部分 fraction = 1.8f
        // niceFraction = 2.0f，因此最终步长 niceSpacing = 2.0f * 0.1f = 0.2f
        // niceMin = floor(0.1 / 0.2) * 0.2 = 0f
        // niceMax = ceil(1.0 / 0.2) * 0.2 = 1.0f
        assertEquals(0f, result.min, delta)
        assertEquals(1.0f, result.max, delta)
        assertEquals(0.2f, result.tickSpacing, delta)
        assertEquals(6, result.ticks.size)
        assertEquals(0f, result.ticks[0], delta)
        assertEquals(0.2f, result.ticks[1], delta)
        assertEquals(0.4f, result.ticks[2], delta)
        assertEquals(0.6f, result.ticks[3], delta)
        assertEquals(0.8f, result.ticks[4], delta)
        assertEquals(1.0f, result.ticks[5], delta)
    }

    @Test
    fun testZeroCrossBound() {
        // min == max == 50f
        val result = AxisIntervalCalculator.calculate(50f, 50f, 5)

        // spacing = 50 * 0.2 = 10f
        // niceMin = 50f - 10f * 2 = 30f
        // niceMax = 50f + 10f * 3 = 80f
        assertEquals(30f, result.min, delta)
        assertEquals(80f, result.max, delta)
        assertEquals(10f, result.tickSpacing, delta)
        assertEquals(6, result.ticks.size)
        assertEquals(30f, result.ticks[0], delta)
        assertEquals(40f, result.ticks[1], delta)
        assertEquals(50f, result.ticks[2], delta)
        assertEquals(60f, result.ticks[3], delta)
        assertEquals(70f, result.ticks[4], delta)
        assertEquals(80f, result.ticks[5], delta)

        // min == max == 0f
        val zeroResult = AxisIntervalCalculator.calculate(0f, 0f, 5)
        assertEquals(-2f, zeroResult.min, delta)
        assertEquals(3f, zeroResult.max, delta)
        assertEquals(1f, zeroResult.tickSpacing, delta)
    }

    @Test
    fun testNegativeInterval() {
        val result = AxisIntervalCalculator.calculate(-35f, -5f, 3)

        // 跨度 30f，期望 3 段，步长 10f
        // niceMin = -40f, niceMax = 0f
        assertEquals(-40f, result.min, delta)
        assertEquals(0f, result.max, delta)
        assertEquals(10f, result.tickSpacing, delta)
        assertEquals(5, result.ticks.size)
        assertEquals(-40f, result.ticks[0], delta)
        assertEquals(-30f, result.ticks[1], delta)
        assertEquals(-20f, result.ticks[2], delta)
        assertEquals(-10f, result.ticks[3], delta)
        assertEquals(0f, result.ticks[4], delta)
    }
}
