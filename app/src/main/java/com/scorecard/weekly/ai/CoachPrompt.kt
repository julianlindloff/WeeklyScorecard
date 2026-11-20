package com.scorecard.weekly.ai

import com.scorecard.weekly.data.AREAS
import com.scorecard.weekly.data.GUT_FEEL_LABEL
import com.scorecard.weekly.data.MAX_AREA_SCORE
import com.scorecard.weekly.data.MAX_TOTAL_SCORE
import com.scorecard.weekly.data.WeekEntry
import com.scorecard.weekly.data.weekNumber
import java.time.LocalDate

/**
 * Builds the instructions + scorecard data that are sent to Claude with every chat message.
 * Pure Kotlin (no Android), so it can be unit-tested on the PC.
 */
object CoachPrompt {

    private val INSTRUCTIONS = """
        You are the friendly, practical coach inside "Weekly Scorecard", a personal self-care app.
        Each week the user ticks off what they did in five areas. Every area has 9 checklist items
        worth 1 point each, plus a 1-point gut-feel bonus ("$GUT_FEEL_LABEL"), so an area scores
        0-$MAX_AREA_SCORE and a week scores 0-$MAX_TOTAL_SCORE.

        You can see all of the user's recorded weeks below. Use them to answer questions, spot
        patterns and trends, compare weeks and areas, and suggest small, concrete next steps.
        Base every claim about the user's weeks on the data; if the data doesn't show something,
        say so instead of guessing. Weeks that are missing were simply not recorded.

        Keep answers short and warm, written for a phone screen: a few sentences or a short list.
        Use plain text. You may use "- " for list items and **bold** for a few key words, but no
        headings, tables or code blocks. You are not a doctor or therapist; if the user mentions
        a serious health or mental-health concern, encourage them kindly to talk to a professional.
    """.trimIndent()

    /** The full system prompt: instructions, the checklist, and every recorded week (oldest first). */
    fun systemPrompt(weeks: Collection<WeekEntry>, today: LocalDate): String = buildString {
        appendLine(INSTRUCTIONS)
        appendLine()
        appendLine("<checklist>")
        AREAS.forEach { area ->
            appendLine("${area.title}:")
            area.items.forEachIndexed { i, item -> appendLine("  ${i + 1}. $item") }
        }
        appendLine("</checklist>")
        appendLine()
        appendLine("Today is $today (week ${weekNumber(today)}).")
        appendLine()
        append(weeksText(weeks))
    }

    /**
     * One block per week. Ticked items are given as item numbers from the checklist above,
     * which keeps the text compact even after years of weeks.
     */
    fun weeksText(weeks: Collection<WeekEntry>): String = buildString {
        val recorded = weeks.filterNot { it.isEmpty }.sortedBy { it.weekStart }
        appendLine("<weeks count=\"${recorded.size}\">")
        if (recorded.isEmpty()) appendLine("No weeks recorded yet.")
        recorded.forEach { week ->
            appendLine(
                "Week ${weekNumber(week.weekStart)}, starting Monday ${week.weekStart}: " +
                    "total ${week.total}/$MAX_TOTAL_SCORE",
            )
            AREAS.forEachIndexed { a, area ->
                val ticked = week.checked[a].sorted().map { it + 1 }
                val items = if (ticked.isEmpty()) "none" else ticked.joinToString(",")
                val gut = if (week.gutFeel[a]) "yes" else "no"
                appendLine(
                    "  ${area.title}: ${week.areaScore(a)}/$MAX_AREA_SCORE " +
                        "(ticked items: $items; gut-feel: $gut)",
                )
            }
        }
        appendLine("</weeks>")
    }
}
