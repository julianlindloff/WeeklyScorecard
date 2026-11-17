package com.scorecard.weekly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.scorecard.weekly.data.ScorecardStore
import com.scorecard.weekly.reminder.ReminderScheduler
import com.scorecard.weekly.ui.ScorecardApp
import com.scorecard.weekly.ui.theme.WeeklyScorecardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Make sure the weekly reminder is planned (e.g. after an app update).
        ReminderScheduler.schedule(this)
        val store = ScorecardStore.get(this)
        setContent {
            WeeklyScorecardTheme {
                ScorecardApp(store)
            }
        }
    }
}
