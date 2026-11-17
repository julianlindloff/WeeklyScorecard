package com.scorecard.weekly

import com.scorecard.weekly.data.AREAS
import com.scorecard.weekly.data.WeekEntry
import com.scorecard.weekly.data.startOfWeek
import com.scorecard.weekly.reminder.ReminderSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class ScoringTest {
    private val monday = LocalDate.of(2026, 11, 16)

    @Test
    fun checklistHasFiveAreasOfNineItems() {
        assertEquals(5, AREAS.size)
        AREAS.forEach { assertEquals(it.title, 9, it.items.size) }
    }

    @Test
    fun fullAreaScoresTen() {
        var e = WeekEntry(monday)
        repeat(9) { e = e.toggleItem(0, it) }
        e = e.toggleGutFeel(0)
        assertEquals(10, e.areaScore(0))
        assertEquals(10, e.total)
    }

    @Test
    fun togglingTwiceUnticks() {
        val e = WeekEntry(monday).toggleItem(2, 3).toggleItem(2, 3)
        assertTrue(e.isEmpty)
    }

    @Test
    fun lowestAreaIsDetected() {
        var e = WeekEntry(monday)
        for (a in AREAS.indices) if (a != 3) e = e.toggleItem(a, 0)
        assertEquals(3, e.lowestArea)
    }

    @Test
    fun weekStartsOnMonday() {
        assertEquals(monday, LocalDate.of(2026, 11, 22).startOfWeek()) // Sunday
        assertEquals(monday, monday.startOfWeek())
    }

    @Test
    fun nextReminderIsLaterThisWeekOrNextWeek() {
        val zone = ZoneId.of("Europe/Berlin")
        val s = ReminderSettings(enabled = true, day = DayOfWeek.SUNDAY, hour = 19, minute = 0)
        val tuesday = ZonedDateTime.of(2026, 11, 17, 10, 0, 0, 0, zone)
        assertEquals(ZonedDateTime.of(2026, 11, 22, 19, 0, 0, 0, zone), s.nextTrigger(tuesday))
        val sundayLate = ZonedDateTime.of(2026, 11, 22, 19, 0, 0, 0, zone)
        assertEquals(ZonedDateTime.of(2026, 11, 29, 19, 0, 0, 0, zone), s.nextTrigger(sundayLate))
    }
}
