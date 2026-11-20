package com.scorecard.weekly.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.scorecard.weekly.R
import com.scorecard.weekly.data.ScorecardStore
import com.scorecard.weekly.data.currentWeekStart
import java.time.LocalDate

enum class Tab(val label: String, @DrawableRes val icon: Int) {
    Week("Week", R.drawable.ic_tab_week),
    History("History", R.drawable.ic_tab_history),
    Trends("Trends", R.drawable.ic_tab_trends),
    Coach("Coach", R.drawable.ic_tab_coach),
    Settings("Settings", R.drawable.ic_tab_settings),
}

@Composable
fun ScorecardApp(store: ScorecardStore) {
    var tab by rememberSaveable { mutableStateOf(Tab.Week) }
    var weekStartText by rememberSaveable { mutableStateOf(currentWeekStart().toString()) }
    val weekStart = LocalDate.parse(weekStartText)

    // The phone's back button returns to the Week tab instead of closing the app.
    BackHandler(enabled = tab != Tab.Week) { tab = Tab.Week }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(painterResource(t.icon), contentDescription = null) },
                        label = { Text(t.label, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        ),
                    )
                }
            }
        },
    ) { padding ->
        AnimatedContent(
            targetState = tab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.padding(padding),
            label = "tab",
        ) { current ->
            Box(Modifier.fillMaxSize()) {
                when (current) {
                    Tab.Week -> WeekScreen(
                        entry = store.entryFor(weekStart),
                        onChange = store::save,
                        onWeekChange = { weekStartText = it.toString() },
                    )
                    Tab.History -> HistoryScreen(
                        entries = store.weeks.values.sortedByDescending { it.weekStart },
                        onOpenWeek = {
                            weekStartText = it.toString()
                            tab = Tab.Week
                        },
                    )
                    Tab.Trends -> TrendsScreen(entries = store.weeks.values.sortedBy { it.weekStart })
                    Tab.Coach -> CoachScreen(store, outerPadding = padding, onOpenSettings = { tab = Tab.Settings })
                    Tab.Settings -> SettingsScreen(store)
                }
            }
        }
    }
}
