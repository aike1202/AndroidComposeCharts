# AndroidComposeCharts

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/Compose-BOM_2024.04-purple.svg)](https://developer.android.com/jetpack/compose)

**AndroidComposeCharts** (Also searchable via: **Android Compose Chart** / **Jetpack Compose Chart**) 是一个利用 Kotlin + Jetpack Compose Canvas 纯原生开发的高性能、高颜值、深具声明式交互质感的 Android 纯 Compose 图表库。

项目通过对物理手势的精细化冲突分发和纯数学矩阵变换直绘，确保库的编译后 APK 体积增量极小，并在移动端提供 100% 顺畅的流畅动画与卓越的交互体验。



---

## 🎨 全量真实效果展示 (43 种图表实例)

以下所有截图均为在物理测试机上运行 **Showroom App** 并使用设备内置的高性能截图工具自动导出的**真实渲染效果图**（未包含任何 AI 虚构成分或网页版假图）。

### 📈 折线图系列 (Line Charts)

<table>
  <tr>
    <td align="center"><b>温度变化趋势</b><br/><img src="docs_screenshots/line_temp_light.png" width="220" alt="温度变化趋势"/></td>
    <td align="center"><b>流量对比看板 (多系列)</b><br/><img src="docs_screenshots/line_multi_light.png" width="220" alt="流量对比看板"/></td>
    <td align="center"><b>海量点密集折线 (1000+)</b><br/><img src="docs_screenshots/line_big_light.png" width="220" alt="密集折线"/></td>
  </tr>
  <tr>
    <td align="center"><b>折线图堆叠</b><br/><img src="docs_screenshots/line_stacked_light.png" width="220" alt="折线图堆叠"/></td>
    <td align="center"><b>堆叠面积图</b><br/><img src="docs_screenshots/line_stacked_area_light.png" width="220" alt="堆叠面积图"/></td>
    <td align="center"><b>渐变堆叠面积</b><br/><img src="docs_screenshots/line_gradient_stacked_area_light.png" width="220" alt="渐变堆叠面积"/></td>
  </tr>
  <tr>
    <td align="center"><b>置信区间带图 (误差范围)</b><br/><img src="docs_screenshots/line_confidence_band_light.png" width="220" alt="置信区间带图"/></td>
    <td align="center"><b>双 X 轴雨量对比</b><br/><img src="docs_screenshots/line_multiple_x_axes_light.png" width="220" alt="双 X 轴雨量对比"/></td>
    <td align="center"><b>阶梯折线图</b><br/><img src="docs_screenshots/line_step_light.png" width="220" alt="阶梯折线"/></td>
  </tr>
  <tr>
    <td align="center"><b>动态排序赛跑 (Race)</b><br/><img src="docs_screenshots/line_race_light.png" width="220" alt="动态排序赛跑"/></td>
    <td align="center"><b>数学函数绘图 (数值双轴)</b><br/><img src="docs_screenshots/line_function_plot_light.png" width="220" alt="数学函数绘图"/></td>
    <td align="center"><b>排名消长凹凸图 (Bump)</b><br/><img src="docs_screenshots/line_bump_light.png" width="220" alt="凹凸图"/></td>
  </tr>
  <tr>
    <td align="center"><b>警戒分带与高峰标注</b><br/><img src="docs_screenshots/line_mark_area_light.png" width="220" alt="警戒分带"/></td>
    <td colspan="2"></td>
  </tr>
</table>

### 📊 柱状图与极坐标系 (Bar & Polar Charts)

<table>
  <tr>
    <td align="center"><b>双柱并列对比</b><br/><img src="docs_screenshots/bar_compare_light.png" width="220" alt="双柱并列对比"/></td>
    <td align="center"><b>正负双向堆叠</b><br/><img src="docs_screenshots/bar_stack_light.png" width="220" alt="正负双向堆叠"/></td>
    <td align="center"><b>空气质量排名 (水平条形)</b><br/><img src="docs_screenshots/bar_horizontal_light.png" width="220" alt="水平条形"/></td>
  </tr>
  <tr>
    <td align="center"><b>交错正负轴条形图</b><br/><img src="docs_screenshots/bar_negative_value_light.png" width="220" alt="交错正负"/></td>
    <td align="center"><b>开支瀑布图</b><br/><img src="docs_screenshots/bar_waterfall_light.png" width="220" alt="瀑布图"/></td>
    <td align="center"><b>发光渐变与点击聚焦</b><br/><img src="docs_screenshots/bar_gradient_zoom_light.png" width="220" alt="点击聚焦"/></td>
  </tr>
  <tr>
    <td align="center"><b>极坐标径向扇形图</b><br/><img src="docs_screenshots/polar_bar_radial_light.png" width="220" alt="极坐标径向"/></td>
    <td align="center"><b>极坐标切向圆环图</b><br/><img src="docs_screenshots/polar_bar_tangential_light.png" width="220" alt="极坐标切向"/></td>
    <td></td>
  </tr>
</table>

### 🍩 饼图系列 (Pie & Doughnut Charts)

<table>
  <tr>
    <td align="center"><b>全球人口占比 (经典饼图)</b><br/><img src="docs_screenshots/pie_basic_light.png" width="220" alt="经典饼图"/></td>
    <td align="center"><b>搜索引擎份额 (空心环图)</b><br/><img src="docs_screenshots/pie_doughnut_light.png" width="220" alt="空心环图"/></td>
    <td align="center"><b>双玫瑰对比 (南丁格尔)</b><br/><img src="docs_screenshots/pie_rose_light.png" width="220" alt="南丁格尔玫瑰"/></td>
  </tr>
  <tr>
    <td align="center"><b>基础南丁格尔玫瑰图</b><br/><img src="docs_screenshots/pie_rose_basic_light.png" width="220" alt="基础南丁格尔"/></td>
    <td align="center"><b>站点访问来源 (引线排版)</b><br/><img src="docs_screenshots/pie_basic_access_light.png" width="220" alt="站点访问来源"/></td>
    <td align="center"><b>圆角环形图 (贝塞尔平滑)</b><br/><img src="docs_screenshots/pie_rounded_doughnut_light.png" width="220" alt="圆角环形"/></td>
  </tr>
  <tr>
    <td align="center"><b>经典分类环形图</b><br/><img src="docs_screenshots/pie_doughnut_basic_light.png" width="220" alt="环形图"/></td>
    <td align="center"><b>半环形占比图 (180度)</b><br/><img src="docs_screenshots/pie_half_doughnut_light.png" width="220" alt="半环形"/></td>
    <td align="center"><b>饼图扇区间隙 (padAngle)</b><br/><img src="docs_screenshots/pie_pad_angle_light.png" width="220" alt="扇区间隙"/></td>
  </tr>
</table>

### 💎 高级特异图表 (Advanced & Specialized Charts)

<table>
  <tr>
    <td align="center"><b>3D 柱状打卡图 (透视投影)</b><br/><img src="docs_screenshots/bar3d_punch_card_light.png" width="220" alt="3D打卡"/></td>
    <td align="center"><b>战力对比雷达</b><br/><img src="docs_screenshots/radar_light.png" width="220" alt="雷达图"/></td>
    <td align="center"><b>个股行情日K (双轴联动)</b><br/><img src="docs_screenshots/kline_light.png" width="220" alt="K线"/></td>
  </tr>
  <tr>
    <td align="center"><b>告警监控散点 (GDP气泡)</b><br/><img src="docs_screenshots/scatter_light.png" width="220" alt="散点图"/></td>
    <td align="center"><b>车速安全仪表 (经典指针)</b><br/><img src="docs_screenshots/gauge_light.png" width="220" alt="仪表盘"/></td>
    <td align="center"><b>等级分类仪表 (小三角指针)</b><br/><img src="docs_screenshots/gauge_grade_light.png" width="220" alt="等级仪表"/></td>
  </tr>
  <tr>
    <td align="center"><b>气温测量仪表 (极简进度弧)</b><br/><img src="docs_screenshots/gauge_temperature_light.png" width="220" alt="气温仪表"/></td>
    <td align="center"><b>基础日历热力图 (提交墙)</b><br/><img src="docs_screenshots/calendar_basic_light.png" width="220" alt="日历热力"/></td>
    <td align="center"><b>纵向打卡日历图 (考勤打卡)</b><br/><img src="docs_screenshots/calendar_vertical_light.png" width="220" alt="纵向日历"/></td>
  </tr>
  <tr>
    <td align="center"><b>农历自定义日历 (徽标嵌入)</b><br/><img src="docs_screenshots/calendar_lunar_light.png" width="220" alt="农历日历"/></td>
    <td align="center"><b>流失转化漏斗</b><br/><img src="docs_screenshots/funnel_light.png" width="220" alt="漏斗"/></td>
    <td align="center"><b>工艺缺陷箱形 (Boxplot)</b><br/><img src="docs_screenshots/boxplot_light.png" width="220" alt="箱线图"/></td>
  </tr>
  <tr>
    <td align="center"><b>雨量蒸发混合 (双Y轴对齐)</b><br/><img src="docs_screenshots/mixed_line_bar_light.png" width="220" alt="混合图"/></td>
    <td colspan="2"></td>
  </tr>
</table>

---

## 🛠 快速上手

这里提供了最常用的四大基础图表在 Jetpack Compose 中的接入代码与渲染预览。

> 💡 **想要获取所有 13 大类图表（如雷达图、日历热力图、K线图、极坐标图等）更详尽的挂载示例与高级参数微调选项？**
>
> 请阅读：👉 **[AndroidComposeCharts 图表全量使用手册 (GUIDE.md)](GUIDE.md)**

### 1. 折线图 (LineChart)
```kotlin
val lineChartData = LineChartData(
    xLabels = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日"),
    series = listOf(
        LineSeries(
            name = "邮件营销",
            points = listOf(
                LinePoint(0f, 120f), LinePoint(1f, 132f), LinePoint(2f, 101f),
                LinePoint(3f, 134f), LinePoint(4f, 90f), LinePoint(5f, 230f), LinePoint(6f, 210f)
            ),
            color = Color(0xFF5470C6),
            isSmooth = true,
            drawArea = true,
            areaBrush = Brush.verticalGradient(
                colors = listOf(Color(0xFF5470C6).copy(alpha = 0.4f), Color.Transparent)
            )
        ),
        LineSeries(
            name = "联盟广告",
            points = listOf(
                LinePoint(0f, 220f), LinePoint(1f, 182f), LinePoint(2f, 191f),
                LinePoint(3f, 234f), LinePoint(4f, 290f), LinePoint(5f, 330f), LinePoint(6f, 310f)
            ),
            color = Color(0xFF91CC75),
            isSmooth = true
        )
    )
)
LineChart(data = lineChartData, modifier = Modifier.fillMaxSize())
```
<img src="docs_screenshots/doc_line_light.png" width="360" alt="折线图预览" />

### 2. 柱状图 (BarChart)
```kotlin
val barChartData = BarChartData(
    xLabels = listOf("一月", "二月", "三月", "四月", "五月", "六月"),
    series = listOf(
        BarSeries(
            name = "蒸发量",
            values = listOf(
                BarValue(2.0f), BarValue(4.9f), BarValue(7.0f),
                BarValue(23.2f), BarValue(25.6f), BarValue(76.7f)
            ),
            color = Color(0xFF5470C6),
            cornerRadius = CornerRadius(12f, 12f),
            barWidthRatio = 0.5f
        ),
        BarSeries(
            name = "降水量",
            values = listOf(
                BarValue(2.6f), BarValue(5.9f), BarValue(9.0f),
                BarValue(26.4f), BarValue(28.7f), BarValue(70.7f)
            ),
            color = Color(0xFF91CC75),
            cornerRadius = CornerRadius(12f, 12f),
            barWidthRatio = 0.5f
        )
    )
)
BarChart(data = barChartData, modifier = Modifier.fillMaxSize())
```
<img src="docs_screenshots/doc_bar_light.png" width="360" alt="柱状图预览" />

### 3. 饼图 / 环形图 (PieChart)
```kotlin
val pieChartData = PieChartData(
    slices = listOf(
        PieSlice("搜索引擎", 1048f, Color(0xFF5470C6)),
        PieSlice("直接输入", 735f, Color(0xFF91CC75)),
        PieSlice("友情链接", 580f, Color(0xFFFAC858)),
        PieSlice("邮件营销", 484f, Color(0xFFEE6666))
    )
)
val customStyle = style.copy(
    pieOptions = style.pieOptions.copy(
        innerRadiusRatio = 0.6f,
        padAngle = 3f,
        cornerRadius = 8.dp
    )
)
PieChart(data = pieChartData, style = customStyle, modifier = Modifier.fillMaxSize())
```
<img src="docs_screenshots/doc_pie_light.png" width="360" alt="饼图预览" />

### 4. 3D 柱状打卡图 (Bar3DChart)
```kotlin
val bar3DChartData = Bar3DChartData(
    xAxisLabels = listOf("12a", "1a", "2a", "3a", "4a", "5a", "6a"),
    yAxisLabels = listOf("周六", "周日"),
    points = listOf(
        Bar3DPoint(xIndex = 0, yIndex = 0, zValue = 5f),
        Bar3DPoint(xIndex = 2, yIndex = 0, zValue = 12f),
        Bar3DPoint(xIndex = 4, yIndex = 1, zValue = 8f),
        Bar3DPoint(xIndex = 6, yIndex = 1, zValue = 15f)
    )
)
val options = Bar3DOptions(
    initialYaw = -45f,
    initialPitch = 30f,
    initialZoom = 1.0f,
    barWidthRatio = 0.5f,
    visualMapColors = listOf(Color(0xFF73C0DE), Color(0xFF3BA272), Color(0xFFFAC858), Color(0xFFEE6666))
)
Bar3DChart(data = bar3DChartData, options = options, modifier = Modifier.fillMaxSize())
```
<img src="docs_screenshots/doc_bar3d_light.png" width="360" alt="3D柱状打卡图预览" />

---


## 📐 核心设计架构原则

1. **数学映射与渲染严格分离**：
   使用逻辑坐标系转换和度量业务数据，在 `DrawScope` 范围内将虚拟比例转换为物理像素。所有渲染器（Renderer）均不直接持有物理手势状态，而是由上一层 Composable 组合函数通过对平移与缩放的统一物理映射来反馈重绘。
2. **状态提升与多图联动 (State Hoisting)**：
   图表的状态（如当前的滚动视口偏移、缩放比例、3D 空间角度等）被完全向外提升并挂载。因此，您可以将同一个 State 状态绑定到多个不同的图表，甚至不同类型的图表上（例如 K线图 与 成交量柱状图 联动滚动），实现优雅的多图同频缩放和联动交互。

3. **物理手势精细隔离**：
   在同一个 Compose 容器上，完美融合了单指拖拽、双指捏合缩放（Pinch-to-zoom）以及单指轻触命中探测。我们设计了极其细腻的冲突分发和消隐过滤算法，并针对多重手势的起止点坐标进行惯性阻尼匹配，确保在 3D 旋转及各种平移滑动交互下依然能稳定保持在 60FPS+。

4. **零第三方 3D 库依赖**：
   3D 柱状图通过自研纯数学三维坐标投影算法，由 Kotlin 在纯 2D Canvas 直接绘制投影后的各个面。极大地保证了渲染性能，且使得最终编译出的库的 APK 体积增量小于 **5KB**。

---

## 📄 许可证

本项目基于 [Apache License 2.0](LICENSE) 许可开源。
