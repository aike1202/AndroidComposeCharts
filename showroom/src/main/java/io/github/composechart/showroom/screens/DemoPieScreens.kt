package io.github.composechart.showroom.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.composechart.charts.pie.PieChart
import io.github.composechart.charts.pie.PieChartData
import io.github.composechart.charts.pie.PieSlice
import io.github.composechart.core.style.ChartStyle
import io.github.composechart.core.style.RoseType
import io.github.composechart.core.style.TitleOptions

/**
 * Demo 7: 基础实心饼图
 */
@Composable
fun DemoBasicPie(style: ChartStyle) {
    var padAngle by remember { mutableFloatStateOf(2f) }
    var cornerRadius by remember { mutableFloatStateOf(0f) }
    var selectedOffset by remember { mutableFloatStateOf(12f) }
    var showLabel by remember { mutableStateOf(true) }

    val data = PieChartData(
        slices = listOf(
            PieSlice("亚洲", 4700f, Color(0xFF5470C6)),
            PieSlice("非洲", 1400f, Color(0xFF91CC75)),
            PieSlice("欧洲", 740f, Color(0xFFFAC858)),
            PieSlice("北美洲", 600f, Color(0xFFEE6666)),
            PieSlice("南美洲", 430f, Color(0xFF73C0DE))
        )
    )

    val customStyle = style.copy(
        titleOptions = TitleOptions(
            text = "全球人口区域占比分布",
            subtext = "基础饼图模式，支持引出折线、防挤压排布与参数化微调",
            textStyle = style.titleOptions.textStyle.copy(fontSize = 16.sp)
        ),
        pieOptions = style.pieOptions.copy(
            roseType = RoseType.None,
            innerRadiusRatio = 0f,
            padAngle = padAngle,
            cornerRadius = cornerRadius.dp,
            selectedOffset = selectedOffset.dp,
            showLabel = showLabel
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
            PieChart(
                data = data,
                style = customStyle,
                modifier = Modifier.fillMaxSize()
            )
        }

        ControlPanel {
            ControlSwitch(label = "显示边缘引线与占比文本 (showLabel)", checked = showLabel, onCheckedChange = { showLabel = it })

            ControlSlider(
                label = "扇区夹角间隙大小 (padAngle)",
                value = padAngle,
                valueRange = 0f..10f,
                onValueChange = { padAngle = it },
                valueFormatter = { String.format("%.1f 度", it) }
            )

            ControlSlider(
                label = "扇区外沿圆角大小 (cornerRadius)",
                value = cornerRadius,
                valueRange = 0f..20f,
                onValueChange = { cornerRadius = it },
                valueFormatter = { String.format("%.0f dp", it) }
            )

            ControlSlider(
                label = "选中扇区凸起偏移量 (selectedOffset)",
                value = selectedOffset,
                valueRange = 0f..25f,
                onValueChange = { selectedOffset = it },
                valueFormatter = { String.format("%.0f dp", it) }
            )
        }
    }
}

/**
 * Demo 8: 空心环形图 (Doughnut)
 */
@Composable
fun DemoDoughnut(style: ChartStyle) {
    var innerRadiusRatio by remember { mutableFloatStateOf(0.55f) }
    var padAngle by remember { mutableFloatStateOf(3f) }
    var cornerRadius by remember { mutableFloatStateOf(0f) }
    var selectedOffset by remember { mutableFloatStateOf(10f) }

    val data = PieChartData(
        slices = listOf(
            PieSlice("Google", 85f, Color(0xFF5470C6)),
            PieSlice("Bing", 8f, Color(0xFFFAC858)),
            PieSlice("Baidu", 4f, Color(0xFF91CC75)),
            PieSlice("Yahoo", 2f, Color(0xFFEE6666)),
            PieSlice("Others", 1f, Color(0xFF73C0DE))
        )
    )

    val customStyle = style.copy(
        titleOptions = TitleOptions(
            text = "搜索引擎全球市场份额",
            subtext = "空心环形图，中心支持重叠 Compose 自定义视图呈现文本",
            textStyle = style.titleOptions.textStyle.copy(fontSize = 16.sp)
        ),
        pieOptions = style.pieOptions.copy(
            roseType = RoseType.None,
            innerRadiusRatio = innerRadiusRatio,
            padAngle = padAngle,
            cornerRadius = cornerRadius.dp,
            selectedOffset = selectedOffset.dp
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
            PieChart(
                data = data,
                style = customStyle,
                modifier = Modifier.fillMaxSize()
            ) {
                // 环形中心自定义布局
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val isDark = style.backgroundColor != Color.Transparent && style.backgroundColor.red < 0.5f
                    Text(
                        text = "SEARCH",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF999999) else Color(0xFF666666)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "100%",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color.White else Color.Black
                    )
                }
            }
        }

        ControlPanel {
            ControlSlider(
                label = "空心内圆半径占比 (innerRadiusRatio)",
                value = innerRadiusRatio,
                valueRange = 0.2f..0.85f,
                onValueChange = { innerRadiusRatio = it },
                valueFormatter = { String.format("%.2f", it) }
            )

            ControlSlider(
                label = "环形扇区空隙角度 (padAngle)",
                value = padAngle,
                valueRange = 0f..8f,
                onValueChange = { padAngle = it },
                valueFormatter = { String.format("%.1f 度", it) }
            )

            ControlSlider(
                label = "环形扇区端点圆角 (cornerRadius)",
                value = cornerRadius,
                valueRange = 0f..16f,
                onValueChange = { cornerRadius = it },
                valueFormatter = { String.format("%.0f dp", it) }
            )
        }
    }
}

/**
 * Demo 9: 南丁格尔玫瑰图 (Nightingale Chart)
 * 经典 Radius 与 Area 双玫瑰对比图
 */
@Composable
fun DemoNightingaleRose(style: ChartStyle) {
    var innerRadiusRatio by remember { mutableFloatStateOf(0.2f) }
    var cornerRadius by remember { mutableFloatStateOf(8f) }
    var padAngle by remember { mutableFloatStateOf(0f) }

    val data = PieChartData(
        slices = listOf(
            PieSlice("rose 1", 40f, Color(0xFF5470C6)),
            PieSlice("rose 2", 38f, Color(0xFF91CC75)),
            PieSlice("rose 3", 32f, Color(0xFFFAC858)),
            PieSlice("rose 4", 30f, Color(0xFFEE6666)),
            PieSlice("rose 5", 28f, Color(0xFF73C0DE)),
            PieSlice("rose 6", 26f, Color(0xFF3BA272)),
            PieSlice("rose 7", 22f, Color(0xFFFC8452)),
            PieSlice("rose 8", 18f, Color(0xFF9A60B4))
        )
    )

    val customStyle = style.copy(
        titleOptions = TitleOptions(
            show = false // 手动统一绘制居中标题
        )
    )

    val leftStyle = customStyle.copy(
        pieOptions = style.pieOptions.copy(
            roseType = RoseType.Area, // 角度按比例，半径按比例 (ECharts 里的 'radius')
            innerRadiusRatio = innerRadiusRatio,
            cornerRadius = cornerRadius.dp,
            padAngle = padAngle,
            showLabel = false // 左图不显示文字及引导线
        )
    )

    val rightStyle = customStyle.copy(
        pieOptions = style.pieOptions.copy(
            roseType = RoseType.Radius, // 角度等分，半径按比例 (ECharts 里的 'area')
            innerRadiusRatio = innerRadiusRatio,
            cornerRadius = cornerRadius.dp,
            padAngle = padAngle,
            showLabel = true // 右图显示文字及引导线
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 渲染对标 ECharts 的统一居中大标题
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val isDark = style.backgroundColor != Color.Transparent && style.backgroundColor.red < 0.5f
            Text(
                text = "Nightingale Chart",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color.Black
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Fake Data",
                fontSize = 12.sp,
                color = if (isDark) Color(0xFF999999) else Color(0xFF666666)
            )
        }

        // 左右并排渲染双玫瑰图
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .padding(horizontal = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {
                PieChart(
                    data = data,
                    style = leftStyle,
                    legendOptions = io.github.composechart.core.style.LegendOptions(show = false),
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .weight(1.2f) // 分配稍微多一点宽度防标签遮挡
                    .fillMaxSize()
            ) {
                PieChart(
                    data = data,
                    style = rightStyle,
                    legendOptions = io.github.composechart.core.style.LegendOptions(show = false),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        ControlPanel {
            ControlSlider(
                label = "玫瑰扇区端点圆角 (cornerRadius)",
                value = cornerRadius,
                valueRange = 0f..16f,
                onValueChange = { cornerRadius = it },
                valueFormatter = { String.format("%.0f dp", it) }
            )

            ControlSlider(
                label = "空心底座孔径比 (innerRadiusRatio)",
                value = innerRadiusRatio,
                valueRange = 0.0f..0.6f,
                onValueChange = { innerRadiusRatio = it },
                valueFormatter = { String.format("%.2f", it) }
            )

            ControlSlider(
                label = "扇区缝隙角度 (padAngle)",
                value = padAngle,
                valueRange = 0f..6f,
                onValueChange = { padAngle = it },
                valueFormatter = { String.format("%.1f 度", it) }
            )
        }
    }
}

/**
 * Demo 9_2: 基础南丁格尔玫瑰图 (Basic Nightingale Chart)
 * 经典角度等分的单玫瑰图，带有底部图例
 */
@Composable
fun DemoBasicNightingaleRose(style: ChartStyle) {
    var innerRadiusRatio by remember { mutableFloatStateOf(0.2f) }
    var cornerRadius by remember { mutableFloatStateOf(8f) }
    var padAngle by remember { mutableFloatStateOf(0f) }

    val data = PieChartData(
        slices = listOf(
            PieSlice("rose 1", 40f, Color(0xFF5470C6)),
            PieSlice("rose 2", 38f, Color(0xFF91CC75)),
            PieSlice("rose 3", 32f, Color(0xFFFAC858)),
            PieSlice("rose 4", 30f, Color(0xFFEE6666)),
            PieSlice("rose 5", 28f, Color(0xFF73C0DE)),
            PieSlice("rose 6", 26f, Color(0xFF3BA272)),
            PieSlice("rose 7", 22f, Color(0xFFFC8452)),
            PieSlice("rose 8", 18f, Color(0xFF9A60B4))
        )
    )

    val customStyle = style.copy(
        titleOptions = TitleOptions(
            show = false
        ),
        pieOptions = style.pieOptions.copy(
            roseType = RoseType.Radius, // 角度等分，半径按比例
            innerRadiusRatio = innerRadiusRatio,
            cornerRadius = cornerRadius.dp,
            padAngle = padAngle,
            showLabel = true
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 统一居中大标题
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val isDark = style.backgroundColor != Color.Transparent && style.backgroundColor.red < 0.5f
            Text(
                text = "Basic Nightingale Chart",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color.Black
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Fake Data",
                fontSize = 12.sp,
                color = if (isDark) Color(0xFF999999) else Color(0xFF666666)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
        ) {
            PieChart(
                data = data,
                style = customStyle,
                legendOptions = io.github.composechart.core.style.LegendOptions(
                    show = true,
                    position = io.github.composechart.core.style.LegendPosition.Bottom,
                    alignment = Alignment.CenterHorizontally,
                    selectMode = io.github.composechart.core.style.LegendSelectMode.Multiple
                ),
                modifier = Modifier.fillMaxSize()
            )
        }

        ControlPanel {
            ControlSlider(
                label = "玫瑰扇区端点圆角 (cornerRadius)",
                value = cornerRadius,
                valueRange = 0f..16f,
                onValueChange = { cornerRadius = it },
                valueFormatter = { String.format("%.0f dp", it) }
            )

            ControlSlider(
                label = "空心底座孔径比 (innerRadiusRatio)",
                value = innerRadiusRatio,
                valueRange = 0.0f..0.6f,
                onValueChange = { innerRadiusRatio = it },
                valueFormatter = { String.format("%.2f", it) }
            )

            ControlSlider(
                label = "扇区缝隙角度 (padAngle)",
                value = padAngle,
                valueRange = 0f..6f,
                onValueChange = { padAngle = it },
                valueFormatter = { String.format("%.1f 度", it) }
            )
        }
    }
}
