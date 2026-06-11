package io.github.composechart.showroom.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.composechart.charts.bar3d.Bar3DChart
import io.github.composechart.charts.bar3d.Bar3DChartData
import io.github.composechart.charts.bar3d.Bar3DOptions
import io.github.composechart.charts.bar3d.Bar3DPoint
import io.github.composechart.core.style.ChartStyle
import io.github.composechart.core.style.TitleOptions

/**
 * Demo 16: 3D 柱体打卡热力图 (Bar3D - Punch Card)
 */
@Composable
fun DemoBar3DPunchCard(style: ChartStyle) {
    var yaw by remember { mutableFloatStateOf(-45f) }
    var pitch by remember { mutableFloatStateOf(30f) }
    var zoom by remember { mutableFloatStateOf(1.0f) }
    var barWidthRatio by remember { mutableFloatStateOf(0.6f) }
    
    var dataMultiplier by remember { mutableFloatStateOf(1.0f) }
    var punchData by remember { mutableStateOf(generateMockPunchCardData()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 3D 柱体容器
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
        ) {
            Bar3DChart(
                data = punchData,
                style = style.copy(
                    titleOptions = TitleOptions(
                        text = "Bar3D - 员工每日打卡热度图",
                        subtext = "24小时（X轴）* 7天（Y轴）三维柱状排布，支持单指拖动旋转及双指捏合缩放",
                        textStyle = style.titleOptions.textStyle.copy(fontSize = 15.sp)
                    )
                ),
                options = Bar3DOptions(
                    initialYaw = yaw,
                    initialPitch = pitch,
                    initialZoom = zoom,
                    barWidthRatio = barWidthRatio,
                    visualMapColors = listOf(
                        Color(0xFF313695), // 冰蓝
                        Color(0xFF4575B4),
                        Color(0xFF74ADD1),
                        Color(0xFFABD9E9),
                        Color(0xFFFFFFBF), // 鹅黄
                        Color(0xFFFEE090),
                        Color(0xFFFDAE61),
                        Color(0xFFF46D43),
                        Color(0xFFD73027), // 红
                        Color(0xFFA50026)  // 深红
                    )
                ),
                modifier = Modifier.fillMaxSize()
            )
        }

        // 交互按钮与控制
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Button(
                onClick = {
                    yaw = -45f
                    pitch = 30f
                    zoom = 1.0f
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5470C6)),
                modifier = Modifier.weight(1f)
            ) {
                Text("重置 3D 视角")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = {
                    punchData = generateMockPunchCardData()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3BA272)),
                modifier = Modifier.weight(1f)
            ) {
                Text("随机热度数据")
            }
        }

        ControlPanel {
            ControlSlider(
                label = "水平旋转偏航角 (Yaw)",
                value = yaw,
                valueRange = -180f..180f,
                onValueChange = { yaw = it },
                valueFormatter = { String.format("%.0f°", it) }
            )

            ControlSlider(
                label = "俯仰倾斜角 (Pitch)",
                value = pitch,
                valueRange = 10f..85f,
                onValueChange = { pitch = it },
                valueFormatter = { String.format("%.0f°", it) }
            )

            ControlSlider(
                label = "视角缩放倍率 (Zoom)",
                value = zoom,
                valueRange = 0.5f..2.5f,
                onValueChange = { zoom = it },
                valueFormatter = { String.format("%.2f 倍", it) }
            )

            ControlSlider(
                label = "柱体宽度占比 (barWidthRatio)",
                value = barWidthRatio,
                valueRange = 0.2f..0.85f,
                onValueChange = { barWidthRatio = it },
                valueFormatter = { String.format("%.2f 倍", it) }
            )
        }
    }
}

/**
 * 经典打卡打点数据生成 (24 小时 * 7 天)
 */
private fun generateMockPunchCardData(): Bar3DChartData {
    val xLabels = listOf(
        "12a", "1a", "2a", "3a", "4a", "5a", "6a", "7a", "8a", "9a", "10a", "11a",
        "12p", "1p", "2p", "3p", "4p", "5p", "6p", "7p", "8p", "9p", "10p", "11p"
    )
    val yLabels = listOf("周六", "周五", "周四", "周三", "周二", "周一", "周日")
    
    val points = mutableListOf<Bar3DPoint>()
    
    // 按高斯或余弦模拟打卡高峰（如中午 12 点与下午 6 点打卡频次最高，周六周日数值较低）
    for (y in 0 until 7) {
        for (x in 0 until 24) {
            val isWeekend = (y == 0 || y == 6)
            
            // 基础概率波动
            var baseZ = 0.5f + Math.random().toFloat() * 1.5f
            
            // 中午 11p - 1p 高峰
            if (x in 11..13) {
                baseZ += 4f + Math.random().toFloat() * 5f
            }
            // 下午 5p - 7p 高峰
            if (x in 17..19) {
                baseZ += 5f + Math.random().toFloat() * 6f
            }
            // 早晨上班打卡高峰 8a - 9a
            if (x in 8..9) {
                baseZ += 3f + Math.random().toFloat() * 4f
            }
            
            // 周末热度折让
            if (isWeekend) {
                baseZ *= 0.35f
            }
            
            val finalZ = baseZ.coerceIn(0f, 12f)
            points.add(Bar3DPoint(xIndex = x, yIndex = y, zValue = finalZ))
        }
    }
    
    return Bar3DChartData(
        xAxisLabels = xLabels,
        yAxisLabels = yLabels,
        points = points,
        zMin = 0f,
        zMax = 12f
    )
}
