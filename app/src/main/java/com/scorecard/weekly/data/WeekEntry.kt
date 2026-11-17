package com.scorecard.weekly.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters

/**
 * One week's answers. A week is identified by its Monday.
 * checked[area] = the numbers of the ticked items in that area.
 * gutFeel[area] = whether the "+1 overall" point was given.
 */
data class WeekEntry(
    val weekStart: LocalDate,
    val checked: List<Set<Int>> = List(AREAS.size) { emptySet() },
    val gutFeel: List<Boolean> = List(AREAS.size) { false },
) {
    fun areaScore(area: Int): Int = checked[area].size + if (gutFeel[area]) 1 else 0

    val total: Int get() = AREAS.indices.sumOf { areaScore(it) }

    val isEmpty: Boolean get() = checked.all { it.isEmpty() } && gutFeel.none { it }

    /** Index of the area with the lowest score (first one if tied). */
    val lowestArea: Int get() = AREAS.indices.minBy { areaScore(it) }

    fun isChecked(area: Int, item: Int): Boolean = item in checked[area]

    fun toggleItem(area: Int, item: Int): WeekEntry {
        val current = checked[area]
        val updated = if (item in current) current - item else current + item
        return copy(checked = checked.toMutableList().also { it[area] = updated })
    }

    fun toggleGutFeel(area: Int): WeekEntry =
        copy(gutFeel = gutFeel.toMutableList().also { it[area] = !it[area] })
}

fun LocalDate.startOfWeek(): LocalDate = with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

fun currentWeekStart(): LocalDate = LocalDate.now().startOfWeek()

fun weekNumber(weekStart: LocalDate): Int = weekStart.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)

private val dayMonth = DateTimeFormatter.ofPattern("d MMM")
private val dayMonthYear = DateTimeFormatter.ofPattern("d MMM yyyy")

/** e.g. "16 Nov – 22 Nov 2026" */
fun weekRangeLabel(weekStart: LocalDate): String =
    "${weekStart.format(dayMonth)} – ${weekStart.plusDays(6).format(dayMonthYear)}"
