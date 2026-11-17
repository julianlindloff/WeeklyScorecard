package com.scorecard.weekly.reminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.scorecard.weekly.MainActivity
import com.scorecard.weekly.R
import java.time.DayOfWeek
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

/** The user's reminder choice, saved in the phone's app settings. */
data class ReminderSettings(
    val enabled: Boolean = false,
    val day: DayOfWeek = DayOfWeek.SUNDAY,
    val hour: Int = 19,
    val minute: Int = 0,
) {
    /** The next moment the reminder should fire, strictly after [now]. */
    fun nextTrigger(now: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime {
        var t = now.with(TemporalAdjusters.nextOrSame(day))
            .withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!t.isAfter(now)) t = t.plusWeeks(1)
        return t
    }

    companion object {
        private const val PREFS = "reminder"

        fun load(context: Context): ReminderSettings {
            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            return ReminderSettings(
                enabled = p.getBoolean("enabled", false),
                day = DayOfWeek.of(p.getInt("day", DayOfWeek.SUNDAY.value)),
                hour = p.getInt("hour", 19),
                minute = p.getInt("minute", 0),
            )
        }

        fun save(context: Context, s: ReminderSettings) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean("enabled", s.enabled)
                .putInt("day", s.day.value)
                .putInt("hour", s.hour)
                .putInt("minute", s.minute)
                .apply()
        }
    }
}

object ReminderScheduler {
    private const val CHANNEL_ID = "weekly_reminder"
    private const val NOTIFICATION_ID = 1

    fun hasNotificationPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, ReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** (Re)plans the next reminder, or cancels it if the reminder is switched off. */
    fun schedule(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pending = alarmIntent(context)
        alarmManager.cancel(pending)
        val settings = ReminderSettings.load(context)
        if (!settings.enabled) return
        // Allowed without special permission; Android may deliver it a few minutes late to save battery.
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            settings.nextTrigger().toInstant().toEpochMilli(),
            pending,
        )
    }

    @SuppressLint("MissingPermission")
    fun showNotification(context: Context) {
        if (!hasNotificationPermission(context)) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Weekly reminder", NotificationManager.IMPORTANCE_DEFAULT),
        )
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time to score your week")
            .setContentText("Take 5 minutes to fill in your weekly scorecard.")
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}

/** Fires when the reminder time arrives: shows the notification and plans next week's. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReminderScheduler.showNotification(context)
        ReminderScheduler.schedule(context)
    }
}

/** Android forgets planned alarms on restart or clock changes, so re-plan them then. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED -> ReminderScheduler.schedule(context)
        }
    }
}
