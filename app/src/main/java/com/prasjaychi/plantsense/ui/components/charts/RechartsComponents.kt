package com.prasjaychi.plantsense.ui.components.charts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prasjaychi.plantsense.ui.theme.Amber500
import com.prasjaychi.plantsense.ui.theme.Cyan400
import com.prasjaychi.plantsense.ui.theme.Cyan500
import com.prasjaychi.plantsense.ui.theme.Emerald400
import com.prasjaychi.plantsense.ui.theme.Emerald500
import com.prasjaychi.plantsense.ui.theme.Emerald600
import com.prasjaychi.plantsense.ui.theme.Indigo500
import com.prasjaychi.plantsense.ui.theme.Indigo600
import com.prasjaychi.plantsense.ui.theme.Rose500
import com.prasjaychi.plantsense.ui.theme.Violet400
import com.prasjaychi.plantsense.ui.theme.Violet500
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Recharts-inspired Botanical Chart Palette.
 */
val RechartsColors = listOf(
    Emerald500,
    Cyan500,
    Indigo500,
    Amber500,
    Violet500,
    Rose500,
    Color(0xFF0EA5E9),
    Color(0xFF10B981),
    Color(0xFFF97316),
    Color(0xFF8B5CF6)
)

data class RechartsDataPoint(
    val label: String,
    val value: Float,
    val secondaryValue: Float? = null,
    val description: String = "",
    val category: String = "",
    val timestamp: Long = 0L,
    val color: Color = Emerald500
)

data class RechartsCategoryShare(
    val name: String,
    val count: Int,
    val percentage: Float,
    val color: Color,
    val subtext: String = ""
)

data class RadarDimension(
    val axisLabel: String,
    val score: Float, // 0.0 to 100.0
    val maxScore: Float = 100f
)

/**
 * Recharts-inspired KPI Metric Stat Card with Mini Sparkline.
 */
@Composable
fun RechartsStatCard(
    title: String,
    value: String,
    subtitle: String,
    sparklineData: List<Float>,
    modifier: Modifier = Modifier,
    delta: String? = null,
    isPositiveDelta: Boolean = true,
    accentColor: Color = Emerald500
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )

                if (delta != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isPositiveDelta) Emerald500.copy(alpha = 0.12f) else Rose500.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isPositiveDelta) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = if (isPositiveDelta) Emerald600 else Rose500,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = delta,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPositiveDelta) Emerald600 else Rose500
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = value,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // Mini Sparkline (Recharts tiny LineChart)
                if (sparklineData.size >= 2) {
                    Box(
                        modifier = Modifier
                            .width(68.dp)
                            .height(30.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val min = sparklineData.minOrNull() ?: 0f
                            val max = (sparklineData.maxOrNull() ?: 1f).coerceAtLeast(min + 0.1f)
                            val stepX = size.width / (sparklineData.size - 1).coerceAtLeast(1)

                            val points = sparklineData.mapIndexed { idx, v ->
                                val x = idx * stepX
                                val normY = ((v - min) / (max - min)).coerceIn(0f, 1f)
                                val y = size.height - (normY * (size.height - 4.dp.toPx())) - 2.dp.toPx()
                                Offset(x, y)
                            }

                            val path = Path().apply {
                                points.forEachIndexed { i, p ->
                                    if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                                }
                            }

                            drawPath(
                                path = path,
                                color = accentColor,
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Recharts-inspired Area & Spline Line Chart with Gradient Fill & Interactive Tooltip.
 */
@Composable
fun RechartsAreaChart(
    dataPoints: List<RechartsDataPoint>,
    modifier: Modifier = Modifier,
    chartTitle: String = "Plant Vitality Trend Curve",
    chartSubtitle: String = "Historical botanical health score progression",
    primaryColor: Color = Emerald500,
    secondaryColor: Color = Cyan400,
    unit: String = "%",
    referenceThreshold: Float? = 80f
) {
    if (dataPoints.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No botanical records recorded yet to render trend curve.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }
        return
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val maxVal = (dataPoints.maxOfOrNull { it.value } ?: 100f).coerceAtLeast(100f)
    val minVal = (dataPoints.minOfOrNull { it.value } ?: 0f).coerceAtMost(0f)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = chartTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = chartSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Legend Pill
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = primaryColor.copy(alpha = 0.12f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = primaryColor,
                        modifier = Modifier.size(6.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Health Score",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(dataPoints) {
                        detectTapGestures { tapOffset ->
                            val stepX = size.width / (dataPoints.size - 1).coerceAtLeast(1)
                            val idx = (tapOffset.x / stepX).toInt().coerceIn(0, dataPoints.size - 1)
                            selectedIndex = idx
                        }
                    }
                    .pointerInput(dataPoints) {
                        detectDragGestures { change, _ ->
                            val stepX = size.width / (dataPoints.size - 1).coerceAtLeast(1)
                            val idx = (change.position.x / stepX).toInt().coerceIn(0, dataPoints.size - 1)
                            selectedIndex = idx
                        }
                    }
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val paddingBottom = 20.dp.toPx()
                val paddingTop = 12.dp.toPx()
                val chartHeight = canvasHeight - paddingBottom - paddingTop

                // 1. Cartesian Grid Horizontal Lines (Recharts style)
                val gridLines = 4
                for (i in 0..gridLines) {
                    val y = paddingTop + (chartHeight / gridLines) * i
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.35f),
                        start = Offset(0f, y),
                        end = Offset(canvasWidth, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    )
                }

                // 2. Reference Threshold Line (e.g. 80% Optimal line)
                if (referenceThreshold != null) {
                    val thresholdRatio = ((referenceThreshold - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)
                    val refY = paddingTop + chartHeight * (1f - thresholdRatio)
                    drawLine(
                        color = Emerald500.copy(alpha = 0.6f),
                        start = Offset(0f, refY),
                        end = Offset(canvasWidth, refY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f), 0f)
                    )
                }

                // 3. Compute Coordinates
                val stepX = canvasWidth / (dataPoints.size - 1).coerceAtLeast(1)
                val points = dataPoints.mapIndexed { index, dp ->
                    val x = index * stepX
                    val normalizedY = ((dp.value - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)
                    val y = paddingTop + chartHeight * (1f - normalizedY)
                    Offset(x, y)
                }

                // 4. Draw Smooth Cubic Bézier Gradient Area
                if (points.size >= 2) {
                    val areaPath = Path().apply {
                        moveTo(points.first().x, canvasHeight - paddingBottom)
                        lineTo(points.first().x, points.first().y)

                        for (i in 0 until points.size - 1) {
                            val p0 = points[i]
                            val p1 = points[i + 1]
                            val controlX1 = p0.x + (p1.x - p0.x) / 2
                            val controlY1 = p0.y
                            val controlX2 = p0.x + (p1.x - p0.x) / 2
                            val controlY2 = p1.y
                            cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                        }

                        lineTo(points.last().x, canvasHeight - paddingBottom)
                        close()
                    }

                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.38f),
                                secondaryColor.copy(alpha = 0.15f),
                                Color.Transparent
                            ),
                            startY = paddingTop,
                            endY = canvasHeight - paddingBottom
                        )
                    )

                    // 5. Draw Smooth Stroke Spline
                    val strokePath = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 0 until points.size - 1) {
                            val p0 = points[i]
                            val p1 = points[i + 1]
                            val controlX1 = p0.x + (p1.x - p0.x) / 2
                            val controlY1 = p0.y
                            val controlX2 = p0.x + (p1.x - p0.x) / 2
                            val controlY2 = p1.y
                            cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                        }
                    }

                    drawPath(
                        path = strokePath,
                        brush = Brush.horizontalGradient(listOf(primaryColor, secondaryColor)),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // 6. Draw Dots & Tooltip Cursor
                points.forEachIndexed { i, pt ->
                    val isSelected = selectedIndex == i
                    val radius = if (isSelected) 6.dp.toPx() else 3.5.dp.toPx()

                    if (isSelected) {
                        // Vertical scrub line
                        drawLine(
                            color = primaryColor.copy(alpha = 0.5f),
                            start = Offset(pt.x, paddingTop),
                            end = Offset(pt.x, canvasHeight - paddingBottom),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                    }

                    drawCircle(
                        color = Color.White,
                        radius = radius + 2.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = if (isSelected) primaryColor else secondaryColor,
                        radius = radius,
                        center = pt
                    )
                }
            }
        }

        // X-Axis Tick Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            dataPoints.forEachIndexed { idx, dp ->
                if (idx == 0 || idx == dataPoints.size / 2 || idx == dataPoints.size - 1) {
                    Text(
                        text = dp.label,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        // Interactive Tooltip Card
        selectedIndex?.let { idx ->
            if (idx in dataPoints.indices) {
                val point = dataPoints[idx]
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = primaryColor.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "${point.label} • ${point.category.ifBlank { "Scan" }}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (point.description.isNotBlank()) {
                                Text(
                                    text = point.description,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = primaryColor
                        ) {
                            Text(
                                text = "${point.value.toInt()}$unit",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Recharts-inspired Categorical Bar Chart with Rounded Caps and Value Badges.
 */
@Composable
fun RechartsBarChart(
    categories: List<RechartsCategoryShare>,
    modifier: Modifier = Modifier,
    chartTitle: String = "Diagnosed Species & Family Distribution",
    chartSubtitle: String = "Categorical frequency recorded in Room database",
    onSelectCategory: ((RechartsCategoryShare) -> Unit)? = null
) {
    if (categories.isEmpty()) return

    val maxCount = (categories.maxOfOrNull { it.count } ?: 1).coerceAtLeast(1)
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = chartTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = chartSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            categories.forEach { cat ->
                val isSelected = selectedCategory == cat.name
                val fraction = (cat.count.toFloat() / maxCount.toFloat()).coerceIn(0.06f, 1f)
                val animatedFraction by animateFloatAsState(
                    targetValue = fraction,
                    animationSpec = tween(durationMillis = 650),
                    label = "bar_anim_${cat.name}"
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) cat.color.copy(alpha = 0.1f) else Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedCategory = if (isSelected) null else cat.name
                            onSelectCategory?.invoke(cat)
                        }
                ) {
                    Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = cat.color,
                                    modifier = Modifier.size(8.dp)
                                ) {}
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = cat.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (cat.subtext.isNotBlank()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "• ${cat.subtext}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.outline,
                                        maxLines = 1
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${cat.count} scans",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(${String.format(Locale.US, "%.0f%%", cat.percentage * 100)})",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(5.dp))

                        // Recharts styled pill bar with gradient & rounded cap
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedFraction)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(cat.color.copy(alpha = 0.75f), cat.color)
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Recharts-inspired Donut / Radial Chart with Center Summary and Segment Legend.
 */
@Composable
fun RechartsDonutChartComponent(
    slices: List<RechartsCategoryShare>,
    modifier: Modifier = Modifier,
    centerTitle: String = "100%",
    centerSubtitle: String = "Botanical Health"
) {
    if (slices.isEmpty()) return

    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Donut Canvas
        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(slices) {
                        detectTapGestures { tapOffset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val dx = tapOffset.x - center.x
                            val dy = tapOffset.y - center.y
                            var angle = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            if (angle < 0) angle += 360f

                            var start = -90f
                            slices.forEachIndexed { i, s ->
                                val sweep = s.percentage * 360f
                                val normalizedStart = (start + 360f) % 360f
                                val normalizedEnd = (normalizedStart + sweep) % 360f

                                val isInSlice = if (normalizedStart < normalizedEnd) {
                                    angle in normalizedStart..normalizedEnd
                                } else {
                                    angle >= normalizedStart || angle <= normalizedEnd
                                }

                                if (isInSlice) {
                                    selectedIndex = if (selectedIndex == i) null else i
                                    return@detectTapGestures
                                }
                                start += sweep
                            }
                        }
                    }
            ) {
                val strokeWidth = 24.dp.toPx()
                val diameter = size.minDimension - strokeWidth
                val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                val arcSize = Size(diameter, diameter)

                var startAngle = -90f
                slices.forEachIndexed { i, slice ->
                    val sweepAngle = slice.percentage * 360f
                    val isSelected = selectedIndex == i

                    drawArc(
                        color = if (isSelected) slice.color else slice.color.copy(alpha = 0.9f),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle - 2.5f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(
                            width = if (isSelected) strokeWidth + 6.dp.toPx() else strokeWidth,
                            cap = StrokeCap.Round
                        )
                    )
                    startAngle += sweepAngle
                }
            }

            // Center KPI Label
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = centerTitle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = centerSubtitle,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Legend Pills
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            slices.forEachIndexed { idx, slice ->
                val isSelected = selectedIndex == idx
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) slice.color.copy(alpha = 0.12f) else Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedIndex = if (isSelected) null else idx }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = slice.color,
                                modifier = Modifier.size(8.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = slice.name,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Text(
                            text = String.format(Locale.US, "%.0f%%", slice.percentage * 100),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = slice.color
                        )
                    }
                }
            }
        }
    }
}

/**
 * Recharts-inspired 5-Axis Botanical Radar / Spider Chart.
 */
@Composable
fun RechartsRadarChart(
    dimensions: List<RadarDimension>,
    modifier: Modifier = Modifier,
    polygonColor: Color = Emerald500
) {
    if (dimensions.size < 3) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.minDimension / 2f) - 32.dp.toPx()
            val count = dimensions.size
            val angleStep = (2 * Math.PI / count).toFloat()

            // 1. Concentric Web Rings (25%, 50%, 75%, 100%)
            val rings = 4
            for (r in 1..rings) {
                val currentR = radius * (r.toFloat() / rings.toFloat())
                val ringPath = Path()
                for (i in 0 until count) {
                    val angle = -Math.PI / 2 + i * angleStep
                    val x = center.x + (currentR * cos(angle)).toFloat()
                    val y = center.y + (currentR * sin(angle)).toFloat()
                    if (i == 0) ringPath.moveTo(x, y) else ringPath.lineTo(x, y)
                }
                ringPath.close()
                drawPath(
                    path = ringPath,
                    color = Color.LightGray.copy(alpha = 0.35f),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // 2. Radial Axis Lines
            for (i in 0 until count) {
                val angle = -Math.PI / 2 + i * angleStep
                val x = center.x + (radius * cos(angle)).toFloat()
                val y = center.y + (radius * sin(angle)).toFloat()
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.45f),
                    start = center,
                    end = Offset(x, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // 3. Shaded Data Polygon Area
            val dataPolygonPath = Path()
            val dataPoints = dimensions.mapIndexed { i, dim ->
                val ratio = (dim.score / dim.maxScore).coerceIn(0.1f, 1f)
                val currentR = radius * ratio
                val angle = -Math.PI / 2 + i * angleStep
                val x = center.x + (currentR * cos(angle)).toFloat()
                val y = center.y + (currentR * sin(angle)).toFloat()
                Offset(x, y)
            }

            dataPoints.forEachIndexed { i, pt ->
                if (i == 0) dataPolygonPath.moveTo(pt.x, pt.y) else dataPolygonPath.lineTo(pt.x, pt.y)
            }
            dataPolygonPath.close()

            drawPath(
                path = dataPolygonPath,
                color = polygonColor.copy(alpha = 0.28f)
            )

            drawPath(
                path = dataPolygonPath,
                color = polygonColor,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // 4. Data Vertex Dots
            dataPoints.forEach { pt ->
                drawCircle(color = Color.White, radius = 4.dp.toPx(), center = pt)
                drawCircle(color = polygonColor, radius = 2.5.dp.toPx(), center = pt)
            }
        }

        // Radar Axis Labels around canvas
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = dimensions.getOrNull(0)?.axisLabel ?: "",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = dimensions.getOrNull(4)?.axisLabel ?: "",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = dimensions.getOrNull(1)?.axisLabel ?: "",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Text(
                    text = dimensions.getOrNull(3)?.axisLabel ?: "",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = dimensions.getOrNull(2)?.axisLabel ?: "",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
