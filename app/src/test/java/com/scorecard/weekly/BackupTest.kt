package com.scorecard.weekly

import com.scorecard.weekly.data.ScorecardJson
import com.scorecard.weekly.data.WeekEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.LocalDate

class BackupTest {
    private val w1 = WeekEntry(LocalDate.of(2026, 11, 9)).toggleItem(0, 2).toggleItem(4, 8).toggleGutFeel(1)
    private val w2 = WeekEntry(LocalDate.of(2026, 11, 16)).toggleItem(3, 0)

    private fun assertRejected(text: String) {
        try {
            ScorecardJson.parseImport(text)
            fail("Expected the file to be rejected")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.isNotBlank())
        }
    }

    @Test
    fun exportThenImportGivesSameWeeks() {
        val text = ScorecardJson.exportText(listOf(w1, w2), "2026-11-17T10:00+01:00")
        val imported = ScorecardJson.parseImport(text).sortedBy { it.weekStart }
        assertEquals(listOf(w1, w2), imported)
    }

    @Test
    fun rawInternalFileCanBeImported() {
        val raw = ScorecardJson.encodeWeeks(listOf(w1)).toString()
        assertEquals(listOf(w1), ScorecardJson.parseImport(raw))
    }

    @Test
    fun emptyWeeksAreSkipped() {
        val text = ScorecardJson.exportText(listOf(w1, WeekEntry(LocalDate.of(2026, 11, 2))), "x")
        assertEquals(listOf(w1), ScorecardJson.parseImport(text))
    }

    @Test
    fun unknownItemNumbersAreIgnored() {
        val text = """{"format":"weekly-scorecard","version":1,"weeks":
            {"2026-11-16":{"checked":[[0,1,99],[],[],[],[]],"gutFeel":[false,false,false,false,false]}}}"""
        val entry = ScorecardJson.parseImport(text).single()
        assertEquals(setOf(0, 1), entry.checked[0])
    }

    @Test
    fun invalidFilesAreRejected() {
        assertRejected("hello")
        assertRejected("""{"format":"something-else","weeks":{}}""")
        assertRejected("""{"format":"weekly-scorecard","version":99,"weeks":{}}""")
        assertRejected("""{"format":"weekly-scorecard","version":1,"weeks":{"not-a-date":{}}}""")
    }
}
