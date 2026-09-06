package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

// African Contemporary Pattern Decorative Header Strip
@Composable
fun AfricanPatternBar(modifier: Modifier = Modifier, height: Dp = 6.dp) {
    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        val segmentWidth = 24.dp.toPx()
        val count = (size.width / segmentWidth).toInt() + 1
        for (i in 0 until count) {
            val startX = i * segmentWidth
            val color = if (i % 2 == 0) VertEmeraude else OrangeTerreCuite
            drawRect(
                color = color,
                topLeft = Offset(startX, 0f),
                size = Size(segmentWidth * 0.7f, size.height)
            )
            drawRect(
                color = BeigeSable,
                topLeft = Offset(startX + segmentWidth * 0.7f, 0f),
                size = Size(segmentWidth * 0.3f, size.height)
            )
        }
    }
}

// Circular Academic Progress Indicator with Animation
@Composable
fun AcademicCircularProgress(
    progressPercent: Int,
    sizeDp: Dp = 140.dp,
    strokeWidth: Dp = 12.dp,
    label: String = "Progression Annuelle",
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progressPercent.coerceIn(0, 100) / 100f,
        animationSpec = tween(durationMillis = 1000),
        label = "circularProgress"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(sizeDp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val diameter = size.minDimension - strokePx
            val topLeft = Offset(strokePx / 2, strokePx / 2)
            val arcSize = Size(diameter, diameter)

            // Track background
            drawArc(
                color = VertEmeraude.copy(alpha = 0.15f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Progress arc
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        VertEmeraude,
                        OrangeTerreCuite,
                        VertEmeraude
                    )
                ),
                startAngle = -90f,
                sweepAngle = animatedProgress * 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$progressPercent%",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = VertEmeraude
                )
            )
            if (label.isNotBlank()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }
}

// Module Status Pill
@Composable
fun ModuleStatusBadge(progression: Int, targetEndDate: Long = 0L) {
    val now = System.currentTimeMillis()
    val isOverdue = targetEndDate > 0L && targetEndDate < now && progression < 100

    val (bg, textColor, text) = when {
        isOverdue -> Triple(StatutEnRetard.copy(alpha = 0.15f), StatutEnRetard, "En retard")
        progression >= 100 -> Triple(StatutTermine.copy(alpha = 0.15f), StatutTermine, "Terminé")
        progression > 0 -> Triple(VertEmeraude.copy(alpha = 0.15f), VertEmeraude, "En cours ($progression%)")
        else -> Triple(StatutNonCommence.copy(alpha = 0.15f), StatutNonCommence, "Non commencé")
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = textColor
            ),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

// 7-day Study Hours Bar Chart
@Composable
fun WeeklyStudyBarChart(
    stats: List<com.example.ui.DayStudyStat>,
    modifier: Modifier = Modifier,
    heightDp: Dp = 140.dp
) {
    val maxMinutes = (stats.maxOfOrNull { it.minutes } ?: 60).coerceAtLeast(60)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(heightDp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            stats.forEach { dayStat ->
                val fraction = (dayStat.minutes.toFloat() / maxMinutes.toFloat()).coerceIn(0.04f, 1f)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f)
                ) {
                    // Minutes label on top of bar
                    if (dayStat.minutes > 0) {
                        Text(
                            text = "${dayStat.minutes}m",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (dayStat.isToday) OrangeTerreCuite else VertEmeraude
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    // Bar
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .fillMaxHeight(fraction)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(
                                if (dayStat.isToday) {
                                    Brush.verticalGradient(listOf(OrangeTerreCuite, OrangeContainerLight))
                                } else if (dayStat.minutes > 0) {
                                    Brush.verticalGradient(listOf(VertEmeraude, VertEmeraudeContainerLight))
                                } else {
                                    Brush.verticalGradient(listOf(GrisNeutre.copy(alpha = 0.2f), GrisNeutre.copy(alpha = 0.1f)))
                                }
                            )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Day label
                    Text(
                        text = dayStat.dayLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (dayStat.isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (dayStat.isToday) OrangeTerreCuite else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}
