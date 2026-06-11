package io.github.composechart.core.data

import androidx.compose.ui.geometry.Rect
import io.github.composechart.core.state.ViewportState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoordinateMapperTest {

    private val delta = 0.0001f

    @Test
    fun testBasicMapping() {
        // 设网格区域为 Left=10f, Top=20f, Right=110f, Bottom=120f (Width=100f, Height=100f)
        val gridRect = Rect(10f, 20f, 110f, 120f)
        // 视口数据范围为 [0, 100]
        val viewportState = ViewportState(0f, 100f, 0f, 100f)
        val mapper = CoordinateMapper(gridRect, viewportState)

        // 测试中心点
        assertEquals(60f, mapper.toScreenX(50f), delta)
        assertEquals(70f, mapper.toScreenY(50f), delta)

        // 测试边界点
        assertEquals(10f, mapper.toScreenX(0f), delta)
        assertEquals(120f, mapper.toScreenY(0f), delta)
        assertEquals(110f, mapper.toScreenX(100f), delta)
        assertEquals(20f, mapper.toScreenY(100f), delta)

        // 测试反向转换
        assertEquals(50f, mapper.toDataX(60f), delta)
        assertEquals(50f, mapper.toDataY(70f), delta)
        assertEquals(0f, mapper.toDataX(10f), delta)
        assertEquals(0f, mapper.toDataY(120f), delta)
        assertEquals(100f, mapper.toDataX(110f), delta)
        assertEquals(100f, mapper.toDataY(20f), delta)
    }

    @Test
    fun testExtremeZoomIn() {
        // 放大 100 倍（可见数据范围极为微小，宽度从 100 变成 1）
        val gridRect = Rect(10f, 20f, 110f, 120f)
        val viewportState = ViewportState(10f, 11f, 20f, 21f)
        val mapper = CoordinateMapper(gridRect, viewportState)

        // 中心数据映射
        assertEquals(60f, mapper.toScreenX(10.5f), delta)
        assertEquals(70f, mapper.toScreenY(20.5f), delta)

        // 反向映射精度
        assertEquals(10.5f, mapper.toDataX(60f), delta)
        assertEquals(20.5f, mapper.toDataY(70f), delta)
    }

    @Test
    fun testExtremeZoomOut() {
        // 缩小 100 倍 (宽度 10000)
        val gridRect = Rect(10f, 20f, 110f, 120f)
        val viewportState = ViewportState(-1000f, 9000f, -1000f, 9000f)
        val mapper = CoordinateMapper(gridRect, viewportState)

        // 中心数据映射
        assertEquals(60f, mapper.toScreenX(4000f), delta)
        assertEquals(70f, mapper.toScreenY(4000f), delta)

        // 反向映射精度
        assertEquals(4000f, mapper.toDataX(60f), delta)
        assertEquals(4000f, mapper.toDataY(70f), delta)
    }

    @Test
    fun testNegativeBoundsAndPan() {
        // 视口平移至负数范围：[-50, -10]
        val gridRect = Rect(10f, 20f, 110f, 120f)
        val viewportState = ViewportState(-50f, -10f, -50f, -10f)
        val mapper = CoordinateMapper(gridRect, viewportState)

        // 视口宽度为 40f
        // 数据点 -30f 是中心点，应该映射为网格物理像素中心 (60f, 70f)
        assertEquals(60f, mapper.toScreenX(-30f), delta)
        assertEquals(70f, mapper.toScreenY(-30f), delta)

        assertEquals(-30f, mapper.toDataX(60f), delta)
        assertEquals(-30f, mapper.toDataY(70f), delta)
    }

    @Test
    fun testOutofBoundsExtrapolation() {
        // 边界外推测试
        val gridRect = Rect(10f, 20f, 110f, 120f)
        val viewportState = ViewportState(0f, 100f, 0f, 100f)
        val mapper = CoordinateMapper(gridRect, viewportState)

        // 映射超出物理右边界 10px (即 120f)，对应数据应是 110f
        assertEquals(110f, mapper.toDataX(120f), delta)
        // 映射超出物理上边界 10px (即 10f)，对应 Y 轴往上，数据应是 110f
        assertEquals(110f, mapper.toDataY(10f), delta)

        // 同样，传入超出范围的数据，也应能外推出屏幕坐标
        assertEquals(120f, mapper.toScreenX(110f), delta)
        assertEquals(10f, mapper.toScreenY(110f), delta)
    }

    @Test
    fun testNullHandling() {
        val gridRect = Rect(10f, 20f, 110f, 120f)
        val viewportState = ViewportState(0f, 100f, 0f, 100f)
        val mapper = CoordinateMapper(gridRect, viewportState)

        assertNull(mapper.toScreenOffset(50f, null))
        assertEquals(60f, mapper.toScreenOffset(50f, 50f)!!.x, delta)
        assertEquals(70f, mapper.toScreenOffset(50f, 50f)!!.y, delta)
    }

    @Test
    fun testZeroDivisionSafety() {
        // 1. 视口可见长度 dx/dy = 0f 的极端异常情况
        val gridRect = Rect(10f, 20f, 110f, 120f)
        val viewportState = ViewportState(10f, 10f, 10f, 10f)
        val mapper = CoordinateMapper(gridRect, viewportState)

        // 应退化返回网格左下角点以保证界面安全不崩溃
        assertEquals(10f, mapper.toScreenX(10f), delta)
        assertEquals(120f, mapper.toScreenY(10f), delta)

        // 2. 物理网格宽/高度为 0 的异常情况
        val zeroRect = Rect(10f, 20f, 10f, 20f)
        val validViewport = ViewportState(0f, 100f, 0f, 100f)
        val zeroMapper = CoordinateMapper(zeroRect, validViewport)

        // 应退化返回数据最小值
        assertEquals(0f, zeroMapper.toDataX(15f), delta)
        assertEquals(0f, zeroMapper.toDataY(15f), delta)
    }
}
