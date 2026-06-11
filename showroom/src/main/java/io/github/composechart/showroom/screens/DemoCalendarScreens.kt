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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.composechart.charts.calendar.CalendarChart
import io.github.composechart.charts.calendar.CalendarChartData
import io.github.composechart.charts.calendar.CalendarDayData
import io.github.composechart.charts.calendar.CalendarOptions
import io.github.composechart.charts.calendar.CalendarOrientation
import io.github.composechart.core.style.ChartStyle
import io.github.composechart.core.style.TitleOptions
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Demo 15A: 基础日历热力图 (Simple Calendar Heatmap)
 * 横向 GitHub 贡献提交墙风格
 */
@Composable
fun DemoSimpleCalendar(style: ChartStyle) {
    var intensity by remember { mutableStateOf(1f) }
    var calendarData by remember { mutableStateOf(generateMockCalendarData(2017, intensity)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
        ) {
            CalendarChart(
                data = calendarData,
                style = style.copy(
                    titleOptions = TitleOptions(
                        text = "GitHub 贡献提交热力墙",
                        subtext = "2017 年每日提交频次热度分布，基于 GitHub 经典绿度渐变色带映射",
                        textStyle = style.titleOptions.textStyle.copy(fontSize = 15.sp)
                    )
                ),
                options = CalendarOptions(
                    orientation = CalendarOrientation.Horizontal,
                    firstDayOfWeek = 0, // 周日开始
                    visualMapColors = listOf(
                        if (style.backgroundColor == Color.White) Color(0xFFEBEDF0) else Color(0xFF21262D), // 基底背景格
                        Color(0xFF9BE9A8), // 浅绿
                        Color(0xFF40C463), // 中浅绿
                        Color(0xFF30A14E), // 中绿
                        Color(0xFF216E39)  // 深绿
                    ),
                    cellSize = 15.dp,
                    cellGap = 2.dp,
                    cellCornerRadius = 2.5.dp
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
                    intensity = (intensity - 0.2f).coerceAtLeast(0.2f)
                    calendarData = generateMockCalendarData(2017, intensity)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30A14E)),
                modifier = Modifier.weight(1f)
            ) {
                Text("降低热度")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = {
                    calendarData = generateMockCalendarData(2017, intensity)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFAC858)),
                modifier = Modifier.weight(1.2f)
            ) {
                Text("随机刷新")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = {
                    intensity = (intensity + 0.2f).coerceAtMost(3.0f)
                    calendarData = generateMockCalendarData(2017, intensity)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEE6666)),
                modifier = Modifier.weight(1f)
            ) {
                Text("提高热度")
            }
        }

        ControlPanel {
            ControlSlider(
                label = "数据放大倍率",
                value = intensity,
                valueRange = 0.2f..3.0f,
                onValueChange = {
                    intensity = it
                    calendarData = generateMockCalendarData(2017, intensity)
                },
                valueFormatter = { String.format("%.1f 倍", it) }
            )
        }
    }
}

/**
 * Demo 15B: 纵向日历热力图 (Vertical Calendar Heatmap)
 * 学习/考勤打卡风格，红黄渐变
 */
@Composable
fun DemoVerticalCalendar(style: ChartStyle) {
    var calendarData by remember { mutableStateOf(generateMockCalendarData(2017, 1.0f)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(680.dp) // 纵向包含53行，需要更高的容器高度
                .padding(bottom = 16.dp)
        ) {
            CalendarChart(
                data = calendarData,
                style = style.copy(
                    titleOptions = TitleOptions(
                        text = "个人年度考勤/学习打卡图",
                        subtext = "2017 纵向日历分布，基于红黄橙三段渐变热力对照",
                        textStyle = style.titleOptions.textStyle.copy(fontSize = 15.sp)
                    )
                ),
                options = CalendarOptions(
                    orientation = CalendarOrientation.Vertical,
                    firstDayOfWeek = 1, // 周一开始
                    visualMapColors = listOf(
                        if (style.backgroundColor == Color.White) Color(0xFFFEF2F2) else Color(0xFF27272A),
                        Color(0xFFFCA5A5), // 浅红
                        Color(0xFFF59E0B), // 橙黄
                        Color(0xFFEF4444), // 红色
                        Color(0xFFB91C1C)  // 深红
                    ),
                    cellSize = 16.dp,
                    cellGap = 3.dp,
                    cellCornerRadius = 3.dp
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
                    calendarData = generateMockCalendarData(2017, 1.0f)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("重新随机生成打卡状态")
            }
        }
    }
}

/**
 * Demo 15C: 农历/徽标自定义日历图 (Custom & Lunar Calendar)
 * 单元格内绘制自定义文字标签 (春节/中秋等) 并携带自定义图标符号
 */
@Composable
fun DemoLunarCalendar(style: ChartStyle) {
    var showSymbols by remember { mutableStateOf(true) }
    var calendarData by remember(showSymbols) {
        mutableStateOf(generateLunarCalendarData(2017, showSymbols))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
        ) {
            CalendarChart(
                data = calendarData,
                style = style.copy(
                    titleOptions = TitleOptions(
                        text = "农历节日及自定义徽标日历",
                        subtext = "在特定网格单元格中内置文字标签（除夕、春节等）或符号徽标（♥、☀）",
                        textStyle = style.titleOptions.textStyle.copy(fontSize = 15.sp)
                    )
                ),
                options = CalendarOptions(
                    orientation = CalendarOrientation.Horizontal,
                    firstDayOfWeek = 0, // 周日开始
                    visualMapColors = listOf(
                        if (style.backgroundColor == Color.White) Color(0xFFF8FAFC) else Color(0xFF1E293B),
                        Color(0xFFBFDBFE), // 节日基色浅蓝
                        Color(0xFF60A5FA), // 浅蓝
                        Color(0xFF3B82F6)  // 蓝色
                    ),
                    cellSize = 15.dp,
                    cellGap = 2.dp,
                    cellCornerRadius = 3.dp
                ),
                modifier = Modifier.fillMaxSize()
            )
        }

        ControlPanel {
            ControlSwitch(
                label = "在特定日期显示符号标记 (♥、☀、★)",
                checked = showSymbols,
                onCheckedChange = { showSymbols = it }
            )
        }
    }
}

/**
 * 模拟日历数据生成器
 */
private fun generateMockCalendarData(year: Int, multiplier: Float): CalendarChartData {
    val days = mutableListOf<CalendarDayData>()
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val cal = Calendar.getInstance()
    cal.set(Calendar.YEAR, year)
    cal.set(Calendar.MONTH, Calendar.JANUARY)
    cal.set(Calendar.DAY_OF_MONTH, 1)
    
    val totalDays = cal.getActualMaximum(Calendar.DAY_OF_YEAR)
    for (i in 1..totalDays) {
        cal.set(Calendar.DAY_OF_YEAR, i)
        val dateStr = sdf.format(cal.time)
        
        // 随机产生热度值，周末较高，工作日间歇出现，代表步数或活跃度
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val baseVal = if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            4000f + Math.random().toFloat() * 6000f
        } else {
            500f + Math.random().toFloat() * 7000f
        }
        val finalVal = (baseVal * multiplier).coerceIn(0f, 15000f)
        
        days.add(CalendarDayData(
            date = dateStr,
            value = finalVal,
            tooltip = "今日指标: ${String.format("%.0f", finalVal)} 步"
        ))
    }
    return CalendarChartData(year, days)
}

/**
 * 农历与节日徽标数据生成器
 */
private fun generateLunarCalendarData(year: Int, includeSymbols: Boolean): CalendarChartData {
    val days = mutableListOf<CalendarDayData>()
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val cal = Calendar.getInstance()
    cal.set(Calendar.YEAR, year)
    cal.set(Calendar.MONTH, Calendar.JANUARY)
    cal.set(Calendar.DAY_OF_MONTH, 1)
    
    // 2017 年几个典型的节日对应公历日期
    val holidays = mapOf(
        "2017-01-27" to "除夕",
        "2017-01-28" to "春节",
        "2017-02-11" to "元宵",
        "2017-05-30" to "端午",
        "2017-10-04" to "中秋",
        "2017-10-01" to "国庆"
    )

    val totalDays = cal.getActualMaximum(Calendar.DAY_OF_YEAR)
    for (i in 1..totalDays) {
        cal.set(Calendar.DAY_OF_YEAR, i)
        val dateStr = sdf.format(cal.time)
        
        val holiday = holidays[dateStr]
        var label: String? = holiday
        var valAmt = 0f
        var tooltips: String? = null

        if (holiday != null) {
            valAmt = 80f // 节日分配高数值以着深色
            tooltips = "节日: $holiday"
        } else if (includeSymbols) {
            // 随机在少量方格里塞入图标字符，作为事件标注
            val rand = Math.random()
            if (rand < 0.02) {
                label = "☀"
                valAmt = 30f
                tooltips = "天气晴朗 ☀️"
            } else if (rand < 0.04) {
                label = "♥"
                valAmt = 50f
                tooltips = "特别纪念日 ❤️"
            } else if (rand < 0.05) {
                label = "★"
                valAmt = 60f
                tooltips = "星级任务完成 ⭐"
            }
        }

        days.add(CalendarDayData(
            date = dateStr,
            value = valAmt,
            label = label,
            tooltip = tooltips ?: "今日无特殊事件"
        ))
    }
    return CalendarChartData(year, days)
}
