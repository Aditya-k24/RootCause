package com.adityakulkarni.haircare

import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** When a task is due: on [weekdays], or only on day [monthDay] of each month when that's set. */
data class Schedule(val weekdays: Set<DayOfWeek> = DayOfWeek.entries.toSet(), val monthDay: Int? = null) {
    fun isDue(date: LocalDate) = monthDay?.let { date.dayOfMonth == it } ?: (date.dayOfWeek in weekdays)
}

data class Task(
    val id: String,
    val label: String,
    val hint: String,
    val group: String,
    val icon: String,
    val schedule: Schedule = Schedule(),
    val camera: Boolean = false, // ticking opens the camera for progress shots
)

const val MORNING = "Morning"
const val SCALP = "Scalp care"
const val EVENING = "Evening"
const val ANYTIME = "Anytime"
val GROUPS = listOf(MORNING, SCALP, EVENING, ANYTIME)

/** Icon keys the editor offers; MainActivity maps them to vectors. */
val ICONS = listOf("pill", "workout", "meditate", "drop", "spa", "shower", "camera", "food", "water", "sleep", "heart", "check")

/** Suggested starting routine (the author's own); everyone can edit it in the app. */
val DEFAULT_ROUTINE = listOf(
    Task("d3", "Vitamin D3", "With a meal that has some fat", MORNING, "pill"),
    Task("b12", "Vitamin B12", "Any time, with or without food", MORNING, "pill"),
    Task("iron", "Iron", "With vitamin C, away from tea and coffee", MORNING, "pill"),
    Task("workout", "Morning workout", "Get the blood flowing", MORNING, "workout"),
    Task("meditate", "Meditate 15 min", "Lower stress, less shedding", MORNING, "meditate"),
    Task("minoxidil", "Minoxidil 5%", "On a dry scalp, leave it on", SCALP, "drop"),
    Task("massage", "Scalp massage", "4 minutes, fingertips, firm circles", SCALP, "spa"),
    Task("keto", "Ketoconazole shampoo", "Lather, leave 3–5 min, rinse", SCALP, "shower", Schedule(setOf(MONDAY, THURSDAY))),
    Task(
        "photos", "Progress photos", "3 shots: crown, hairline, top. Same spot, same light.", SCALP, "camera",
        Schedule(monthDay = 1), camera = true,
    ),
)

fun tasksFor(routine: List<Task>, date: LocalDate) = routine.filter { it.schedule.isDue(date) }

fun describe(s: Schedule): String = when {
    s.monthDay != null -> "Monthly on the ${ordinal(s.monthDay)}"
    s.weekdays.size == 7 -> "Every day"
    s.weekdays.isEmpty() -> "Never"
    else -> DayOfWeek.entries.filter { it in s.weekdays }
        .joinToString(", ") { it.name.take(3).lowercase().replaceFirstChar(Char::uppercase) }
}

fun ordinal(n: Int) = "$n" + when {
    n % 100 in 11..13 -> "th"
    n % 10 == 1 -> "st"
    n % 10 == 2 -> "nd"
    n % 10 == 3 -> "rd"
    else -> "th"
}

fun encodeRoutine(routine: List<Task>): String = JSONArray(
    routine.map { t ->
        JSONObject()
            .put("id", t.id).put("label", t.label).put("hint", t.hint).put("group", t.group).put("icon", t.icon)
            .put("weekdays", JSONArray(t.schedule.weekdays.map { it.name }))
            .put("monthDay", t.schedule.monthDay ?: JSONObject.NULL)
            .put("camera", t.camera)
    },
).toString()

fun decodeRoutine(json: String): List<Task> {
    val a = JSONArray(json)
    return (0 until a.length()).map { i ->
        val o = a.getJSONObject(i)
        val days = o.getJSONArray("weekdays")
        Task(
            o.getString("id"), o.getString("label"), o.optString("hint"), o.optString("group", ANYTIME),
            o.optString("icon", "check"),
            Schedule(
                (0 until days.length()).map { DayOfWeek.valueOf(days.getString(it)) }.toSet(),
                if (o.isNull("monthDay")) null else o.getInt("monthDay"),
            ),
            o.optBoolean("camera"),
        )
    }
}

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
