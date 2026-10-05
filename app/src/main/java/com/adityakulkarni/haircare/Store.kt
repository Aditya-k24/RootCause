package com.adityakulkarni.haircare

import android.content.Context
import java.time.LocalDate

object Store {
    private fun prefs(c: Context) = c.getSharedPreferences("haircare", Context.MODE_PRIVATE)

    // ponytail: one key per day, never pruned; ~100 bytes/day so years before it matters
    fun checked(c: Context, date: LocalDate = LocalDate.now()): Set<String> =
        prefs(c).getStringSet("checked_$date", null).orEmpty()

    fun doneDays(c: Context): Set<LocalDate> =
        prefs(c).getStringSet("done", null).orEmpty().map(LocalDate::parse).toSet()

    fun isDoneToday(c: Context) = LocalDate.now() in doneDays(c)

    fun streak(c: Context) = streak(doneDays(c), LocalDate.now())

    fun toggle(c: Context, id: String) {
        val today = LocalDate.now()
        val checked = checked(c, today).toMutableSet()
        if (!checked.add(id)) checked.remove(id)
        val done = prefs(c).getStringSet("done", null).orEmpty().toMutableSet()
        if (checked.containsAll(tasksFor(today).map { it.id })) done += today.toString() else done -= today.toString()
        prefs(c).edit().putStringSet("checked_$today", checked).putStringSet("done", done).commit()
    }
}
