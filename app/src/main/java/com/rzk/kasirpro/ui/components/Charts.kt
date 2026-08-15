package com.rzk.kasirpro.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rzk.kasirpro.ui.theme.kasirColors
import kotlin.math.max
import kotlin.math.min

data class BarDatum(
    val label: String,
    val value: Long,
    /** Shown when the bar is selected; falls back to the formatted value. */
    val display: String = ""
)

/**
 * Single-series column chart for "sales per day" / "sales per hour".
 *
 * One series means no legend is needed — the section title names it. Rather than labelling
 * every column (noise), only the tallest is labelled by default, and tapping any column
 * reveals its own value. That tap is the touch equivalent of a hover tooltip.
 */
@Composable
fun BarChart(
    data: List<BarDatum>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    height: androidx.compose.ui.unit.Dp = 150.dp,
    emptyLabel: String = ""
) {
    if (data.isEmpty()) {
        Box(modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) {
            Text(
                emptyLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    var selected by remember(data) { mutableIntStateOf(-1) }
    val peakIndex = remember(data) { data.indices.maxByOrNull { data[it].value } ?: 0 }
    val highlighted = if (selected >= 0) selected else peakIndex
    val maxValue = remember(data) { max(1L, data.maxOf { it.value }) }

    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier.fillMaxWidth()) {
        // Value callout for the highlighted column, so the chart always states one number.
        Text(
            text = data.getOrNull(highlighted)?.let {
                "${it.label} • ${it.display.ifBlank { it.value.toString() }}"
            }.orEmpty(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(6.dp))
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
                .pointerInput(data) {
                    detectTapGestures { offset ->
                        val slot = size.width / data.size.toFloat()
                        selected = (offset.x / slot).toInt().coerceIn(0, data.lastIndex)
                    }
                }
        ) {
            val slot = size.width / data.size
            // 2px of surface between adjacent fills keeps columns from fusing into a block.
            val gap = min(slot * 0.32f, 10f)
            val barWidth = (slot - gap).coerceAtLeast(3f)
            val radius = CornerRadius(min(barWidth / 2f, 8f), min(barWidth / 2f, 8f))

            data.forEachIndexed { index, datum ->
                val ratio = datum.value.toFloat() / maxValue.toFloat()
                val barHeight = (size.height * ratio).coerceAtLeast(if (datum.value > 0) 3f else 1.5f)
                val left = index * slot + gap / 2f
                val top = size.height - barHeight

                // Recessive track behind every column gives the eye a shared baseline
                // without drawing gridlines across the plot.
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(left, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = radius
                )
                drawRoundRect(
                    color = if (index == highlighted) barColor else barColor.copy(alpha = 0.55f),
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = radius
                )
            }
            drawLine(
                color = onSurfaceVariant.copy(alpha = 0.25f),
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = 2f
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            data.forEachIndexed { index, datum ->
                Text(
                    datum.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (index == highlighted) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (index == highlighted) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

data class GroupedBarDatum(
    val label: String,
    val primary: Long,
    val secondary: Long
)

/**
 * Two-series columns — money in beside money out. Both series share one axis (never two),
 * and the legend is mandatory here because colour is now carrying identity.
 */
@Composable
fun GroupedBarChart(
    data: List<GroupedBarDatum>,
    primaryLabel: String,
    secondaryLabel: String,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 140.dp
) {
    val inColor = MaterialTheme.kasirColors.cashIn
    val outColor = MaterialTheme.kasirColors.cashOut
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val baseline = MaterialTheme.colorScheme.onSurfaceVariant

    if (data.isEmpty()) return

    val maxValue = remember(data) {
        max(1L, data.maxOf { max(it.primary, it.secondary) })
    }

    Column(modifier.fillMaxWidth()) {
        ChartLegend(
            entries = listOf(primaryLabel to inColor, secondaryLabel to outColor)
        )
        Spacer(Modifier.height(10.dp))
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            val slot = size.width / data.size
            val groupGap = min(slot * 0.3f, 12f)
            val barWidth = ((slot - groupGap) / 2f - 2f).coerceAtLeast(3f)
            val radius = CornerRadius(min(barWidth / 2f, 6f), min(barWidth / 2f, 6f))

            data.forEachIndexed { index, datum ->
                val base = index * slot + groupGap / 2f
                listOf(datum.primary to inColor, datum.secondary to outColor)
                    .forEachIndexed { series, (value, color) ->
                        val left = base + series * (barWidth + 2f)
                        val h = (size.height * value.toFloat() / maxValue.toFloat())
                            .coerceAtLeast(if (value > 0) 3f else 1f)
                        drawRoundRect(
                            color = trackColor,
                            topLeft = Offset(left, 0f),
                            size = Size(barWidth, size.height),
                            cornerRadius = radius
                        )
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(left, size.height - h),
                            size = Size(barWidth, h),
                            cornerRadius = radius
                        )
                    }
            }
            drawLine(
                color = baseline.copy(alpha = 0.25f),
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = 2f
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            data.forEach { datum ->
                Text(
                    datum.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

data class SliceDatum(
    val label: String,
    val value: Long,
    val display: String,
    val color: Color
)

/**
 * Part-to-whole for payment mix — the one job a ring genuinely does better than bars,
 * and only because there are a handful of slices that really do sum to 100%.
 * Every slice is direct-labelled in the legend with its own value, so the ring is a
 * summary, not the only way to read the data.
 */
@Composable
fun DonutChart(
    slices: List<SliceDatum>,
    centerLabel: String,
    centerValue: String,
    modifier: Modifier = Modifier,
    diameter: androidx.compose.ui.unit.Dp = 132.dp
) {
    if (slices.isEmpty()) return
    val total = remember(slices) { max(1L, slices.sumOf { it.value }) }
    val surface = MaterialTheme.colorScheme.surface
    val sweepProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(600),
        label = "donutSweep"
    )

    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(diameter), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(diameter)) {
                val stroke = size.minDimension * 0.22f
                var start = -90f
                slices.forEach { slice ->
                    val sweep = 360f * (slice.value.toFloat() / total.toFloat()) * sweepProgress
                    drawArc(
                        color = slice.color,
                        startAngle = start,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(stroke / 2f, stroke / 2f),
                        size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(width = stroke)
                    )
                    // A 2px surface ring keeps neighbouring arcs from bleeding together.
                    drawArc(
                        color = surface,
                        startAngle = start + sweep - 0.6f,
                        sweepAngle = 1.2f,
                        useCenter = false,
                        topLeft = Offset(stroke / 2f, stroke / 2f),
                        size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(width = stroke)
                    )
                    start += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    centerValue,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    centerLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            slices.forEach { slice ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .background(slice.color, RoundedCornerShape(3.dp))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        slice.label,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        slice.display,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Ranked horizontal bars — the right form for "best sellers" because the labels are long
 * text and the job is comparing magnitudes, not reading a part-to-whole share.
 */
@Composable
fun RankBarRow(
    rank: Int,
    label: String,
    value: String,
    supporting: String,
    fraction: Float,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary
) {
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(500),
        label = "rankBar"
    )
    Column(modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(22.dp)
                    .background(barColor.copy(alpha = 0.16f), RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    rank.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = barColor
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(8.dp))
            Text(
                value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .weight(1f)
                    .height(8.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHighest,
                        RoundedCornerShape(4.dp)
                    )
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(animated)
                        .height(8.dp)
                        .background(barColor, RoundedCornerShape(4.dp))
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                supporting,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Legend chips. Present whenever two or more series share a plot. */
@Composable
fun ChartLegend(
    entries: List<Pair<String, Color>>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        entries.forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .background(color, RoundedCornerShape(3.dp))
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
