package com.adityakulkarni.haircare

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object Reminders {
    fun scheduleNext(c: Context) {
        val at = nextSlot(LocalDateTime.now()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val pi = PendingIntent.getBroadcast(
            c, 0, Intent(c, ReminderReceiver::class.java).setAction(ACTION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        // Exact so the 23:00 "last chance" can't slide past midnight (inexact windows are up to 1h).
        // ponytail: USE_EXACT_ALARM is fine for a sideloaded app; Play only allows it for alarm/calendar apps,
        // so before publishing switch to SCHEDULE_EXACT_ALARM + a settings prompt.
        val am = c.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    fun notifyIfNeeded(c: Context) {
        val hour = LocalDateTime.now().hour
        if (hour == 0 || Store.isDoneToday(c)) return
        val open = Store.tasksToday(c).filter { it.id !in Store.checked(c) }
        val n = open.size
        val left = if (n == 1) "1 task" else "$n tasks"
        val s = Store.streak(c)
        val streakText = if (s == 0) "your streak" else "your $s-day streak"
        val (title, body) = when {
            hour < 12 -> "Good morning ☀️" to "$left to go today. Start with: ${open.firstOrNull()?.label}"
            hour < 22 -> "🔥 Don't lose $streakText!" to "$left left. Don't let your hair down."
            hour < 23 -> "⏳ 2 hours left" to "$left left to save $streakText."
            else -> "🚨 LAST CHANCE" to "Midnight is coming for $streakText. $left left!"
        }
        val nm = c.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("streak", "Streak reminders", NotificationManager.IMPORTANCE_HIGH))
        val tap = PendingIntent.getActivity(
            c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        nm.notify(
            1,
            android.app.Notification.Builder(c, "streak")
                .setSmallIcon(R.drawable.ic_flame)
                .setContentTitle(title)
                .setContentText(body)
                .setContentIntent(tap)
                .setAutoCancel(true)
                .build(),
        )
    }

    const val ACTION = "com.adityakulkarni.haircare.REMIND"
}

/** Fires at each slot; also re-arms after reboot / app update (alarms don't survive those). */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, intent: Intent) {
        Store.syncToday(c) // the 00:01 run marks rest days (nothing due) as done
        if (intent.action == Reminders.ACTION) Reminders.notifyIfNeeded(c)
        updateWidgets(c)
        Reminders.scheduleNext(c)
    }
}
