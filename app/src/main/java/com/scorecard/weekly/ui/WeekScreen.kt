package com.scorecard.weekly.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scorecard.weekly.R
import com.scorecard.weekly.data.AREAS
import com.scorecard.weekly.data.Area
import com.scorecard.weekly.data.GUT_FEEL_LABEL
import com.scorecard.weekly.data.MAX_AREA_SCORE
import com.scorecard.weekly.data.MAX_TOTAL_SCORE
import com.scorecard.weekly.data.WeekEntry
import com.scorecard.weekly.data.currentWeekStart
import com.scorecard.weekly.data.weekNumber
import com.scorecard.weekly.data.weekRangeLabel
import java.time.LocalDate

@Composable
fun WeekScreen(
    entry: WeekEntry,
    onChange: (WeekEntry) -> Unit,
    onWeekChange: (LocalDate) -> Unit,
) {
    val thisWeek = currentWeekStart()
    val isCurrentWeek = entry.weekStart == thisWeek
    // Which area card is open (-1 = none). Remembered per week.
    var expanded by rememberSaveable(entry.weekStart.toString()) { mutableIntStateOf(-1) }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                title = if (isCurrentWeek) "This week" else "Week ${weekNumber(entry.weekStart)}",
                overline = weekRangeLabel(entry.weekStart),
            ) {
                FilledTonalIconButton(onClick = { onWeekChange(entry.weekStart.minusWeeks(1)) }) {
                    Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = "Previous week")
                }
                Spacer(Modifier.width(8.dp))
                FilledTonalIconButton(
                    onClick = { onWeekChange(entry.weekStart.plusWeeks(1)) },
                    enabled = !isCurrentWeek,
                ) {
                    Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = "Next week")
                }
            }
            if (!isCurrentWeek) {
                TextButton(onClick = { onWeekChange(thisWeek) }, contentPadding = PaddingValues(0.dp)) {
                    Text("← Back to this week")
                }
            }
        }
        item { HeroCard(entry) }
        item {
            Text(
                "Areas",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp),
            )
        }
        AREAS.forEachIndexed { areaIndex, area ->
            item(key = areaIndex) {
                AreaCard(
                    area = area,
                    areaIndex = areaIndex,
                    entry = entry,
                    expanded = expanded == areaIndex,
                    onToggleExpanded = { expanded = if (expanded == areaIndex) -1 else areaIndex },
                    onChange = onChange,
                )
            }
        }
    }
}

@Composable
private fun HeroCard(entry: WeekEntry) {
    val white = Color.White
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(BrandGradient)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ScoreRing(
                progress = entry.total / MAX_TOTAL_SCORE.toFloat(),
                color = white,
                trackColor = white.copy(alpha = 0.22f),
                strokeWidth = 11.dp,
                modifier = Modifier.size(118.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${entry.total}", style = MaterialTheme.typography.displaySmall, color = white)
                    Text("of $MAX_TOTAL_SCORE", style = MaterialTheme.typography.labelMedium, color = white.copy(alpha = 0.8f))
                }
            }
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f)) {
                Text(verdict(entry.total, entry.isEmpty), style = MaterialTheme.typography.headlineSmall, color = white)
                Spacer(Modifier.height(6.dp))
                val focus = AREAS[entry.lowestArea]
                Text(
                    if (entry.isEmpty) {
                        "Open an area below and tick what was true this week."
                    } else {
                        "Focus next week:\n${focus.emoji} ${focus.title}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = white.copy(alpha = 0.88f),
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AREAS.forEachIndexed { i, area ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(area.emoji, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    ScoreBar(entry.areaScore(i) / MAX_AREA_SCORE.toFloat(), white, height = 5.dp)
                    Spacer(Modifier.height(4.dp))
                    Text("${entry.areaScore(i)}", style = MaterialTheme.typography.labelMedium, color = white)
                }
            }
        }
    }
}

@Composable
private fun AreaCard(
    area: Area,
    areaIndex: Int,
    entry: WeekEntry,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onChange: (WeekEntry) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val score = entry.areaScore(areaIndex)
    val ticked = entry.checked[areaIndex].size
    val gut = entry.gutFeel[areaIndex]
    val chevronRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")

    AppCard(contentPadding = PaddingValues(0.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpanded)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBubble(area.emoji, area.color)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(area.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    "$ticked of ${area.items.size} ticked" + if (gut) " · +1 bonus" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ScoreRing(
                progress = score / MAX_AREA_SCORE.toFloat(),
                color = area.color,
                trackColor = area.color.copy(alpha = 0.15f),
                strokeWidth = 5.dp,
                modifier = Modifier.size(46.dp),
            ) {
                Text("$score", style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.width(6.dp))
            Icon(
                painterResource(R.drawable.ic_expand_more),
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(chevronRotation),
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(Modifier.padding(start = 8.dp, end = 8.dp, bottom = 12.dp)) {
                area.items.forEachIndexed { itemIndex, text ->
                    val checked = entry.isChecked(areaIndex, itemIndex)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .toggleable(
                                value = checked,
                                role = Role.Checkbox,
                                onValueChange = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onChange(entry.toggleItem(areaIndex, itemIndex))
                                },
                            )
                            .padding(horizontal = 10.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CheckCircle(checked, area.color)
                        Spacer(Modifier.width(14.dp))
                        Text(text, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(area.color.copy(alpha = 0.09f))
                        .toggleable(
                            value = gut,
                            role = Role.Switch,
                            onValueChange = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onChange(entry.toggleGutFeel(areaIndex))
                            },
                        )
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("✨ $GUT_FEEL_LABEL", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text(
                            "Bonus point · +1",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = gut,
                        onCheckedChange = null,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = area.color,
                            checkedThumbColor = Color.White,
                            checkedBorderColor = area.color,
                        ),
                    )
                }
            }
        }
    }
}
