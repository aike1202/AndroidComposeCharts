package io.github.composechart.showroom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.composechart.core.style.ChartStyle
import io.github.composechart.showroom.screens.*
import io.github.composechart.showroom.ui.theme.ComposeChartTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var isDarkTheme by remember { mutableStateOf(false) }

            ComposeChartTheme(darkTheme = isDarkTheme) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ShowroomApp(
                        isDark = isDarkTheme,
                        onThemeToggle = { isDarkTheme = !isDarkTheme },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

/**
 * 屏幕路由定义
 */
sealed class Screen {
    object Home : Screen()
    data class Detail(val chartType: ChartType) : Screen()
}

/**
 * 图表类型定义与信息配置
 */
enum class ChartType(
    val title: String,
    val subtext: String,
    val category: String,
    val indicatorColor: Color
) {
    LINE_TEMP("温度变化趋势", "24小时平滑渐变折线面积图", "折线图系列", Color(0xFFFAC858)),
    LINE_MULTI("流量对比看板", "多系列虚实线对比与平均值/极值标记", "折线图系列", Color(0xFF5470C6)),
    LINE_BIG("密集海量测试", "1000+ 密集点二分二分裁剪高性能滑放", "折线图系列", Color(0xFF73C0DE)),
    LINE_STACKED("折线图堆叠", "折线数据按系列向上累加堆叠对比", "折线图系列", Color(0xFFFAC858)),
    LINE_STACKED_AREA("堆叠面积图", "多层面积图堆叠填充，展现总量与占比", "折线图系列", Color(0xFF5470C6)),
    LINE_GRADIENT_STACKED_AREA("渐变堆叠面积", "渐变色彩堆叠面积，对标ECharts高颜值视觉", "折线图系列", Color(0xFFEE6666)),
    LINE_CONFIDENCE_BAND("置信区间带图", "主折线环绕半透明置信阴影，展示误差包络", "折线图系列", Color(0xFF73C0DE)),
    
    BAR_COMPARE("双柱并列对比", "对比并列柱体、高颜值渐变与阴影槽", "柱状图系列", Color(0xFF5470C6)),
    BAR_STACK("正负双向堆叠", "四季度运营成本与财务营收多层堆叠", "柱状图系列", Color(0xFF3BA272)),
    BAR_HORIZONTAL("空气质量排名", "水平条形图旋转映射及数据滑块", "柱状图系列", Color(0xFF73C0DE)),
    
    PIE_BASIC("全球人口占比", "经典实心饼图及防重叠引线标注", "饼图系列", Color(0xFF5470C6)),
    PIE_DOUGHNUT("搜索引擎份额", "空心环形图及中心叠加卡片", "饼图系列", Color(0xFFFAC858)),
    PIE_ROSE("南丁格尔玫瑰图", "并排双玫瑰对比（角度等分与比例分配）", "饼图系列", Color(0xFFEE6666)),
    PIE_ROSE_BASIC("基础南丁格尔玫瑰图", "单个大玫瑰图，经典角度等分放射展示", "饼图系列", Color(0xFFFAC858)),
    PIE_BASIC_ACCESS("某站点用户Access From", "经典实心饼图及折线标签排版", "饼图系列", Color(0xFF5470C6)),
    PIE_ROUNDED_DOUGHNUT("圆角环形图", "环形首尾呈半圆胶囊状圆滑端点", "饼图系列", Color(0xFF91CC75)),
    PIE_DOUGHNUT_BASIC("环形图", "空心环形图，经典分类占比展示", "饼图系列", Color(0xFFFAC858)),
    PIE_HALF_DOUGHNUT("半环形占比图", "180度半圆弧展示，适用于占比关键指标", "饼图系列", Color(0xFFFAC858)),
    PIE_PAD_ANGLE("饼图扇区间隙", "扇区之间带有平行等宽背景分割缝隙", "饼图系列", Color(0xFFEE6666)),
    
    RADAR("战力对比雷达", "Polygon/Circle 多层同心网格对比", "高级特异图表", Color(0xFFEE6666)),
    KLINE("个股行情日K", "蜡烛图实体及下挂成交量联动锁定", "高级特异图表", Color(0xFFFF8A45)),
    SCATTER("告警监控散点", "GDP气泡映射与故障点涟漪告警特效", "高级特异图表", Color(0xFF91CC75)),
    GAUGE("车速安全仪表", "高性能车速刻度与烈焰红色指针", "高级特异图表", Color(0xFFFF003C)),
    GAUGE_GRADE("等级分类仪表", "等级分类D/C/B/A及悬空短三角指针", "高级特异图表", Color(0xFFFAC858)),
    GAUGE_TEMPERATURE("气温测量仪表", "气温进度弧模式，圆角端点无指针", "高级特异图表", Color(0xFFFF6B6B)),
    CALENDAR_BASIC("基础日历热力图", "GitHub 贡献墙绿度步数热力渲染及滑动数据控制", "高级特异图表", Color(0xFF3BA272)),
    CALENDAR_VERTICAL("纵向日历图", "纵向排布一整年每日数据打卡与红黄渐变热度", "高级特异图表", Color(0xFFEE6666)),
    CALENDAR_LUNAR("农历自定义日历", "日历格内置文字标注（初一、春节等自定义徽标）", "高级特异图表", Color(0xFFFAC858)),
    BAR3D_PUNCH_CARD("3D 柱状打卡图", "三维网格空间、平行光影着色与拖动视角打卡热度", "高级特异图表", Color(0xFF73C0DE)),
    FUNNEL("流失转化漏斗", "用户漏斗流失率标记及倒置金字塔", "高级特异图表", Color(0xFF5470C6)),
    BOXPLOT("工艺缺陷箱形", "多系列并列，五数大观与异常值标记", "高级特异图表", Color(0xFF91CC75)),
    MIXED_LINE_BAR("雨量蒸发混合", "双 Y 轴折线柱状混合与刻度重合对齐", "高级特异图表", Color(0xFF5470C6)),
    LINE_MULTIPLE_X_AXES("双 X 轴雨量对比", "上下两套 X 类目轴绑定不同降水年份系列", "折线图系列", Color(0xFF5470C6)),
    LINE_STEP("阶梯折线图", "折线在拐角处呈 start/middle/end 阶段跳跃", "折线图系列", Color(0xFF91CC75)),
    LINE_RACE("动态排序赛跑", "随着时间轴向前滚动推进，折线赛跑及终点标签贴附", "折线图系列", Color(0xFFEE6666)),
    LINE_FUNCTION_PLOT("数学函数绘图", "X/Y均为数值轴，且零值十字中心原点相交", "折线图系列", Color(0xFF73C0DE)),
    LINE_BUMP("排名消长凹凸图", "Y轴反转且圆点内置名次序号，展示排名变化", "折线图系列", Color(0xFFFAC858)),
    LINE_MARK_AREA("警戒分带与高峰标注", "横向分带背景标注及纵向时间用电区间标注", "折线图系列", Color(0xFFEE6666)),
    
    BAR_NEGATIVE_VALUE("交错正负轴条形图", "零轴分类标签动态错位避让", "柱状图系列", Color(0xFF5470C6)),
    BAR_WATERFALL("开支瀑布图", "首尾闭合中段悬空的瀑布图模拟", "柱状图系列", Color(0xFF91CC75)),
    BAR_GRADIENT_ZOOM("发光渐变与点击聚焦", "柱体带阴影及渐变，点击类目轴自动缩放", "柱状图系列", Color(0xFFFAC858)),
    POLAR_BAR_RADIAL("极坐标径向扇形图", "不闭合极坐标网格下径向排版标签的柱状图", "极坐标系", Color(0xFF73C0DE)),
    POLAR_BAR_TANGENTIAL("极坐标切向圆环图", "极轴同心轨道上切向排列标签的圆环图", "极坐标系", Color(0xFF3BA272)),
    DOC_SCREENSHOTS("文档截图生成器", "对照 GUIDE.md 的 13 类图表一键生成纯图表截图", "文档工具", Color(0xFF67C23A))
}

@Composable
fun ShowroomApp(
    isDark: Boolean,
    onThemeToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    val homeGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    val backgroundColor = if (isDark) Color(0xFF131314) else Color(0xFFF8F9FA)

    // 自动截图控制状态
    var autoScreenshotIndex by remember { mutableStateOf<Int?>(null) }
    var isAutoRunning by remember { mutableStateOf(false) }
    val view = androidx.compose.ui.platform.LocalView.current
    val context = androidx.compose.ui.platform.LocalContext.current

    // 系统返回键拦截
    if (currentScreen is Screen.Detail) {
        BackHandler {
            if (isAutoRunning) {
                isAutoRunning = false
                autoScreenshotIndex = null
            }
            currentScreen = Screen.Home
        }
    }

    // 自动轮播截图驱动器
    androidx.compose.runtime.LaunchedEffect(autoScreenshotIndex, isAutoRunning, isDark) {
        val index = autoScreenshotIndex
        if (isAutoRunning && index != null) {
            val chartTypes = ChartType.values()
            if (index < chartTypes.size) {
                val chartType = chartTypes[index]
                // 1. 切换到该详情页
                currentScreen = Screen.Detail(chartType)
                // 2. 延迟 1500ms，等待图表动画与渲染完全加载完毕
                kotlinx.coroutines.delay(1500)
                // 3. 执行截图
                val themeSuffix = if (isDark) "_dark" else "_light"
                val fileName = "${chartType.name.lowercase()}$themeSuffix"
                ScreenshotHelper.captureAndSave(view, context, fileName) { success, _ ->
                    // 无论成功与否，继续推进到下一张
                    val nextIndex = index + 1
                    if (nextIndex < chartTypes.size) {
                        autoScreenshotIndex = nextIndex
                    } else {
                        // 结束自动轮播
                        autoScreenshotIndex = null
                        isAutoRunning = false
                        currentScreen = Screen.Home
                        android.widget.Toast.makeText(context, "🎉 所有图表截图已自动生成完成！", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                autoScreenshotIndex = null
                isAutoRunning = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // TopBar 顶栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentScreen is Screen.Detail) {
                IconButton(onClick = {
                    if (isAutoRunning) {
                        isAutoRunning = false
                        autoScreenshotIndex = null
                    }
                    currentScreen = Screen.Home
                }) {
                    Text(
                        text = "◀",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color.Black
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(12.dp))
            }

            Text(
                text = when (val s = currentScreen) {
                    Screen.Home -> "ComposeChart 演示大厅"
                    is Screen.Detail -> s.chartType.title
                },
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color.Black
                )
            )
            Spacer(modifier = Modifier.weight(1f))

            // 按钮操作区
            if (currentScreen is Screen.Home) {
                // 主页：提供自动截图按钮
                val totalCount = ChartType.values().size
                val btnText = if (isAutoRunning && autoScreenshotIndex != null) {
                    "停止 (${autoScreenshotIndex!! + 1}/$totalCount)"
                } else {
                    "自动截图"
                }

                Button(
                    onClick = {
                        if (isAutoRunning) {
                            isAutoRunning = false
                            autoScreenshotIndex = null
                            currentScreen = Screen.Home
                        } else {
                            autoScreenshotIndex = 0
                            isAutoRunning = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAutoRunning) Color(0xFFEE6666) else Color(0xFFE6A23C)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(text = btnText, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
            } else if (currentScreen is Screen.Detail && !isAutoRunning) {
                // 详情页且非自动轮播时：提供手动保存当前截图按钮
                val s = currentScreen as Screen.Detail
                Button(
                    onClick = {
                        val themeSuffix = if (isDark) "_dark" else "_light"
                        val fileName = "${s.chartType.name.lowercase()}$themeSuffix"
                        ScreenshotHelper.captureAndSave(view, context, fileName)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF67C23A)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(text = "保存截图", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Button(
                onClick = onThemeToggle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color(0xFF5470C6) else Color(0xFF333333)
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(text = if (isDark) "切换亮色" else "切换暗色", fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }


        // 分发渲染主界面或详情页
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val chartStyle = if (isDark) ChartStyle.Dark else ChartStyle.Light

            val isHome = currentScreen is Screen.Home

            // 1. 始终渲染主列表并保留在组合树中，通过 alpha 控制可见度以物理保留其状态
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = if (isHome) 1f else 0f
                    }
            ) {
                HomeListScreen(
                    isDark = isDark,
                    state = homeGridState,
                    onItemClick = { currentScreen = Screen.Detail(it) }
                )
            }

            // 2. 详情页作为顶层叠加渲染，防止点击穿透到底层列表项
            if (currentScreen is Screen.Detail) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundColor)
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null,
                            onClick = {} // 拦截点击
                        )
                ) {
                    val s = currentScreen as Screen.Detail
                    when (s.chartType) {
                        ChartType.LINE_TEMP -> DemoTemperature(style = chartStyle)
                        ChartType.LINE_MULTI -> DemoMultiSeries(style = chartStyle)
                        ChartType.LINE_BIG -> DemoBigData(style = chartStyle)
                        ChartType.LINE_STACKED -> DemoStackedLine(style = chartStyle)
                        ChartType.LINE_STACKED_AREA -> DemoStackedArea(style = chartStyle)
                        ChartType.LINE_GRADIENT_STACKED_AREA -> DemoGradientStackedArea(style = chartStyle)
                        ChartType.LINE_CONFIDENCE_BAND -> DemoConfidenceBand(style = chartStyle)
                        ChartType.BAR_COMPARE -> DemoBarComparison(style = chartStyle)
                        ChartType.BAR_STACK -> DemoBarStack(style = chartStyle)
                        ChartType.BAR_HORIZONTAL -> DemoHorizontalBar(style = chartStyle)
                        ChartType.PIE_BASIC -> DemoBasicPie(style = chartStyle)
                        ChartType.PIE_DOUGHNUT -> DemoDoughnut(style = chartStyle)
                        ChartType.PIE_ROSE -> DemoNightingaleRose(style = chartStyle)
                        ChartType.PIE_ROSE_BASIC -> DemoBasicNightingaleRose(style = chartStyle)
                        ChartType.PIE_BASIC_ACCESS -> DemoBasicAccessPie(style = chartStyle)
                        ChartType.PIE_ROUNDED_DOUGHNUT -> DemoRoundedDoughnut(style = chartStyle)
                        ChartType.PIE_DOUGHNUT_BASIC -> DemoDoughnutBasic(style = chartStyle)
                        ChartType.PIE_HALF_DOUGHNUT -> DemoHalfDoughnut(style = chartStyle)
                        ChartType.PIE_PAD_ANGLE -> DemoPiePadAngle(style = chartStyle)
                        ChartType.RADAR -> DemoRadar(style = chartStyle)
                        ChartType.KLINE -> DemoKLine(style = chartStyle)
                        ChartType.SCATTER -> DemoScatter(style = chartStyle)
                        ChartType.GAUGE -> DemoGauge(style = chartStyle)
                        ChartType.GAUGE_GRADE -> DemoGradeGauge(style = chartStyle)
                        ChartType.GAUGE_TEMPERATURE -> DemoTemperatureGauge(style = chartStyle)
                        ChartType.CALENDAR_BASIC -> DemoSimpleCalendar(style = chartStyle)
                        ChartType.CALENDAR_VERTICAL -> DemoVerticalCalendar(style = chartStyle)
                        ChartType.CALENDAR_LUNAR -> DemoLunarCalendar(style = chartStyle)
                        ChartType.BAR3D_PUNCH_CARD -> DemoBar3DPunchCard(style = chartStyle)
                        ChartType.FUNNEL -> DemoFunnel(style = chartStyle)
                        ChartType.BOXPLOT -> DemoBoxplot(style = chartStyle)
                        ChartType.MIXED_LINE_BAR -> DemoMixedChart(style = chartStyle)
                        ChartType.LINE_MULTIPLE_X_AXES -> DemoMultipleXAxes(style = chartStyle)
                        ChartType.LINE_STEP -> DemoStepLine(style = chartStyle)
                        ChartType.LINE_RACE -> DemoLineRace(style = chartStyle)
                        ChartType.LINE_FUNCTION_PLOT -> DemoFunctionPlot(style = chartStyle)
                        ChartType.LINE_BUMP -> DemoBumpChart(style = chartStyle)
                        ChartType.LINE_MARK_AREA -> DemoMarkArea(style = chartStyle)
                        
                        ChartType.BAR_NEGATIVE_VALUE -> DemoBarNegativeValue(style = chartStyle)
                        ChartType.BAR_WATERFALL -> DemoBarWaterfall(style = chartStyle)
                        ChartType.BAR_GRADIENT_ZOOM -> DemoBarGradientZoom(style = chartStyle)
                        ChartType.POLAR_BAR_RADIAL -> DemoPolarRadialBar(style = chartStyle)
                        ChartType.POLAR_BAR_TANGENTIAL -> DemoPolarTangentialBar(style = chartStyle)
                        ChartType.DOC_SCREENSHOTS -> DocScreenshotsScreen(style = chartStyle)
                    }
                }
            }
        }
    }
}

/**
 * 高颜值主目录列表
 */
@Composable
fun HomeListScreen(
    isDark: Boolean,
    state: androidx.compose.foundation.lazy.grid.LazyGridState,
    onItemClick: (ChartType) -> Unit
) {
    val items = ChartType.values().toList()
    // 按分类对 Demo 归档
    val grouped = remember(items) { items.groupBy { it.category } }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = state,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        grouped.forEach { (category, list) ->
            // 渲染分类标题栏
            item(span = { GridItemSpan(2) }, key = "category_$category") {
                Text(
                    text = category,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color(0xFF8C9BB4) else Color(0xFF556677),
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp, start = 4.dp)
                )
            }

            // 渲染每一个图表入口卡片
            items(list, key = { "chart_${it.name}" }) { chart ->
                ChartCard(
                    chart = chart,
                    isDark = isDark,
                    onClick = { onItemClick(chart) }
                )
            }
        }
        item(span = { GridItemSpan(2) }, key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ChartCard(
    chart: ChartType,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val cardBg = if (isDark) Color(0xFF1E1E20) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val subTextColor = if (isDark) Color(0xFF999999) else Color(0xFF666666)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 指示标小圆圈
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(chart.indicatorColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = chart.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = chart.subtext,
                fontSize = 11.sp,
                color = subTextColor,
                maxLines = 2,
                lineHeight = 15.sp,
                modifier = Modifier.height(30.dp)
            )
        }
    }
}