package com.scorecard.weekly.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.scorecard.weekly.data.AREAS
import com.scorecard.weekly.data.MAX_AREA_SCORE
import com.scorecard.weekly.data.MAX_TOTAL_SCORE
import com.scorecard.weekly.data.WeekEntry
import com.scorecard.weekly.data.weekNumber
import com.scorecard.weekly.data.weekRangeLabel
import java.time.LocalDate
import java.util.Locale

/** [entries] must be sorted newest first. */
@Composable
fun HistoryScreen(entries: List<WeekEntry>, onOpenWeek: (LocalDate) -> Unit) {
    if (entries.isEmpty()) {
        EmptyMessage("📅", "No history yet", "Your scored weeks will show up here.")
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader("History", overline = "${entries.size} " + if (entries.size == 1) "week scored" else "weeks scored")
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val average = entries.map { it.total }.average()
                StatTile("Average", String.format(Locale.getDefault(), "%.0f", average), Modifier.weight(1f))
                StatTile("Best", "${entries.maxOf { it.total }}", Modifier.weight(1f))
                StatTile("Latest", "${entries.first().total}", Modifier.weight(1f))
            }
        }
        itemsIndexed(entries, key = { _, e -> e.weekStart.toString() }) { index, entry ->
            val previous = entries.getOrNull(index + 1)
            WeekCard(entry, delta = previous?.let { entry.total - it.total }, onClick = { onOpenWeek(entry.weekStart) })
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier) {
    AppCard(modifier, contentPadding = PaddingValues(16.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Text(
                " /$MAX_TOTAL_SCORE",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 5.dp),
            )
        }
    }
}

@Composable
private fun WeekCard(entry: WeekEntry, delta: Int?, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Week ${weekNumber(entry.weekStart)}", style = MaterialTheme.typography.titleMedium)
                Text(
                    weekRangeLabel(entry.weekStart),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${entry.total}", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        " /$MAX_TOTAL_SCORE",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                if (delta != null) DeltaChip(delta)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AREAS.forEachIndexed { i, area ->
                val score = entry.areaScore(i)
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(area.color.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(score / MAX_AREA_SCORE.toFloat())
                                .clip(RoundedCornerShape(10.dp))
                                .background(area.color),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("${area.emoji} $score", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
