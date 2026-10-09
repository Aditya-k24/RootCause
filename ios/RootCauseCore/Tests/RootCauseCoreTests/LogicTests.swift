import Foundation
import Testing
@testable import RootCauseCore

// Mirrors app/src/test/java/com/adityakulkarni/haircare/LogicTest.kt
struct LogicTests {
    let mon = Day(2026, 10, 5) // a Monday

    @Test func streakCounts() {
        #expect(streak([], mon) == 0)
        let three: Set = [mon, mon.plus(-1), mon.plus(-2)]
        #expect(streak(three, mon) == 3)
        // today not done yet: streak still alive from yesterday
        #expect(streak(three.subtracting([mon]), mon) == 2)
        // gap breaks it
        #expect(streak([mon, mon.plus(-2)], mon) == 1)
        // missed yesterday and today: dead
        #expect(streak([mon.plus(-2)], mon) == 0)
    }

    @Test func urgencyByTime() {
        #expect(urgency(true, hour: 23) == .done)
        #expect(urgency(false, hour: 19) == .calm)
        #expect(urgency(false, hour: 20) == .warn)
        #expect(urgency(false, hour: 23) == .panic)
    }

    @Test func ketoOnlyMonThu() {
        #expect(mon.weekday == .monday)
        #expect(tasksFor(DEFAULT_ROUTINE, mon).count == 8)
        #expect(tasksFor(DEFAULT_ROUTINE, mon.plus(1)).count == 7)
        #expect(tasksFor(DEFAULT_ROUTINE, mon.plus(3)).count == 8)
    }

    @Test func photosOnFirstOfMonth() {
        let oct1 = Day(2026, 10, 1) // a Thursday: keto + photos
        #expect(tasksFor(DEFAULT_ROUTINE, oct1).contains { $0.id == "photos" })
        #expect(tasksFor(DEFAULT_ROUTINE, oct1).count == 9)
        #expect(!tasksFor(DEFAULT_ROUTINE, oct1.plus(1)).contains { $0.id == "photos" })
    }

    @Test func nextSlotWraps() {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = TimeZone(identifier: "America/New_York")!
        func at(_ d: Day, _ h: Int, _ m: Int) -> Date { d.at(h, m, calendar: cal) }
        #expect(nextSlot(after: at(mon, 7, 0), calendar: cal) == at(mon, 8, 0))
        #expect(nextSlot(after: at(mon, 8, 0), calendar: cal) == at(mon, 20, 0))
        #expect(nextSlot(after: at(mon, 23, 30), calendar: cal) == at(mon.plus(1), 0, 1))
        #expect(nextSlot(after: at(mon, 0, 1), calendar: cal) == at(mon, 8, 0))
    }

    @Test func bestAndWeek() {
        #expect(bestStreak([]) == 0)
        let runs: Set = [mon, mon.plus(-1), mon.plus(-5), mon.plus(-6), mon.plus(-7)]
        #expect(bestStreak(runs) == 3)
        let week = lastWeek(mon)
        #expect(week.count == 7)
        #expect([week.first!, week.last!] == [mon.plus(-6), mon])
    }

    @Test func milestones() {
        #expect(nextMilestone(0)?.day == 3)
        #expect(nextMilestone(7)?.day == 14)
        #expect(nextMilestone(365) == nil)
    }

    @Test func routineJsonRoundTrip() {
        #expect(decodeRoutine(encodeRoutine(DEFAULT_ROUTINE)) == DEFAULT_ROUTINE)
    }

    @Test func scheduleText() {
        #expect(describe(Schedule()) == "Every day")
        #expect(describe(Schedule(weekdays: [.thursday, .monday])) == "Mon, Thu")
        #expect(describe(Schedule(monthDay: 1)) == "Monthly on the 1st")
        #expect(ordinal(11) == "11th")
        #expect(ordinal(22) == "22nd")
    }

    @Test func dayParsesAndMonthWraps() {
        #expect(Day("2026-10-05") == mon)
        #expect(mon.description == "2026-10-05")
        #expect(Day(2026, 10, 31).plus(1) == Day(2026, 11, 1))
    }
}
