package com.scorecard.weekly.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.scorecard.weekly.data.AREAS
import com.scorecard.weekly.data.MAX_AREA_SCORE
import com.scorecard.weekly.data.MAX_TOTAL_SCORE
import com.scorecard.weekly.data.WeekEntry
import com.scorecard.weekly.data.weekNumber
import java.util.Locale

private const val WEEKS_SHOWN = 12

/** [entries] must be sorted oldest first. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrendsScreen(entries: List<WeekEntry>) {
    if (entries.size < 2) {
        EmptyMessage("📈", "Trends need two weeks", "Score at least two weeks and your progress will appear here.")
        return
    }
    val shown = entries.takeLast(WEEKS_SHOWN)
    val labels = shown.map { "W${weekNumber(it.weekStart)}" }
    var visible by remember { mutableStateOf(AREAS.indices.toSet()) }
    val latest = shown.last()
    val previous = shown[shown.size - 2]

    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader("Trends", overline = "Last ${shown.size} scored weeks")

        // Total score
        AppCard {
            Text("Total score", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${latest.total}", style = MaterialTheme.typography.displaySmall)
                Text(
                    " / $MAX_TOTAL_SCORE",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                DeltaChip(latest.total - previous.total)
            }
            Spacer(Modifier.height(12.dp))
            LineChart(
                series = listOf(ChartSeries(MaterialTheme.colorScheme.primary, shown.map { it.total })),
                maxValue = MAX_TOTAL_SCORE,
                gridStep = 10,
                xLabels = labels,
                fill = true,
                modifier = Modifier.fillMaxWidth().height(180.dp),
            )
        }

        // Per area
        AppCard {
            Text("By area", style = MaterialTheme.typography.titleMedium)
            Text(
                "Tap an area to show or hide it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AREAS.forEachIndexed { i, area ->
                    val selected = i in visible
                    Row(
                        Modifier
                            .clip(CircleShape)
                            .background(
                                if (selected) area.color.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
                            )
                            .clickable { visible = if (selected) visible - i else visible + i }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(area.emoji)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            area.shortName,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            LineChart(
                series = AREAS.indices.filter { it in visible }.map { a ->
                    ChartSeries(AREAS[a].color, shown.map { it.areaScore(a) })
                },
                maxValue = MAX_AREA_SCORE,
                gridStep = 2,
                xLabels = labels,
                modifier = Modifier.fillMaxWidth().height(220.dp),
            )
        }

        // Averages
        AppCard {
            Text("Averages", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            val averages = AREAS.indices
                .map { i -> i to shown.map { it.areaScore(i) }.average() }
                .sortedBy { it.second }
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                averages.forEachIndexed { rank, (i, avg) ->
                    val area = AREAS[i]
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBubble(area.emoji, area.color, size = 38.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(area.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f, fill = false))
                                if (rank == 0) {
                                    Spacer(Modifier.width(8.dp))
                                    Box(
                                        Modifier
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                            .padding(horizontal = 8.dp, vertical = 2.dp),
                                    ) {
                                        Text(
                                            "Focus",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                    }
                                }
                                Spacer(Modifier.weight(1f))
                                Text(String.format(Locale.getDefault(), "%.1f", avg), style = MaterialTheme.typography.titleSmall)
                            }
                            Spacer(Modifier.height(6.dp))
                            ScoreBar((avg / MAX_AREA_SCORE).toFloat(), area.color, height = 6.dp)
                        }
                    }
                }
            }
        }
    }
}
