package com.adityakulkarni.haircare

import java.time.DayOfWeek
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class Task(
    val id: String,
    val label: String,
    val hint: String,
    val group: String,
    val days: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
)

const val MORNING = "Morning routine"
const val SCALP = "Scalp care"

// ponytail: hardcoded routine, add an edit screen if the routine changes often
val TASKS = listOf(
    Task("d3", "Vitamin D3", "With a meal that has some fat", MORNING),
    Task("b12", "Vitamin B12", "Any time, with or without food", MORNING),
    Task("iron", "Iron", "With vitamin C, away from tea and coffee", MORNING),
    Task("workout", "Morning workout", "Get the blood flowing", MORNING),
    Task("meditate", "Meditate 15 min", "Lower stress, less shedding", MORNING),
    Task("minoxidil", "Minoxidil 5%", "On a dry scalp, leave it on", SCALP),
    Task("massage", "Scalp massage", "4 minutes, fingertips, firm circles", SCALP),
    Task("keto", "Ketoconazole shampoo", "Lather, leave 3–5 min, rinse", SCALP, setOf(MONDAY, THURSDAY)),
)

fun tasksFor(date: LocalDate) = TASKS.filter { date.dayOfWeek in it.days }

/** Consecutive completed days ending today, or yesterday if today isn't done yet (streak still alive). */
fun streak(done: Set<LocalDate>, today: LocalDate): Int {
    var d = if (today in done) today else today.minusDays(1)
    var n = 0
    while (d in done) { n++; d = d.minusDays(1) }
    return n
}

fun bestStreak(done: Set<LocalDate>): Int =
    done.filter { it.minusDays(1) !in done }.maxOfOrNull { start ->
        generateSequence(start) { it.plusDays(1) }.takeWhile { it in done }.count()
    } ?: 0

/** The 7 days ending today, oldest first, like Duolingo's week strip. */
fun lastWeek(today: LocalDate) = (6 downTo 0).map { today.minusDays(it.toLong()) }

data class Milestone(val day: Int, val title: String, val why: String)

val MILESTONES = listOf(
    Milestone(3, "Getting started", "Three days in a row. The habit is forming."),
    Milestone(7, "One week strong", "A full week. Keep the chain going."),
    Milestone(14, "Two weeks", "Some shedding now is normal with minoxidil: old hairs making room."),
    Milestone(30, "One month", "Hair grows about 1 cm a month. Your routine is now automatic."),
    Milestone(60, "Two months", "Follicles are shifting into the growth phase. Stay consistent."),
    Milestone(90, "Three months", "Around when early minoxidil results start to show."),
    Milestone(120, "Four months", "Take comparison photos. Changes become visible here."),
    Milestone(180, "Six months", "The point where you can fairly judge results."),
    Milestone(365, "One year", "A full year of showing up for your hair."),
)

fun nextMilestone(streak: Int) = MILESTONES.firstOrNull { it.day > streak }

enum class Urgency { DONE, CALM, WARN, PANIC }

fun urgency(doneToday: Boolean, now: LocalTime) = when {
    doneToday -> Urgency.DONE
    now.hour >= 23 -> Urgency.PANIC
    now.hour >= 20 -> Urgency.WARN
    else -> Urgency.CALM
}

fun headline(u: Urgency, streak: Int) = when (u) {
    Urgency.DONE -> "Streak safe. See you tomorrow!"
    Urgency.CALM -> if (streak == 0) "Start your streak today" else "Keep it going today"
    Urgency.WARN -> "Your streak is at risk!"
    Urgency.PANIC -> "Under 1 hour left!"
}

// 00:01 = midnight refresh so the widget rolls over to the new day
val SLOTS: List<LocalTime> = listOf(0 to 1, 8 to 0, 20 to 0, 22 to 0, 23 to 0).map { (h, m) -> LocalTime.of(h, m) }

fun nextSlot(now: LocalDateTime): LocalDateTime =
    listOf(now.toLocalDate(), now.toLocalDate().plusDays(1))
        .flatMap { d -> SLOTS.map { d.atTime(it) } }
        .first { it.isAfter(now) }
