package com.adityakulkarni.haircare

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class LogicTest {
    private val mon = LocalDate.of(2026, 10, 5) // a Monday

    @Test fun streakCounts() {
        assertEquals(0, streak(emptySet(), mon))
        val three = setOf(mon, mon.minusDays(1), mon.minusDays(2))
        assertEquals(3, streak(three, mon))
        // today not done yet: streak still alive from yesterday
        assertEquals(2, streak(three - mon, mon))
        // gap breaks it
        assertEquals(1, streak(setOf(mon, mon.minusDays(2)), mon))
        // missed yesterday and today: dead
        assertEquals(0, streak(setOf(mon.minusDays(2)), mon))
    }

    @Test fun urgencyByTime() {
        assertEquals(Urgency.DONE, urgency(true, LocalTime.of(23, 30)))
        assertEquals(Urgency.CALM, urgency(false, LocalTime.of(19, 59)))
        assertEquals(Urgency.WARN, urgency(false, LocalTime.of(20, 0)))
        assertEquals(Urgency.PANIC, urgency(false, LocalTime.of(23, 0)))
    }

    @Test fun ketoOnlyMonThu() {
        assertEquals(8, tasksFor(DEFAULT_ROUTINE, mon).size)
        assertEquals(7, tasksFor(DEFAULT_ROUTINE, mon.plusDays(1)).size)
        assertEquals(8, tasksFor(DEFAULT_ROUTINE, mon.plusDays(3)).size)
    }

    @Test fun photosOnFirstOfMonth() {
        val oct1 = LocalDate.of(2026, 10, 1) // a Thursday: keto + photos
        assertEquals(true, tasksFor(DEFAULT_ROUTINE, oct1).any { it.id == "photos" })
        assertEquals(9, tasksFor(DEFAULT_ROUTINE, oct1).size)
        assertEquals(false, tasksFor(DEFAULT_ROUTINE, oct1.plusDays(1)).any { it.id == "photos" })
    }

    @Test fun nextSlotWraps() {
        assertEquals(mon.atTime(8, 0), nextSlot(mon.atTime(7, 0)))
        assertEquals(mon.atTime(20, 0), nextSlot(mon.atTime(8, 0)))
        assertEquals(mon.plusDays(1).atTime(0, 1), nextSlot(mon.atTime(23, 30)))
        assertEquals(mon.atTime(8, 0), nextSlot(LocalDateTime.of(mon, LocalTime.of(0, 1))))
    }

    @Test fun bestAndWeek() {
        assertEquals(0, bestStreak(emptySet()))
        val runs = setOf(mon, mon.minusDays(1), mon.minusDays(5), mon.minusDays(6), mon.minusDays(7))
        assertEquals(3, bestStreak(runs))
        assertEquals(listOf(mon.minusDays(6), mon), lastWeek(mon).let { listOf(it.first(), it.last()) })
    }

    @Test fun milestones() {
        assertEquals(3, nextMilestone(0)!!.day)
        assertEquals(14, nextMilestone(7)!!.day)
        assertEquals(null, nextMilestone(365))
    }

    @Test fun routineJsonRoundTrip() {
        assertEquals(DEFAULT_ROUTINE, decodeRoutine(encodeRoutine(DEFAULT_ROUTINE)))
    }

    @Test fun scheduleText() {
        assertEquals("Every day", describe(Schedule()))
        assertEquals("Mon, Thu", describe(Schedule(setOf(java.time.DayOfWeek.THURSDAY, java.time.DayOfWeek.MONDAY))))
        assertEquals("Monthly on the 1st", describe(Schedule(monthDay = 1)))
        assertEquals("11th", ordinal(11))
        assertEquals("22nd", ordinal(22))
    }
}
