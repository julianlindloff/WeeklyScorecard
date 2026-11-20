package com.scorecard.weekly.ui

import android.Manifest
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.scorecard.weekly.data.ScorecardJson
import com.scorecard.weekly.data.ScorecardStore
import com.scorecard.weekly.data.WeekEntry
import com.scorecard.weekly.reminder.ReminderScheduler
import com.scorecard.weekly.reminder.ReminderSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun SettingsScreen(store: ScorecardStore) {
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader("Settings", overline = "Reminder, AI & data")
        SectionTitle("Reminder")
        ReminderSection()
        SectionTitle("AI coach")
        AiKeySection()
        SectionTitle("Your data")
        DataSection(store)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp, start = 4.dp))
}

// ---------------------------------------------------------------- Reminder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderSection() {
    val context = LocalContext.current
    var settings by remember { mutableStateOf(ReminderSettings.load(context)) }
    var permissionDenied by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    fun update(new: ReminderSettings) {
        settings = new
        ReminderSettings.save(context, new)
        ReminderScheduler.schedule(context)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionDenied = !granted
        if (granted) update(settings.copy(enabled = true))
    }

    val locale = Locale.getDefault()
    val timeText = LocalTime.of(settings.hour, settings.minute)
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    val dayName = settings.day.getDisplayName(TextStyle.FULL, locale)

    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBubble("🔔", MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Weekly reminder", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (settings.enabled) "Every $dayName at $timeText" else "Off",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = settings.enabled,
                onCheckedChange = { on ->
                    if (on && !ReminderScheduler.hasNotificationPermission(context) &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    ) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        update(settings.copy(enabled = on))
                    }
                },
            )
        }
        if (permissionDenied) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Notifications are blocked. Allow them in Android Settings → Apps → Weekly Scorecard → Notifications, then try again.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }

    AppCard(Modifier.alpha(if (settings.enabled) 1f else 0.55f)) {
        Text("Day", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DayOfWeek.entries.forEach { day ->
                val selected = settings.day == day
                val bg by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    label = "day",
                )
                Box(
                    Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(bg)
                        .clickable { update(settings.copy(day = day)) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        day.getDisplayName(TextStyle.SHORT, locale).take(2),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Time", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                .clickable { showTimePicker = true }
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(timeText, style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
                Text("Tap to change", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (settings.enabled) {
            Spacer(Modifier.height(16.dp))
            Text(
                "Next: ${settings.nextTrigger().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale))} · $timeText",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showTimePicker) {
        val state = rememberTimePickerState(
            initialHour = settings.hour,
            initialMinute = settings.minute,
            is24Hour = DateFormat.is24HourFormat(context),
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    update(settings.copy(hour = state.hour, minute = state.minute))
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
            text = { TimePicker(state = state) },
        )
    }
}

// ---------------------------------------------------------------- Export / import

private fun weeksText(n: Int) = if (n == 1) "1 week" else "$n weeks"

@Composable
private fun DataSection(store: ScorecardStore) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<List<WeekEntry>?>(null) }

    fun show(text: String, error: Boolean = false) {
        message = text
        isError = error
    }

    // Opens Android's "Save as" screen; the user picks where (Downloads, Google Drive, OneDrive, ...).
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = store.exportText()
        val count = store.weeks.size
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val out = context.contentResolver.openOutputStream(uri, "w")
                        ?: throw IllegalStateException("Couldn't create the file.")
                    out.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                }
                show("Exported ${weeksText(count)}.")
            } catch (e: Exception) {
                show("Export failed: ${e.message ?: "unknown error"}", error = true)
            }
        }
    }

    // Opens Android's file picker.
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    val input = context.contentResolver.openInputStream(uri)
                        ?: throw IllegalStateException("Couldn't open the file.")
                    input.use { it.readBytes().toString(Charsets.UTF_8) }
                }
                val entries = ScorecardJson.parseImport(text)
                if (entries.isEmpty()) {
                    show("This backup doesn't contain any scored weeks.", error = true)
                } else {
                    message = null
                    pendingImport = entries
                }
            } catch (e: IllegalArgumentException) {
                show(e.message ?: "This file can't be imported.", error = true)
            } catch (e: Exception) {
                show("Import failed: ${e.message ?: "unknown error"}", error = true)
            }
        }
    }

    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBubble("💾", MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Backup", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${weeksText(store.weeks.size)} saved on this phone",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalButton(
                onClick = { exportLauncher.launch("weekly-scorecard-${LocalDate.now()}.json") },
                enabled = store.weeks.isNotEmpty(),
                modifier = Modifier.weight(1f),
            ) { Text("Export") }
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/json", "text/*", "application/octet-stream")) },
                modifier = Modifier.weight(1f),
            ) { Text("Import") }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Export saves all your weeks to a file — keep it in OneDrive, Google Drive or email it to yourself. " +
                "Import loads such a file, for example on a new phone.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        message?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
        }
    }

    pendingImport?.let { entries ->
        val overlap = entries.count { it.weekStart in store.weeks }
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("Import ${weeksText(entries.size)}?") },
            text = {
                Text(
                    buildString {
                        append("Merge: adds the weeks from the file and keeps your other weeks.")
                        if (overlap > 0) {
                            append(" ${weeksText(overlap)} exist in both — the file's version will be used.")
                        }
                        append("\n\nReplace all: deletes everything on this phone first, then loads the file.")
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    store.import(entries, replaceAll = false)
                    pendingImport = null
                    show("Imported ${weeksText(entries.size)}.")
                }) { Text("Merge") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { pendingImport = null }) { Text("Cancel") }
                    TextButton(
                        onClick = {
                            store.import(entries, replaceAll = true)
                            pendingImport = null
                            show("Replaced all data with ${weeksText(entries.size)} from the file.")
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text("Replace all") }
                }
            },
        )
    }
}
