package com.adityakulkarni.haircare

import android.content.Context
import java.time.LocalDate

object Store {
    private fun prefs(c: Context) = c.getSharedPreferences("haircare", Context.MODE_PRIVATE)

    fun routine(c: Context): List<Task> =
        prefs(c).getString("routine", null)?.let(::decodeRoutine) ?: DEFAULT_ROUTINE

    /** False until the routine editor is first opened; drives the "make it yours" card. */
    fun introSeen(c: Context) = prefs(c).getBoolean("intro_seen", false)

    fun markIntroSeen(c: Context) = prefs(c).edit().putBoolean("intro_seen", true).apply()

    fun saveRoutine(c: Context, routine: List<Task>) {
        prefs(c).edit().putString("routine", encodeRoutine(routine)).commit()
        syncToday(c) // adding/removing tasks can complete or un-complete today
    }

    fun tasksToday(c: Context) = tasksFor(routine(c), LocalDate.now())

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
        prefs(c).edit().putStringSet("checked_$today", checked).commit()
        syncToday(c)
    }

    /**
     * Today counts as done when every task due today is ticked. A day with nothing due is a rest day
     * and also counts, so a weekdays-only routine doesn't break the streak at the weekend.
     */
    fun syncToday(c: Context) {
        val today = LocalDate.now()
        val required = tasksFor(routine(c), today).map { it.id }
        val done = prefs(c).getStringSet("done", null).orEmpty().toMutableSet()
        if (checked(c, today).containsAll(required)) done += today.toString() else done -= today.toString()
        prefs(c).edit().putStringSet("done", done).commit()
    }
}
