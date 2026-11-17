package com.scorecard.weekly.data

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Converts weeks to and from JSON.
 *
 * - Internal file (weeks.json):  { "2026-11-16": { "checked": [[0,3],[...]], "gutFeel": [true,...] }, ... }
 * - Backup/export file:          { "format": "weekly-scorecard", "version": 1, "exportedAt": "...",
 *                                  "areas": ["Health & Energy", ...], "weeks": { <same as internal> } }
 */
object ScorecardJson {
    const val FORMAT = "weekly-scorecard"
    const val VERSION = 1

    private const val NOT_A_BACKUP = "This file isn't a Weekly Scorecard backup."

    fun encodeWeeks(entries: Collection<WeekEntry>): JSONObject {
        val root = JSONObject()
        for (entry in entries.sortedBy { it.weekStart }) {
            root.put(
                entry.weekStart.toString(),
                JSONObject()
                    .put("checked", JSONArray(entry.checked.map { JSONArray(it.sorted()) }))
                    .put("gutFeel", JSONArray(entry.gutFeel)),
            )
        }
        return root
    }

    fun decodeWeeks(root: JSONObject): List<WeekEntry> =
        root.keys().asSequence().map { key -> decodeEntry(key, root.getJSONObject(key)) }.toList()

    private fun decodeEntry(key: String, obj: JSONObject): WeekEntry {
        val checkedJson = obj.optJSONArray("checked") ?: JSONArray()
        val gutJson = obj.optJSONArray("gutFeel") ?: JSONArray()
        val checked = AREAS.mapIndexed { a, area ->
            val arr = checkedJson.optJSONArray(a) ?: JSONArray()
            // Ignore item numbers that don't exist in the checklist.
            (0 until arr.length()).map { arr.getInt(it) }.filter { it in area.items.indices }.toSet()
        }
        val gutFeel = AREAS.indices.map { a -> gutJson.optBoolean(a, false) }
        return WeekEntry(LocalDate.parse(key).startOfWeek(), checked, gutFeel)
    }

    /** Pretty-printed backup file content. */
    fun exportText(entries: Collection<WeekEntry>, exportedAt: String): String =
        JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("exportedAt", exportedAt)
            .put("areas", JSONArray(AREAS.map { it.title }))
            .put("weeks", encodeWeeks(entries))
            .toString(2)

    /**
     * Reads a backup file (or a raw weeks.json). Returns only weeks that have scores.
     * Throws [IllegalArgumentException] with a message suitable for the user if the file is not valid.
     */
    fun parseImport(text: String): List<WeekEntry> {
        val root = try {
            JSONObject(text)
        } catch (e: JSONException) {
            throw IllegalArgumentException(NOT_A_BACKUP)
        }
        val weeks = if (root.has("weeks")) {
            if (root.optString("format") != FORMAT) throw IllegalArgumentException(NOT_A_BACKUP)
            if (root.optInt("version", 1) > VERSION) {
                throw IllegalArgumentException("This backup was made by a newer version of the app. Please update the app first.")
            }
            root.optJSONObject("weeks") ?: throw IllegalArgumentException(NOT_A_BACKUP)
        } else {
            root
        }
        return try {
            decodeWeeks(weeks).filterNot { it.isEmpty }
        } catch (e: JSONException) {
            throw IllegalArgumentException("The backup file is damaged and can't be read.")
        } catch (e: DateTimeParseException) {
            throw IllegalArgumentException("The backup file is damaged and can't be read.")
        }
    }
}
