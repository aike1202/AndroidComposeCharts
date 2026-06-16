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
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.border
import androidx.compose.material3.Slider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.mutableFloatStateOf
import io.github.composechart.charts.pie.*
import io.github.composechart.core.style.PieLabelAlign
import io.github.composechart.core.style.PieOptions

enum class ScreenshotTask {
    NONE,
    SHOWCASE_LIGHT,
    SHOWCASE_DARK,
    DOCS_LIGHT,
    DOCS_DARK
}

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
                        onThemeChange = { isDarkTheme = it },
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
    onThemeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    val homeGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    val backgroundColor = if (isDark) Color(0xFF131314) else Color(0xFFF8F9FA)

    // 自动截图控制状态
    var currentTask by remember { mutableStateOf(ScreenshotTask.NONE) }
    var taskIndex by remember { mutableStateOf(0) }
    var runSingleTaskOnly by remember { mutableStateOf(false) }
    var showScreenshotPanel by remember { mutableStateOf(false) }

    val isAutoRunning = currentTask != ScreenshotTask.NONE
    val view = androidx.compose.ui.platform.LocalView.current
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val detailController = remember { ScreenshotController() }
    val showControlPanel = !isAutoRunning

    // 系统返回键拦截
    if (currentScreen is Screen.Detail) {
        BackHandler {
            if (isAutoRunning) {
                currentTask = ScreenshotTask.NONE
                taskIndex = 0
            }
            currentScreen = Screen.Home
        }
    }

    // 自动轮播截图驱动器
    androidx.compose.runtime.LaunchedEffect(currentTask, taskIndex, isDark) {
        if (currentTask == ScreenshotTask.NONE) return@LaunchedEffect
        
        val showcaseList = ChartType.values().filter { it != ChartType.DOC_SCREENSHOTS }
        val isTaskDark = (currentTask == ScreenshotTask.SHOWCASE_DARK || currentTask == ScreenshotTask.DOCS_DARK)
        
        // 保证主题状态与当前任务阶段绝对对齐
        if (isTaskDark != isDark) {
            onThemeChange(isTaskDark)
            kotlinx.coroutines.delay(1000) // 延迟等待系统重新渲染
            return@LaunchedEffect
        }
        
        when (currentTask) {
            ScreenshotTask.SHOWCASE_LIGHT, ScreenshotTask.SHOWCASE_DARK -> {
                if (taskIndex < showcaseList.size) {
                    val chartType = showcaseList[taskIndex]
                    currentScreen = Screen.Detail(chartType)
                    
                    // 等待 1500ms 让动画加载完毕
                    kotlinx.coroutines.delay(1500)
                    
                    val themeSuffix = if (isDark) "_dark" else "_light"
                    val fileName = "${chartType.name.lowercase()}$themeSuffix"
                    
                    // 触发截图（用 Picture 绘图录制，不需要等待存盘完成回调）
                    detailController.capture(fileName)
                    
                    // 额外延迟 500ms 给 IO 线程缓冲，然后继续
                    kotlinx.coroutines.delay(500)
                    taskIndex++
                } else {
                    // 本阶段大厅截图已完成
                    if (runSingleTaskOnly) {
                        currentTask = ScreenshotTask.NONE
                        taskIndex = 0
                        currentScreen = Screen.Home
                        android.widget.Toast.makeText(context, "🎉 大厅图表截图自动生成完成！", android.widget.Toast.LENGTH_LONG).show()
                    } else {
                        if (currentTask == ScreenshotTask.SHOWCASE_LIGHT) {
                            currentTask = ScreenshotTask.DOCS_LIGHT
                            taskIndex = 0
                        } else {
                            currentTask = ScreenshotTask.NONE
                            taskIndex = 0
                        }
                    }
                }
            }
            ScreenshotTask.DOCS_LIGHT, ScreenshotTask.DOCS_DARK -> {
                // 切换到文档截图页面，该页面内有独立的 LaunchedEffect 会执行这 13 个图表的自动截图
                currentScreen = Screen.Detail(ChartType.DOC_SCREENSHOTS)
            }
            else -> {}
        }
    }

    CompositionLocalProvider(LocalShowControlPanel provides showControlPanel) {
        Box(modifier = Modifier.fillMaxSize()) {
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
                        currentTask = ScreenshotTask.NONE
                        taskIndex = 0
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
                val btnText = if (isAutoRunning) {
                    "停止中"
                } else {
                    "截图中心"
                }

                Button(
                    onClick = {
                        if (isAutoRunning) {
                            currentTask = ScreenshotTask.NONE
                            taskIndex = 0
                        } else {
                            showScreenshotPanel = true
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
                        detailController.capture(fileName)
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
                onClick = { onThemeChange(!isDark) },
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
                val s = currentScreen as Screen.Detail
                val themeSuffix = if (isDark) "_dark" else "_light"
                val fileName = "${s.chartType.name.lowercase()}$themeSuffix"

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundColor)
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null,
                            onClick = {} // 拦截点击
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    ChartScreenshotContainer(
                        fileName = fileName,
                        controller = detailController,
                        modifier = Modifier
                            .run {
                                if (isAutoRunning) {
                                    this.size(width = 360.dp, height = 360.dp)
                                } else {
                                    this.fillMaxSize()
                                }
                            }
                    ) {
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
                        ChartType.DOC_SCREENSHOTS -> DocScreenshotsScreen(
                            style = chartStyle,
                            currentTask = currentTask,
                            onDocsFinished = {
                                if (currentTask == ScreenshotTask.DOCS_LIGHT) {
                                    currentTask = ScreenshotTask.NONE
                                    taskIndex = 0
                                    currentScreen = Screen.Home
                                    android.widget.Toast.makeText(context, "🎉 全量 56 张亮色图表截图已全自动生成完成！", android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        )
                    }
                } // 闭合 ChartScreenshotContainer
            }
        }
    }
}

    // 自动截图进度提示条 Overlay
    if (isAutoRunning) {
        val showcaseList = ChartType.values().filter { it != ChartType.DOC_SCREENSHOTS }
        val totalCount = if (currentTask == ScreenshotTask.SHOWCASE_LIGHT || currentTask == ScreenshotTask.SHOWCASE_DARK) {
            showcaseList.size
        } else {
            13
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .clickable(enabled = true, onClick = {}), // 拦截全屏点击防止误触
            contentAlignment = Alignment.TopCenter
        ) {
            Card(
                modifier = Modifier
                    .padding(top = 80.dp)
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE6A23C)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📸 正在自动截图中: ${currentTask.name}\n进度: ${taskIndex + 1} / $totalCount",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = {
                            currentTask = ScreenshotTask.NONE
                            taskIndex = 0
                            currentScreen = Screen.Home
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEE6666)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("停止", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }
    }

    // 截图中心底部面板 Overlay
    if (showScreenshotPanel && currentScreen is Screen.Home) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { showScreenshotPanel = false },
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable(enabled = false, onClick = {}),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF1E1E20) else Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 40.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isDark) Color.DarkGray else Color.LightGray)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "📸 自动截图控制中心",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "生成的截图以 360dp*360dp 纯净画幅保存至手机相册 Pictures/ComposeChart/",
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFF999999) else Color(0xFF666666),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                showScreenshotPanel = false
                                runSingleTaskOnly = true
                                taskIndex = 0
                                currentTask = ScreenshotTask.SHOWCASE_LIGHT
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFAC858)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("仅生成大厅亮色截图 (43张)", fontSize = 12.sp, color = Color.Black)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Button(
                        onClick = {
                            showScreenshotPanel = false
                            runSingleTaskOnly = false
                            taskIndex = 0
                            currentTask = ScreenshotTask.SHOWCASE_LIGHT
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF67C23A)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("一键全自动生成 56 张亮色截图", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "温馨提示：生成中请保持屏幕常亮。\n生成后，在电脑端执行 pull_doc_images.ps1 拉取到工程目录。",
                        fontSize = 11.sp,
                        color = Color(0xFFE6A23C),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
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
        columns = GridCells.Fixed(1), // 手机端测试，一列只展示一个
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

@Composable
fun PieTestScreen(
    onExitTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pieChartData = remember {
        PieChartData(
            slices = listOf(
                PieSlice("肉", 46.2f, Color(0xFFEE6666)),
                PieSlice("学生奶", 14.9f, Color(0xFF5470C6)),
                PieSlice("水果", 22.7f, Color(0xFFFC8452)),
                PieSlice("蔬菜", 12.1f, Color(0xFF9A60B4)),
                PieSlice("米、面、油", 2.5f, Color(0xFF3BA272)),
                PieSlice("蛋类", 1.0f, Color(0xFFFAC858)),
                PieSlice("调味品", 0.5f, Color(0xFF73C0DE)),
                PieSlice("馕", 0.1f, Color(0xFF91CC75)),
                PieSlice("糕点类", 0.01f, Color(0xFFEE6666))
            )
        )
    }

    var labelAlign by remember { mutableStateOf(PieLabelAlign.LeftRight) }
    var outerRadiusRatio by remember { mutableFloatStateOf(0.9f) }
    var innerRadiusRatio by remember { mutableFloatStateOf(0.6f) }
    var containerWidthRatio by remember { mutableFloatStateOf(0.7f) }
    var showLabel by remember { mutableStateOf(true) }
    var minShowLabelPercent by remember { mutableFloatStateOf(0f) } // 默认 0f，即不对过小标签隐去，以便测试 minAngle
    var minAngle by remember { mutableFloatStateOf(0f) } // 默认 0度，还原真实的比例

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. 顶部标题与退出测试按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "饼图 4:3 比例自适应测试",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = onExitTest,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("返回主演示", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. 核心 4:3 浅白色 1dp 边框容器及饼图渲染
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(containerWidthRatio)
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1B1B1D))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)) // 1dp 浅白色边框
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                val testStyle = ChartStyle.Dark.copy(
                    backgroundColor = Color.Transparent,
                    pieOptions = ChartStyle.Dark.pieOptions.copy(
                        innerRadiusRatio = innerRadiusRatio,
                        outerRadiusRatio = outerRadiusRatio,
                        showLabel = showLabel,
                        labelAlign = labelAlign,
                        padAngle = 2f,
                        cornerRadius = 4.dp,
                        minShowLabelPercent = minShowLabelPercent,
                        minAngle = minAngle
                    )
                )

                PieChart(
                    data = pieChartData,
                    style = testStyle,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. 底部调试控制面板
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // 对齐模式 RadioButtons
                Text("标签排布模式 (labelAlign):", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = labelAlign == PieLabelAlign.None,
                            onClick = { labelAlign = PieLabelAlign.None }
                        )
                        Text("None (放射)", color = Color.LightGray, fontSize = 11.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = labelAlign == PieLabelAlign.LeftRight,
                            onClick = { labelAlign = PieLabelAlign.LeftRight }
                        )
                        Text("LeftRight (左右)", color = Color.LightGray, fontSize = 11.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = labelAlign == PieLabelAlign.TopBottom,
                            onClick = { labelAlign = PieLabelAlign.TopBottom }
                        )
                        Text("TopBottom (上下)", color = Color.LightGray, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 滑块与开关控制
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("显示外部标签:", color = Color.White, fontSize = 12.sp)
                    Checkbox(
                        checked = showLabel,
                        onCheckedChange = { showLabel = it }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 最小扫掠角 Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("最小角度大小 (minAngle):", color = Color.White, fontSize = 12.sp)
                        Text(String.format("%.1f°", minAngle), color = Color(0xFF3B82F6), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = minAngle,
                        onValueChange = { minAngle = it },
                        valueRange = 0f..30.0f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 动态过滤微小占比标签的 Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("微小标签过滤阈值 (minShowLabelPercent):", color = Color.White, fontSize = 12.sp)
                        Text(String.format("%.2f%%", minShowLabelPercent), color = Color(0xFF3B82F6), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = minShowLabelPercent,
                        onValueChange = { minShowLabelPercent = it },
                        valueRange = 0f..5.0f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 动态拉缩容器宽度的 Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("测试容器宽度比例 (maxWidthRatio):", color = Color.White, fontSize = 12.sp)
                        Text(String.format("%.2f", containerWidthRatio), color = Color(0xFF3B82F6), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = containerWidthRatio,
                        onValueChange = { containerWidthRatio = it },
                        valueRange = 0.35f..1.0f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("外半径比例 (outerRadiusRatio):", color = Color.White, fontSize = 12.sp)
                        Text(String.format("%.2f", outerRadiusRatio), color = Color(0xFF3B82F6), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = outerRadiusRatio,
                        onValueChange = { outerRadiusRatio = it },
                        valueRange = 0.4f..1.0f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("内半径比例 (innerRadiusRatio):", color = Color.White, fontSize = 12.sp)
                        Text(String.format("%.2f", innerRadiusRatio), color = Color(0xFF3B82F6), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = innerRadiusRatio,
                        onValueChange = { innerRadiusRatio = it },
                        valueRange = 0.0f..0.8f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}