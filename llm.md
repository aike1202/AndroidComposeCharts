# AndroidComposeCharts LLM Integration Reference

AndroidComposeCharts is a high-performance, premium-designed chart library written in pure Kotlin and Jetpack Compose Canvas. It operates without any external 2D/3D graphics libraries (like OpenGL) and relies fully on Compose Canvas drawing.

---

## 📦 Dependency Integration

To integrate the library, add this coordinate directly to your Kotlin DSL `build.gradle.kts` dependencies section:

```kotlin
dependencies {
    implementation("com.github.aike1202:charts:1.0.0")
}
```

---

## 🎨 Global Design System: ChartStyle

`ChartStyle` represents the unified theme and grid system for all charts, containing sub-configurations for grid margins, titles, legends, axes, and paletted colors.

### ChartStyle Definition
```kotlin
package io.github.composechart.core.style

data class ChartStyle(
    val backgroundColor: Color = Color.Transparent,
    val titleOptions: TitleOptions = TitleOptions(),
    val legendOptions: LegendOptions = LegendOptions(),
    val gridOptions: GridOptions = GridOptions(),
    val xAxisOptions: AxisOptions = AxisOptions(showGridLines = false), // X-axis default: no vertical gridlines
    val yAxisOptions: AxisOptions = AxisOptions(showGridLines = true),  // Y-axis default: show horizontal gridlines
    val y2AxisOptions: AxisOptions = AxisOptions(show = false),         // Secondary Y-axis (hidden by default)
    val x2AxisOptions: AxisOptions = AxisOptions(show = false),         // Top X-axis (hidden by default)
    val colorPalette: List<Color> = DefaultColors,
    val pieOptions: PieOptions = PieOptions(),
    val radarOptions: RadarOptions = RadarOptions(),
    val polarOptions: PolarOptions = PolarOptions()
) {
    companion object {
        val Light = ChartStyle()
        val Dark = ChartStyle(
            backgroundColor = Color(0xFF1B1B1D),
            titleOptions = TitleOptions(
                textStyle = TextStyle(color = Color(0xFFEEEEEE), fontSize = 15.sp, fontWeight = FontWeight.Bold),
                subtextStyle = TextStyle(color = Color(0xFFB0B0B0), fontSize = 11.sp)
            ),
            legendOptions = LegendOptions(
                textStyle = TextStyle(color = Color(0xFFCCCCCC), fontSize = 11.sp)
            ),
            xAxisOptions = AxisOptions(
                showGridLines = false,
                lineColor = Color(0xFF6E7078),
                tickColor = Color(0xFF6E7078),
                labelTextStyle = TextStyle(color = Color(0xFFB0B3C0), fontSize = 10.sp)
            ),
            yAxisOptions = AxisOptions(
                showGridLines = true,
                lineColor = Color(0xFF6E7078),
                tickColor = Color(0xFF6E7078),
                gridLineColor = Color(0xFF3A3A3D),
                labelTextStyle = TextStyle(color = Color(0xFFB0B3C0), fontSize = 10.sp)
            ),
            colorPalette = DarkColors
        )
        val Default = Light
    }
}
```

### ChartStyle Sub-configuration Blocks

#### 1. TitleOptions (Main & Sub-title Configuration)
```kotlin
data class TitleOptions(
    val show: Boolean = true,
    val text: String = "",
    val subtext: String = "",
    val textStyle: TextStyle = TextStyle(color = Color(0xFF333333), fontSize = 15.sp, fontWeight = FontWeight.Bold),
    val subtextStyle: TextStyle = TextStyle(color = Color(0xFF999999), fontSize = 11.sp),
    val itemGap: Dp = 4.dp, // Vertical gap between title and sub-title
    val padding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    val alignment: Alignment.Horizontal = Alignment.Start
)
```

#### 2. LegendOptions (Legend Filter Controls)
```kotlin
data class LegendOptions(
    val show: Boolean = true,
    val position: LegendPosition = LegendPosition.Top, // Top, Bottom, Left, Right
    val alignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    val itemGap: Dp = 12.dp, // Gap between legend icons
    val itemWidth: Dp = 14.dp,
    val itemHeight: Dp = 10.dp,
    val iconShape: LegendIconShape = LegendIconShape.RoundRect, // RoundRect, Circle, Rect, Line, Diamond
    val textStyle: TextStyle = TextStyle(color = Color(0xFF666666), fontSize = 11.sp),
    val selectMode: LegendSelectMode = LegendSelectMode.Multiple // Multiple, Single, None
)

enum class LegendPosition { Top, Bottom, Left, Right }
enum class LegendIconShape { RoundRect, Circle, Rect, Line, Diamond }
enum class LegendSelectMode { Multiple, Single, None }
```

#### 3. GridOptions (Chart Layout Bounds)
```kotlin
data class GridOptions(
    val left: Dp = 40.dp,   // Margin allocated for Y-axis labels
    val top: Dp = 48.dp,    // Margin allocated for Title/Legend
    val right: Dp = 24.dp,  // Margin allocated for secondary Y-axis
    val bottom: Dp = 40.dp, // Margin allocated for X-axis labels
    val containLabel: Boolean = true // Auto-expand grid size dynamically based on label length
)
```

#### 4. AxisOptions (Detailed Axis Styling)
```kotlin
data class AxisOptions(
    val show: Boolean = true,
    val onZero: Boolean = false, // Whether the axis line locks to the zero point
    val inverse: Boolean = false, // Inverts axis direction (useful for ranking/bump charts)
    val name: String = "", // Axis label name/unit (e.g., "Temp (℃)")
    val nameTextStyle: TextStyle = TextStyle(color = Color(0xFF888888), fontSize = 10.sp),
    val showLine: Boolean = true,
    val lineColor: Color = Color(0xFF757575),
    val lineWidth: Dp = 1.dp,
    val showTicks: Boolean = true,
    val tickColor: Color = Color(0xFF757575),
    val tickLength: Dp = 4.dp,
    val showLabels: Boolean = true,
    val labelTextStyle: TextStyle = TextStyle(color = Color(0xFF757575), fontSize = 10.sp),
    val labelRotate: Float = 0f, // Rotate labels (e.g., -45f)
    val labelOnZero: Boolean = true, // Whether zero labels stay locked to the zero axis line
    val labelFormatter: ((value: Float) -> String)? = null, // Custom text formatter callback
    val showGridLines: Boolean = true,
    val gridLineColor: Color = Color(0xFFE0E0E0),
    val gridLineWidth: Dp = 0.5.dp,
    val gridLineStyle: GridLineStyle = GridLineStyle.Solid, // Solid, Dashed
    val showSplitArea: Boolean = false,
    val splitAreaColors: List<Color> = listOf(Color.Transparent, Color(0xFFF9F9F9).copy(alpha = 0.4f))
)

enum class GridLineStyle { Solid, Dashed }
```

#### 5. TooltipOptions (High-fidelity Popover Settings)
```kotlin
data class TooltipOptions(
    val enabled: Boolean = true,
    val trigger: TriggerType = TriggerType.Axis, // Axis (axis snap) or Item (data point hover)
    val indicatorStyle: IndicatorStyle = IndicatorStyle.Line, // Line (vertical line), Cross, Shadow
    val indicatorColor: Color = Color(0xFF5470C6).copy(alpha = 0.4f),
    val backgroundColor: Color = Color.White.copy(alpha = 0.9f),
    val borderColor: Color = Color(0xFFE0E0E0),
    val borderWidth: Dp = 1.dp,
    val cornerRadius: Dp = 4.dp,
    val textStyle: TextStyle = TextStyle(color = Color(0xFF333333), fontSize = 12.sp),
    val shadowRadius: Dp = 6.dp
)
```

---

## 📈 Viewport & Interaction Controllers

To support zooming, panning, and synchronized tooltips across multiple charts, hoist `ViewportState` and `InteractionState` at the composable level.

```kotlin
package io.github.composechart.core.state
```

### ViewportState (Panning & Zooming)
Use `rememberViewportState` to manage horizontal and vertical viewing limits.

```kotlin
@Composable
fun rememberViewportState(
    initialMinX: Float = 0f,
    initialMaxX: Float = 100f,
    initialMinY: Float = 0f,
    initialMaxY: Float = 100f,
    viewportMode: ViewportMode = ViewportMode.Fit,
    enablePan: Boolean = true,
    enableZoom: Boolean = true
): ViewportState
```

#### ViewportMode Types
- `ViewportMode.Fit`: Standard fit-to-screen mode.
- `ViewportMode.Scrollable(visibleCount: Int)`: Fixed viewing count on screen, enabling side-scrolling for additional points.

### InteractionState (Tooltip Hover Sync)
Allows cross-chart tracking of hovering states and physical tap coordinates.
```kotlin
class InteractionState {
    var isTooltipActive: Boolean
    var tooltipDataX: Float? // Current active X-value in coordinate space
    var tooltipScreenOffset: Offset? // Screen coordinates for rendering popovers
}
```

---

## 📊 Detailed Composable API Reference for 13 Chart Types

---

### 1. LineChart (折线与面积渐变图)

Supports smooth spline curves, vertical gradient areas, custom points, step lines, and visual highlights.

#### Composable Signature
```kotlin
import io.github.composechart.charts.line.*

@Composable
fun LineChart(
    data: LineChartData,
    modifier: Modifier = Modifier,
    style: ChartStyle = ChartStyle.Default,
    viewportState: ViewportState = rememberViewportState(),
    interactionState: InteractionState = rememberInteractionState(),
    tooltipOptions: TooltipOptions = TooltipOptions.Default,
    legendOptions: LegendOptions = LegendOptions.Default,
    dataZoomOptions: DataZoomOptions? = null,
    visualMap: List<VisualMapRange>? = null,
    rangeAreaOptions: RangeAreaOptions? = null,
    rangeAreaOptionsList: List<RangeAreaOptions> = emptyList(),
    x2Labels: List<String>? = null,
    xAxisTicks: List<Float>? = null,
    x2AxisTicks: List<Float>? = null,
    markAreas: List<MarkArea> = emptyList(),
    onValueSelected: ((point: LinePoint, series: LineSeries) -> Unit)? = null
)
```

#### Models & Enums
```kotlin
data class LineChartData(val xLabels: List<String>, val series: List<LineSeries>)

data class LineSeries(
    val name: String,
    val points: List<LinePoint>,
    val color: Color,
    val isSmooth: Boolean = false,
    val lineWidth: Dp = 2.dp,
    val lineStyle: LineStyleType = LineStyleType.Solid,
    val drawArea: Boolean = false,
    val areaBrush: Brush? = null,
    val symbol: SymbolType = SymbolType.Circle,
    val symbolSize: Dp = 4.dp,
    val symbolColor: Color? = null,
    val yAxisIndex: Int = 0, // 0: Left Y-axis, 1: Right Y-axis
    val xAxisIndex: Int = 0, // 0: Bottom X-axis, 1: Top X-axis
    val showEndLabel: Boolean = false,
    val showSymbolLabel: Boolean = false,
    val stack: String? = null, // Stack ID for stacked line charts
    val stepType: StepType = StepType.None,
    val connectNulls: Boolean = false,
    val markPoints: List<MarkPoint> = emptyList(),
    val markLines: List<MarkLine> = emptyList()
)

data class LinePoint(val x: Float, val y: Float?) // y = null indicates missing data points

enum class LineStyleType { Solid, Dashed, Dotted }
enum class SymbolType { Circle, Square, Diamond, None }
enum class StepType { None, Start, Middle, End }

data class VisualMapRange(val min: Float, val max: Float, val color: Color)
data class MarkArea(
    val startX: Float? = null, val endX: Float? = null,
    val startY: Float? = null, val endY: Float? = null,
    val color: Color, val label: String? = null,
    val labelColor: Color = Color(0xFF888888)
)
```

#### Code Example
```kotlin
val data = LineChartData(
    xLabels = listOf("周一", "周二", "周三", "周四"),
    series = listOf(
        LineSeries(
            name = "邮件营销",
            points = listOf(LinePoint(0f, 120f), LinePoint(1f, 132f), LinePoint(2f, 101f), LinePoint(3f, 134f)),
            color = Color(0xFF5470C6),
            isSmooth = true,
            drawArea = true,
            areaBrush = Brush.verticalGradient(listOf(Color(0xFF5470C6).copy(alpha = 0.4f), Color.Transparent))
        )
    )
)
LineChart(data = data, modifier = Modifier.fillMaxSize())
```

---

### 2. BarChart (直角柱状/条形图)

Supports vertical/horizontal orientation, stacked bars, background grid slots, and corner radius styling.

#### Composable Signature
```kotlin
import io.github.composechart.charts.bar.*

@Composable
fun BarChart(
    data: BarChartData,
    modifier: Modifier = Modifier,
    style: ChartStyle = ChartStyle.Default,
    viewportState: ViewportState = rememberViewportState(),
    interactionState: InteractionState = rememberInteractionState(),
    tooltipOptions: TooltipOptions = TooltipOptions.Default,
    legendOptions: LegendOptions = LegendOptions.Default,
    dataZoomOptions: DataZoomOptions? = null,
    horizontal: Boolean = false, // Set to true for horizontal bar/strip charts
    clickToZoom: Boolean = false,
    onBarSelected: ((bar: BarValue, series: BarSeries) -> Unit)? = null
)
```

#### Models & Enums
```kotlin
data class BarChartData(val xLabels: List<String>, val series: List<BarSeries>)

data class BarSeries(
    val name: String,
    val values: List<BarValue>,
    val color: Color,
    val gradientBrush: Brush? = null,
    val cornerRadius: CornerRadius = CornerRadius(4f, 4f),
    val barWidthRatio: Float = 0.6f, // Relative width within category gap (0.0..1.0)
    val barGapRatio: Float = 0.2f,   // Gap between multiple series' adjacent bars
    val showBackground: Boolean = false,
    val backgroundColor: Color = Color.LightGray.copy(alpha = 0.2f),
    val yAxisIndex: Int = 0,
    val stack: String? = null, // Stack ID. Bars with same Stack ID stack vertically
    val shadowColor: Color? = null,
    val shadowBlur: Float = 0f,
    val shadowOffset: Offset = Offset.Zero
)

data class BarValue(val value: Float, val baseValue: Float? = null) // baseValue defines bar bottom offset
```

#### Code Example
```kotlin
val data = BarChartData(
    xLabels = listOf("一月", "二月", "三月"),
    series = listOf(
        BarSeries(
            name = "蒸发量",
            values = listOf(BarValue(2.0f), BarValue(4.9f), BarValue(7.0f)),
            color = Color(0xFF5470C6),
            cornerRadius = CornerRadius(12f, 12f)
        )
    )
)
BarChart(data = data, horizontal = false, modifier = Modifier.fillMaxSize())
```

---

### 3. PieChart (饼图/环形图/南丁格尔玫瑰图)

Supports empty doughnut centers, Nightingale Rose charts, outer guide annotations, and curved ends.

#### Composable Signature
```kotlin
import io.github.composechart.charts.pie.*

@Composable
fun PieChart(
    data: PieChartData,
    modifier: Modifier = Modifier,
    style: ChartStyle = ChartStyle.Default,
    legendOptions: LegendOptions = LegendOptions.Default,
    onSliceSelected: ((slice: PieSlice) -> Unit)? = null,
    centerContent: (@Composable () -> Unit)? = null // Custom composable in the doughnut center
)
```

#### Models & Enums
```kotlin
data class PieChartData(val slices: List<PieSlice>)
data class PieSlice(val name: String, val value: Float, val color: Color)

// Hoisted under ChartStyle.pieOptions
data class PieOptions(
    val roseType: RoseType = RoseType.None,
    val innerRadiusRatio: Float = 0f, // 0f = full Pie, >0f = Doughnut Ring (e.g. 0.6f)
    val padAngle: Float = 0f,         // Angular gap between sectors (in degrees)
    val cornerRadius: Dp = 0.dp,      // Inner/outer corner radius
    val selectedOffset: Dp = 10.dp,   // Pop-out offset when sector is clicked
    val showLabel: Boolean = true,
    val labelLineLength1: Dp = 15.dp,
    val labelLineLength2: Dp = 10.dp,
    val labelLineColor: Color? = null,
    val labelLineWidth: Dp = 1.dp,
    val startAngle: Float = -90f,
    val maxAngleSweep: Float = 360f,
    val roundCap: Boolean = false,    // Ends of doughnut arcs rounded as capsule caps
    val borderWidth: Dp = 0.dp,
    val borderColor: Color? = null
)

enum class RoseType {
    None,   // Standard Pie
    Radius, // Nightingale Rose (even angles, radius maps values)
    Area    // Nightingale Rose (angles map relative value, radius maps values)
}
```

#### Code Example
```kotlin
val data = PieChartData(
    slices = listOf(PieSlice("搜索引擎", 1048f, Color(0xFF5470C6)), PieSlice("直接输入", 735f, Color(0xFF91CC75)))
)
val customStyle = ChartStyle.Default.copy(
    pieOptions = PieOptions(innerRadiusRatio = 0.6f, padAngle = 3f, cornerRadius = 8.dp)
)
PieChart(data = data, style = customStyle, modifier = Modifier.fillMaxSize())
```

---

### 4. Bar3DChart (3D 柱状打卡图)

Employs pure vector rotation (Pitch/Yaw/Zoom) mathematically projected onto Compose 2D Canvas.

#### Composable Signature
```kotlin
import io.github.composechart.charts.bar3d.*

@Composable
fun Bar3DChart(
    data: Bar3DChartData,
    modifier: Modifier = Modifier,
    style: ChartStyle = ChartStyle.Default,
    options: Bar3DOptions = Bar3DOptions()
)
```

#### Models & Enums
```kotlin
data class Bar3DPoint(val xIndex: Int, val yIndex: Int, val zValue: Float, val color: Color? = null)

data class Bar3DChartData(
    val xAxisLabels: List<String>,
    val yAxisLabels: List<String>,
    val points: List<Bar3DPoint>,
    val zMin: Float = 0f,
    val zMax: Float = 12f
)

data class Bar3DOptions(
    val initialYaw: Float = -45f,
    val initialPitch: Float = 30f,
    val initialZoom: Float = 1.0f,
    val barWidthRatio: Float = 0.5f,
    val gridColor: Color = Color.Gray.copy(alpha = 0.25f),
    val visualMapColors: List<Color> = listOf(Color(0xFF73C0DE), Color(0xFFFAC858), Color(0xFFEE6666)),
    val showVisualMap: Boolean = true,
    val labelColor: Color? = null
)
```

#### Code Example
```kotlin
val data = Bar3DChartData(
    xAxisLabels = listOf("12a", "6a", "12p"),
    yAxisLabels = listOf("Sat", "Sun"),
    points = listOf(Bar3DPoint(0, 0, 5f), Bar3DPoint(2, 1, 12f))
)
Bar3DChart(data = data, options = Bar3DOptions(initialYaw = -45f, initialPitch = 30f))
```

---

### 5. CalendarChart (矩阵日历热力图)

Generates contribution grids (similar to GitHub commits wall) supporting horizontal and vertical scrolling orientation.

#### Composable Signature
```kotlin
import io.github.composechart.charts.calendar.*

@Composable
fun CalendarChart(
    data: CalendarChartData,
    modifier: Modifier = Modifier,
    style: ChartStyle = ChartStyle.Default,
    options: CalendarOptions = CalendarOptions.Default
)
```

#### Models & Enums
```kotlin
data class CalendarDayData(val date: String, val value: Float, val label: String? = null, val tooltip: String? = null)
data class CalendarChartData(val year: Int, val days: List<CalendarDayData>)

enum class CalendarOrientation { Horizontal, Vertical }

data class CalendarOptions(
    val firstDayOfWeek: Int = 0, // 0 = Sunday, 1 = Monday
    val orientation: CalendarOrientation = CalendarOrientation.Horizontal,
    val cellSize: Dp = 16.dp,
    val cellGap: Dp = 2.dp,
    val cellCornerRadius: Dp = 2.dp,
    val emptyCellColor: Color? = null,
    val visualMapColors: List<Color> = listOf(Color(0xFFEBEDF0), Color(0xFF216E39)),
    val showVisualMap: Boolean = true,
    val weekdayLabels: List<String>? = null,
    val monthLabels: List<String>? = null,
    val labelColor: Color? = null
)
```

#### Code Example
```kotlin
val data = CalendarChartData(
    year = 2026,
    days = listOf(CalendarDayData("2026-01-01", 10f, label = "元旦"), CalendarDayData("2026-01-02", 50f))
)
CalendarChart(data = data, options = CalendarOptions(orientation = CalendarOrientation.Horizontal))
```

---

### 6. GaugeChart (经典与进度仪表盘)

Renders progress arcs or traditional mechanical needles with speed zones.

#### Composable Signature
```kotlin
import io.github.composechart.charts.gauge.*

@Composable
fun GaugeChart(
    data: GaugeChartData,
    modifier: Modifier = Modifier,
    style: ChartStyle = ChartStyle.Default,
    options: GaugeOptions = GaugeOptions.Default
)
```

#### Models & Enums
```kotlin
data class GaugeChartData(val value: Float, val min: Float = 0f, val max: Float = 100f, val name: String = "", val unit: String = "")

data class GaugeOptions(
    val startAngle: Float = 135f,
    val sweepAngle: Float = 270f,
    val axisLineWidth: Dp = 12.dp,
    val axisLineColors: List<Pair<Float, Color>> = listOf(0.3f to Color.Green, 0.7f to Color.Yellow, 1.0f to Color.Red),
    val axisBgColor: Color = Color.LightGray.copy(alpha = 0.25f),
    val showTicks: Boolean = true,
    val tickCount: Int = 10,
    val tickLength: Dp = 8.dp,
    val tickWidth: Dp = 1.5.dp,
    val tickColor: Color = Color.Gray.copy(alpha = 0.6f),
    val subTickCount: Int = 5,
    val subTickLength: Dp = 4.dp,
    val subTickWidth: Dp = 0.8.dp,
    val subTickColor: Color = Color.Gray.copy(alpha = 0.4f),
    val showTickLabels: Boolean = true,
    val tickLabelTextStyle: TextStyle? = null,
    val pointerWidth: Dp = 6.dp,
    val pointerLengthRatio: Float = 0.62f,
    val pointerColor: Color? = null,
    val centerCircleRadius: Dp = 10.dp,
    val centerCircleColor: Color? = null,
    val valueTextStyle: TextStyle? = null,
    val nameTextStyle: TextStyle? = null,
    val valueAboveName: Boolean = true,
    val customLabels: List<String>? = null,
    val pointerType: PointerType = PointerType.Triangle,
    val showPointer: Boolean = true,
    val isProgress: Boolean = false // If true, colors sweep only up to value percentage, mimicking progress bars
)

enum class PointerType { Triangle, ShortTriangle }
```

#### Code Example
```kotlin
val data = GaugeChartData(value = 68.5f, name = "当前进度", unit = "%")
GaugeChart(data = data, options = GaugeOptions(isProgress = true, showPointer = false))
```

---

### 7. RadarChart (战力对比雷达图)

Draws multi-dimensional attribute metrics supporting polygonal蛛网 configurations.

#### Composable Signature
```kotlin
import io.github.composechart.charts.radar.*

@Composable
fun RadarChart(
    data: RadarChartData,
    modifier: Modifier = Modifier,
    style: ChartStyle = ChartStyle.Default,
    legendOptions: LegendOptions = LegendOptions.Default
)
```

#### Models & Enums
```kotlin
data class RadarChartData(val indicators: List<RadarIndicator>, val series: List<RadarSeries>)
data class RadarIndicator(val name: String, val max: Float, val min: Float = 0f)

data class RadarSeries(
    val name: String,
    val values: List<Float>,
    val color: Color,
    val fillColor: Color? = null,
    val strokeWidth: Dp = 2.dp
)

// Hoisted under ChartStyle.radarOptions
data class RadarOptions(
    val shape: RadarShape = RadarShape.Polygon,
    val splitNumber: Int = 5,
    val gridColor: Color = Color(0xFFD3D3D3),
    val gridLineWidth: Dp = 0.8.dp,
    val axisLineColor: Color = Color(0xFFD3D3D3),
    val axisLineWidth: Dp = 0.8.dp,
    val drawScaleLabel: Boolean = true,
    val symbolSize: Dp = 4.dp
)

enum class RadarShape { Polygon, Circle }
```

#### Code Example
```kotlin
val data = RadarChartData(
    indicators = listOf(RadarIndicator("销售", 100f), RadarIndicator("客服", 100f), RadarIndicator("研发", 100f)),
    series = listOf(RadarSeries("预算分配", listOf(80f, 90f, 65f), Color(0xFF5470C6)))
)
RadarChart(data = data, modifier = Modifier.fillMaxSize())
```

---

### 8. KLineChart (个股日K线成交量图)

Integrates candlestick bars, transaction volume sections, and multi-line rolling averages (MA5, MA10, MA20).

#### Composable Signature
```kotlin
import io.github.composechart.charts.kline.*

@Composable
fun KLineChart(
    data: KLineChartData,
    modifier: Modifier = Modifier,
    style: ChartStyle = ChartStyle.Default,
    kLineStyle: KLineStyle = KLineStyle.Default,
    viewportState: ViewportState = rememberViewportState(),
    interactionState: InteractionState = rememberInteractionState()
)
```

#### Models & Enums
```kotlin
data class KLineChartData(val entries: List<KLineEntry>)
data class KLineEntry(val time: String, val open: Float, val close: Float, val high: Float, val low: Float, val volume: Float)

data class KLineStyle(
    val upColor: Color = Color(0xFFEE6666),
    val downColor: Color = Color(0xFF3BA272),
    val upFilled: Boolean = false,
    val downFilled: Boolean = true,
    val candleWidthRatio: Float = 0.7f,
    val shadowLineWidth: Dp = 1.dp,
    val ma5Color: Color = Color(0xFF5470C6),
    val ma10Color: Color = Color(0xFFFAC858),
    val ma20Color: Color = Color(0xFF9A60B4),
    val maLineWidth: Dp = 1.dp
) {
    companion object {
        val Default = KLineStyle()
        val International = KLineStyle(upColor = Color(0xFF3BA272), downColor = Color(0xFFEE6666), upFilled = true, downFilled = true)
    }
}
```

#### Code Example
```kotlin
val data = KLineChartData(
    entries = listOf(KLineEntry("2026-06-01", 2300f, 2350f, 2360f, 2290f, 120000f))
)
KLineChart(data = data, kLineStyle = KLineStyle.International, modifier = Modifier.fillMaxSize())
```

---

### 9. ScatterChart (告警监控散点图/气泡图)

Features custom symbols, dimensional visual mappings, and ripple radar warnings.

#### Composable Signature
```kotlin
import io.github.composechart.charts.scatter.*

@Composable
fun ScatterChart(
    data: ScatterChartData,
    modifier: Modifier = Modifier,
    style: ChartStyle = ChartStyle.Default,
    viewportState: ViewportState = rememberViewportState(),
    interactionState: InteractionState = rememberInteractionState(),
    tooltipOptions: TooltipOptions = TooltipOptions.Default.copy(trigger = TriggerType.Item, indicatorStyle = IndicatorStyle.Cross),
    legendOptions: LegendOptions = LegendOptions.Default,
    visualMap: ScatterVisualMap? = null,
    onValueSelected: ((point: ScatterPoint, series: ScatterSeries) -> Unit)? = null
)
```

#### Models & Enums
```kotlin
data class ScatterChartData(val series: List<ScatterSeries>, val xLabels: List<String>? = null) // xLabels = null triggers numeric coordinate axis

data class ScatterSeries(
    val name: String,
    val points: List<ScatterPoint>,
    val color: Color,
    val symbol: ScatterSymbolType = ScatterSymbolType.Circle,
    val symbolSize: Dp = 8.dp,
    val strokeWidth: Dp = 1.dp,
    val strokeColor: Color? = null,
    val effectScatter: Boolean = false, // If true, radiates animated warning rings
    val effectOptions: EffectOptions = EffectOptions()
)

data class ScatterPoint(val x: Float, val y: Float, val value: Float? = null, val name: String? = null)

enum class ScatterSymbolType { Circle, Square, Triangle, Diamond }

data class ScatterVisualMap(
    val min: Float, val max: Float,
    val minSize: Dp = 6.dp, val maxSize: Dp = 36.dp,
    val colorRange: List<Color> = emptyList(),
    val alphaRange: Pair<Float, Float>? = null
)

data class EffectOptions(val rippleRadiusRatio: Float = 2.2f, val rippleCount: Int = 2, val durationMs: Int = 1800)
```

#### Code Example
```kotlin
val data = ScatterChartData(
    series = listOf(
        ScatterSeries("告警事件", listOf(ScatterPoint(11f, 8.33f)), Color(0xFFEE6666), effectScatter = true)
    )
)
ScatterChart(data = data)
```

---

### 10. BoxplotChart (盒须箱线图)

Renders statistical summary plots including upper/lower quartile, median, and outlier dots.

#### Composable Signature
```kotlin
import io.github.composechart.charts.boxplot.*

@Composable
fun BoxplotChart(
    data: BoxplotChartData,
    modifier: Modifier = Modifier,
    style: ChartStyle = ChartStyle.Default,
    viewportState: ViewportState = rememberViewportState(),
    interactionState: InteractionState = rememberInteractionState(),
    tooltipOptions: TooltipOptions = TooltipOptions.Default,
    legendOptions: LegendOptions = LegendOptions.Default,
    onValueSelected: ((point: BoxplotPoint, series: BoxplotSeries) -> Unit)? = null
)
```

#### Models & Enums
```kotlin
data class BoxplotChartData(val xLabels: List<String>, val series: List<BoxplotSeries>)

data class BoxplotSeries(
    val name: String,
    val points: List<BoxplotPoint>,
    val color: Color,
    val fillColor: Color? = null,
    val boxWidthRatio: Float = 0.45f,
    val whiskerWidthRatio: Float = 0.22f,
    val outliers: List<BoxplotOutlier> = emptyList()
)

data class BoxplotPoint(val min: Float, val q1: Float, val median: Float, val q3: Float, val max: Float)
data class BoxplotOutlier(val xIndex: Int, val value: Float, val name: String? = null)
```

#### Code Example
```kotlin
val data = BoxplotChartData(
    xLabels = listOf("轴承A"),
    series = listOf(
        BoxplotSeries(
            name = "缺陷分布",
            points = listOf(BoxplotPoint(655f, 850f, 940f, 980f, 1075f)),
            color = Color(0xFF91CC75),
            outliers = listOf(BoxplotOutlier(0, 1150f, "严重离群值"))
        )
    )
)
BoxplotChart(data = data, modifier = Modifier.fillMaxSize())
```

---

### 11. FunnelChart (流失转化漏斗图)

Renders stacked trapezoid sections representing workflow conversions.

#### Composable Signature
```kotlin
import io.github.composechart.charts.funnel.*

@Composable
fun FunnelChart(
    data: FunnelChartData,
    modifier: Modifier = Modifier,
    style: ChartStyle = ChartStyle.Default,
    options: FunnelOptions = FunnelOptions.Default,
    viewportState: ViewportState = rememberViewportState(),
    interactionState: InteractionState = rememberInteractionState(),
    tooltipOptions: TooltipOptions = TooltipOptions.Default.copy(trigger = TriggerType.Item),
    legendOptions: LegendOptions = LegendOptions.Default,
    onSliceSelected: ((slice: FunnelSlice) -> Unit)? = null
)
```

#### Models & Enums
```kotlin
data class FunnelChartData(val slices: List<FunnelSlice>)
data class FunnelSlice(val name: String, val value: Float, val color: Color? = null)

data class FunnelOptions(
    val align: FunnelAlign = FunnelAlign.Center,
    val sort: FunnelSort = FunnelSort.Descending,
    val gap: Dp = 4.dp,
    val cornerRadius: Dp = 4.dp,
    val minWidthRatio: Float = 0.08f,
    val maxWidthRatio: Float = 0.85f,
    val showLabels: Boolean = true,
    val labelTextStyle: TextStyle? = null,
    val showConversion: Boolean = true,
    val conversionTextStyle: TextStyle? = null
)

enum class FunnelAlign { Left, Center, Right }
enum class FunnelSort { Descending, Ascending, None }
```

#### Code Example
```kotlin
val data = FunnelChartData(
    slices = listOf(FunnelSlice("展现", 100f), FunnelSlice("点击", 80f), FunnelSlice("订单", 20f))
)
FunnelChart(data = data, options = FunnelOptions(align = FunnelAlign.Center))
```

---

### 12. MixedChart (折线柱状图混合图)

Integrates both `LineSeries` and `BarSeries` onto a single grid sharing dual vertical axes.

#### Composable Signature
```kotlin
import io.github.composechart.charts.mixed.*

@Composable
fun MixedChart(
    data: MixedChartData,
    modifier: Modifier = Modifier,
    style: ChartStyle = ChartStyle.Default,
    viewportState: ViewportState = rememberViewportState(),
    interactionState: InteractionState = rememberInteractionState(),
    tooltipOptions: TooltipOptions = TooltipOptions.Default,
    legendOptions: LegendOptions = LegendOptions.Default,
    dataZoomOptions: DataZoomOptions? = null,
    onValueSelected: ((index: Int, seriesName: String, value: Float) -> Unit)? = null
)
```

#### Models & Enums
```kotlin
data class MixedChartData(
    val xLabels: List<String>,
    val lineSeries: List<LineSeries> = emptyList(),
    val barSeries: List<BarSeries> = emptyList()
)
```

#### Code Example
```kotlin
val data = MixedChartData(
    xLabels = listOf("Jan", "Feb", "Mar"),
    barSeries = listOf(BarSeries("降水量", listOf(BarValue(2.0f), BarValue(4.9f), BarValue(7.0f)), Color(0xFF5470C6), yAxisIndex = 0)),
    lineSeries = listOf(LineSeries("平均温度", listOf(LinePoint(0f, 2.0f), LinePoint(1f, 2.2f), LinePoint(2f, 3.3f)), Color(0xFFFAC858), yAxisIndex = 1))
)
val style = ChartStyle.Default.copy(
    y2AxisOptions = AxisOptions(show = true, name = "Temp (℃)") // Display the secondary Y-axis on the right
)
MixedChart(data = data, style = style)
```

---

### 13. PolarBarChart (极坐标系扇形/圆环图)

Plots sectoral bars onto a radial coordinate frame. Supports Radial fan slices or concentric multitrack circles.

#### Composable Signature
```kotlin
import io.github.composechart.charts.polar.*

@Composable
fun PolarBarChart(
    data: PolarChartData,
    modifier: Modifier = Modifier,
    style: ChartStyle = ChartStyle.Default,
    legendOptions: LegendOptions = LegendOptions.Default,
    horizontal: Boolean = false // false = Radial bar sectors; true = Tangential multi-layered tracks
)
```

#### Models & Enums
```kotlin
data class PolarChartData(val xLabels: List<String>, val series: List<PolarBarSeries>)
data class PolarBarSeries(val name: String, val values: List<PolarBarValue>, val color: Color, val labelPosition: PolarLabelPosition = PolarLabelPosition.Middle)
data class PolarBarValue(val value: Float)

enum class PolarLabelPosition { Middle, End, None }

// Hoisted under ChartStyle.polarOptions
data class PolarOptions(
    val show: Boolean = true,
    val centerX: Float = 0.5f,
    val centerY: Float = 0.5f,
    val startAngle: Float = 90f, // 0 degrees relative direction (90f = 12 o'clock)
    val endAngle: Float = 360f + 90f,
    val radiusStepNumber: Int = 5,
    val gridColor: Color = Color(0xFFD3D3D3),
    val gridLineWidth: Dp = 0.8.dp,
    val axisLineColor: Color = Color(0xFFD3D3D3),
    val axisLineWidth: Dp = 0.8.dp,
    val drawRadiusLabel: Boolean = true,
    val drawAngleLabel: Boolean = true
)
```

#### Code Example
```kotlin
val data = PolarChartData(
    xLabels = listOf("CatA", "CatB"),
    series = listOf(PolarBarSeries("系列A", listOf(PolarBarValue(80f), PolarBarValue(60f)), Color(0xFF73C0DE)))
)
PolarBarChart(data = data, horizontal = true)
```

---

## 💡 Key Development Tips for AI Assistants

1. **Dark Mode Adaptation**: Pass `ChartStyle.Dark` into the `style` parameter. The chart colors, label fonts, and grid splits adjust themselves automatically.
2. **Horizontal Layouts**: To rotate a bar chart to a horizontal landscape, simply toggle `horizontal = true` on `BarChart`.
3. **Double Axes Mixed Graphs**: For dual axis rendering, map series `yAxisIndex = 0` (for left Y-axis) and `yAxisIndex = 1` (for right Y-axis) on `MixedChart` data models. Remember to set `style.y2AxisOptions = AxisOptions(show = true)` to make the right axis visible.
4. **Scrollable Viewports**: Hoist `val viewportState = rememberViewportState(viewportMode = ViewportMode.Scrollable(5))` and bind it to `LineChart`, `BarChart`, `KLineChart`, or `BoxplotChart` to handle massive datasets gracefully with horizontal panning gestures.
