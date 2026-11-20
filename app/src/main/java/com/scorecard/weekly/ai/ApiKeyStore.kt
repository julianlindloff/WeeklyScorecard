package com.scorecard.weekly.ai

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit

/**
 * Keeps the user's Claude API key in the app's private storage on the phone.
 * The file is excluded from Android's cloud backup (see backup_rules.xml and data_extraction_rules.xml).
 */
class ApiKeyStore private constructor(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var key: String? by mutableStateOf(prefs.getString(KEY, null))
        private set

    val hasKey: Boolean get() = !key.isNullOrBlank()

    fun save(newKey: String) {
        val cleaned = newKey.trim()
        prefs.edit { putString(KEY, cleaned) }
        key = cleaned
    }

    fun clear() {
        prefs.edit { remove(KEY) }
        key = null
    }

    companion object {
        const val PREFS_NAME = "ai"
        private const val KEY = "anthropic_api_key"

        @Volatile private var instance: ApiKeyStore? = null

        fun get(context: Context): ApiKeyStore =
            instance ?: synchronized(this) {
                instance ?: ApiKeyStore(context.applicationContext).also { instance = it }
            }

        /** Shows only the end of the key, e.g. "sk-ant-…3fA9". */
        fun masked(key: String): String =
            if (key.length <= 10) "••••" else key.take(7) + "…" + key.takeLast(4)
    }
}
