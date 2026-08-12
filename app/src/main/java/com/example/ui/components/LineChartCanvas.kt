package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.NightlySummaryEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

fun formatXAxisLabel(
    index: Int,
    sortedList: List<NightlySummaryEntity>
): String {
    val currentItem = sortedList[index]
    val parts = currentItem.dateString.split("-")
    if (parts.size != 3) {
        val sdfYear = SimpleDateFormat("yyyy", Locale.getDefault())
        val sdfMonth = SimpleDateFormat("MM", Locale.getDefault())
        val sdfDay = SimpleDateFormat("dd", Locale.getDefault())
        val curDate = Date(currentItem.timestamp)
        return formatLabelFromParts(
            index, sortedList,
            sdfYear.format(curDate),
            sdfMonth.format(curDate),
            sdfDay.format(curDate)
        )
    } else {
        return formatLabelFromParts(index, sortedList, parts[0], parts[1], parts[2])
    }
}

private fun formatLabelFromParts(
    index: Int,
    sortedList: List<NightlySummaryEntity>,
    curY: String,
    curM: String,
    curD: String
): String {
    if (index == 0) {
        val currentCalendarYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
        return if (curY != currentCalendarYear) {
            "$curD-$curM-$curY"
        } else {
            "$curD-$curM"
        }
    }

    val prevItem = sortedList[index - 1]
    val prevParts = prevItem.dateString.split("-")
    val (prevY, prevM) = if (prevParts.size == 3) {
        Pair(prevParts[0], prevParts[1])
    } else {
        val sdfYear = SimpleDateFormat("yyyy", Locale.getDefault())
        val sdfMonth = SimpleDateFormat("MM", Locale.getDefault())
        val prevDate = Date(prevItem.timestamp)
        Pair(sdfYear.format(prevDate), sdfMonth.format(prevDate))
    }

    return when {
        curY != prevY -> "$curD-$curM-$curY"
        curM != prevM -> "$curD-$curM"
        else -> curD
    }
}

@Composable
fun LineChartCanvas(
    summaries: List<NightlySummaryEntity>,
    modifier: Modifier = Modifier
) {
    var selectedPoint by remember { mutableStateOf<NightlySummaryEntity?>(null) }
    val textMeasurer = rememberTextMeasurer()

    val primaryColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val textMutedColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .testTag("line_chart_canvas_container")
        ) {
            if (summaries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No history recorded yet. Your daily trends will appear here!",
                        style = MaterialTheme.typography.bodySmall,
                        color = textMutedColor
                    )
                }
            } else {
                // Sort summaries chronologically ascending
                val sortedList = remember(summaries) { summaries.sortedBy { it.timestamp } }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(sortedList) {
                            detectTapGestures { tapOffset ->
                                val width = size.width.toFloat()
                                val height = size.height.toFloat()

                                val leftPadding = 80f
                                val rightPadding = 40f
                                val topPadding = 40f
                                val bottomPadding = 60f

                                val chartWidth = width - leftPadding - rightPadding
                                val chartHeight = height - topPadding - bottomPadding

                                val count = sortedList.size
                                var closest: NightlySummaryEntity? = null
                                var minDistance = Float.MAX_VALUE

                                sortedList.forEachIndexed { index, item ->
                                    val x = if (count > 1) {
                                        leftPadding + (index.toFloat() / (count - 1)) * chartWidth
                                    } else {
                                        leftPadding + chartWidth / 2f
                                    }

                                    val pct = item.completionPercentage.coerceIn(0f, 100f)
                                    val y = topPadding + chartHeight * (1f - (pct / 100f))

                                    val dist = abs(tapOffset.x - x)
                                    if (dist < 40f && dist < minDistance) {
                                        minDistance = dist
                                        closest = item
                                    }
                                }
                                selectedPoint = closest
                            }
                        }
                ) {
                    val width = size.width
                    val height = size.height

                    val leftPadding = 80f
                    val rightPadding = 40f
                    val topPadding = 40f
                    val bottomPadding = 60f

                    val chartWidth = width - leftPadding - rightPadding
                    val chartHeight = height - topPadding - bottomPadding

                    // 1. Draw horizontal gridlines (0%, 25%, 50%, 75%, 100%)
                    val ySteps = listOf(100f, 75f, 50f, 25f, 0f)
                    ySteps.forEach { pct ->
                        val y = topPadding + chartHeight * (1f - (pct / 100f))
                        drawLine(
                            color = gridColor,
                            start = Offset(leftPadding, y),
                            end = Offset(width - rightPadding, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )

                        // Draw Y-Axis label
                        val labelText = "${pct.toInt()}%"
                        val textLayout = textMeasurer.measure(
                            text = labelText,
                            style = TextStyle(color = textMutedColor, fontSize = 10.sp)
                        )
                        drawText(
                            textLayoutResult = textLayout,
                            topLeft = Offset(10f, y - textLayout.size.height / 2f)
                        )
                    }

                    // 2. Draw points and line
                    val points = mutableListOf<Offset>()
                    val count = sortedList.size

                    sortedList.forEachIndexed { index, item ->
                        val x = if (count > 1) {
                            leftPadding + (index.toFloat() / (count - 1)) * chartWidth
                        } else {
                            leftPadding + chartWidth / 2f
                        }

                        val pct = item.completionPercentage.coerceIn(0f, 100f)
                        val y = topPadding + chartHeight * (1f - (pct / 100f))
                        points.add(Offset(x, y))

                        // X-axis date labels
                        if (index % maxOf(1, count / 7) == 0 || index == count - 1) {
                            val dateLabel = formatXAxisLabel(index, sortedList)
                            val textLayout = textMeasurer.measure(
                                text = dateLabel,
                                style = TextStyle(color = textMutedColor, fontSize = 9.sp)
                            )
                            drawText(
                                textLayoutResult = textLayout,
                                topLeft = Offset(
                                    x - textLayout.size.width / 2f,
                                    height - bottomPadding + 10f
                                )
                            )
                        }
                    }

                    if (points.isNotEmpty()) {
                        // Path for fill
                        val fillPath = Path().apply {
                            moveTo(points.first().x, topPadding + chartHeight)
                            points.forEach { lineTo(it.x, it.y) }
                            lineTo(points.last().x, topPadding + chartHeight)
                            close()
                        }

                        val strokePath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                lineTo(points[i].x, points[i].y)
                            }
                        }

                        // Draw gradient fill
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.35f),
                                    Color.Transparent
                                ),
                                startY = topPadding,
                                endY = topPadding + chartHeight
                            )
                        )

                        // Draw line
                        drawPath(
                            path = strokePath,
                            color = primaryColor,
                            style = Stroke(width = 3.dp.toPx())
                        )

                        // Draw point circles
                        points.forEachIndexed { idx, point ->
                            val isSelected = selectedPoint?.id == sortedList[idx].id
                            drawCircle(
                                color = if (isSelected) Color.White else primaryColor,
                                radius = if (isSelected) 7.dp.toPx() else 4.5.dp.toPx(),
                                center = point
                            )
                            drawCircle(
                                color = primaryColor,
                                radius = if (isSelected) 9.dp.toPx() else 6.dp.toPx(),
                                center = point,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }
                }
            }
        }

        // Tooltip or selected point detail card
        selectedPoint?.let { summary ->
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("chart_selected_point_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${String.format("%.0f", summary.completionPercentage)}%",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = summary.dateString,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = summary.summaryText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
