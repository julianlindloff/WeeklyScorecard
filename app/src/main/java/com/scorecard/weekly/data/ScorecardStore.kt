package com.scorecard.weekly.data

import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * Keeps all weeks in memory and saves them to a small JSON file on the phone
 * (weeks.json in the app's private storage). Nothing leaves the device unless you export it.
 */
class ScorecardStore private constructor(private val file: File) {

    private val _weeks = mutableStateMapOf<LocalDate, WeekEntry>()
    val weeks: Map<LocalDate, WeekEntry> get() = _weeks

    init {
        load()
    }

    fun entryFor(weekStart: LocalDate): WeekEntry = _weeks[weekStart] ?: WeekEntry(weekStart)

    fun save(entry: WeekEntry) {
        if (entry.isEmpty) _weeks.remove(entry.weekStart) else _weeks[entry.weekStart] = entry
        persist()
    }

    /** Contents of a backup file with all weeks. */
    fun exportText(): String = ScorecardJson.exportText(_weeks.values, ZonedDateTime.now().toString())

    /**
     * Adds imported weeks. Weeks that exist on both sides take the imported version.
     * With [replaceAll], everything on the phone is removed first.
     */
    fun import(entries: List<WeekEntry>, replaceAll: Boolean) {
        if (replaceAll) _weeks.clear()
        entries.filterNot { it.isEmpty }.forEach { _weeks[it.weekStart] = it }
        persist()
    }

    private fun load() {
        if (!file.exists()) return
        runCatching {
            ScorecardJson.decodeWeeks(JSONObject(file.readText())).forEach { _weeks[it.weekStart] = it }
        }
    }

    private fun persist() {
        val text = ScorecardJson.encodeWeeks(_weeks.values).toString()
        // Write to a temporary file first so a crash mid-write can't corrupt the data.
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(file)) {
            file.writeText(text)
            tmp.delete()
        }
    }

    companion object {
        @Volatile
        private var instance: ScorecardStore? = null

        fun get(context: Context): ScorecardStore =
            instance ?: synchronized(this) {
                instance ?: ScorecardStore(File(context.applicationContext.filesDir, "weeks.json"))
                    .also { instance = it }
            }
    }
}
