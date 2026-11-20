package com.scorecard.weekly

import com.scorecard.weekly.ai.CoachPrompt
import com.scorecard.weekly.data.WeekEntry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CoachPromptTest {
    private val w1 = WeekEntry(LocalDate.of(2026, 11, 9)).toggleItem(0, 0).toggleItem(0, 2).toggleGutFeel(0)
    private val w2 = WeekEntry(LocalDate.of(2026, 11, 16)).toggleItem(4, 8)

    @Test
    fun weeksAreListedOldestFirstWithScores() {
        val text = CoachPrompt.weeksText(listOf(w2, w1))
        assertTrue(text.indexOf("2026-11-09") < text.indexOf("2026-11-16"))
        assertTrue(text.contains("total 3/50"))
        assertTrue(text.contains("Health & Energy: 3/10 (ticked items: 1,3; gut-feel: yes)"))
        assertTrue(text.contains("Mental Well-being: 1/10 (ticked items: 9; gut-feel: no)"))
    }

    @Test
    fun emptyWeeksAreLeftOut() {
        val text = CoachPrompt.weeksText(listOf(w1, WeekEntry(LocalDate.of(2026, 11, 2))))
        assertFalse(text.contains("2026-11-02"))
        assertTrue(text.contains("count=\"1\""))
    }

    @Test
    fun systemPromptHasChecklistAndToday() {
        val prompt = CoachPrompt.systemPrompt(listOf(w1), LocalDate.of(2026, 11, 20))
        assertTrue(prompt.contains("1. I got 7–8 hours of sleep most nights."))
        assertTrue(prompt.contains("Today is 2026-11-20"))
    }
}
