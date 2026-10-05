package com.adityakulkarni.haircare

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

// Plain RemoteViews, not Glance: Glance redraws through a background session that Android
// starts/stops on its own, and updates sent around that lifecycle were lost (stale "0 days").
// updateAppWidget() here is synchronous, so the widget always matches Store.
class HairCareWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = updateWidgets(context)
}

private val LABELS = intArrayOf(R.id.label0, R.id.label1, R.id.label2, R.id.label3, R.id.label4, R.id.label5, R.id.label6)
private val DOTS = intArrayOf(R.id.dot0, R.id.dot1, R.id.dot2, R.id.dot3, R.id.dot4, R.id.dot5, R.id.dot6)

/** Duolingo-style streak card: streak, mood line, last 7 days. Ticking happens in the app. */
fun updateWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val ids = manager.getAppWidgetIds(ComponentName(context, HairCareWidgetReceiver::class.java))
    if (ids.isEmpty()) return

    val today = LocalDate.now()
    val doneDays = Store.doneDays(context)
    val doneToday = today in doneDays
    val streak = streak(doneDays, today)
    val u = urgency(doneToday, LocalTime.now())

    val v = RemoteViews(context.packageName, R.layout.widget)
    v.setInt(
        R.id.root, "setBackgroundResource",
        when (u) {
            Urgency.DONE -> R.drawable.widget_bg_done
            Urgency.CALM -> R.drawable.widget_bg_calm
            Urgency.WARN -> R.drawable.widget_bg_warn
            Urgency.PANIC -> R.drawable.widget_bg_panic
        },
    )
    v.setTextViewText(R.id.streak, if (streak == 1) "1 day" else "$streak days")
    v.setTextViewText(R.id.headline, headline(u, streak))
    v.setInt(R.id.flame, "setColorFilter", if (doneToday) 0xFFFDE68A.toInt() else 0x80FFFFFF.toInt())
    v.setContentDescription(R.id.flame, if (doneToday) "Flame lit" else "Flame not lit yet")

    val checkColor = heroColors(u).first.toArgb()
    lastWeek(today).forEachIndexed { i, d ->
        val done = d in doneDays
        v.setTextViewText(LABELS[i], d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()))
        v.setTextViewText(DOTS[i], if (done) "✓" else "")
        v.setTextColor(DOTS[i], checkColor)
        v.setInt(
            DOTS[i], "setBackgroundResource",
            when {
                done -> R.drawable.dot_done
                d == today -> R.drawable.dot_today
                else -> R.drawable.dot_empty
            },
        )
    }

    val open = PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
    )
    v.setOnClickPendingIntent(R.id.root, open)
    manager.updateAppWidget(ids, v)
}
