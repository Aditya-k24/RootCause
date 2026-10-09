import Foundation

// Port of app/src/main/java/com/adityakulkarni/haircare/Logic.kt. Keep the two in step.

/// A calendar date with no time or zone, like java.time.LocalDate.
public struct Day: Hashable, Comparable, Codable, CustomStringConvertible {
    public let year: Int, month: Int, day: Int

    // ponytail: fixed UTC gregorian calendar so day arithmetic never trips over DST
    private static let cal: Calendar = {
        var c = Calendar(identifier: .gregorian)
        c.timeZone = TimeZone(identifier: "UTC")!
        return c
    }()

    public init(_ year: Int, _ month: Int, _ day: Int) {
        self.year = year; self.month = month; self.day = day
    }

    public init(_ date: Date, calendar: Calendar = .current) {
        let c = calendar.dateComponents([.year, .month, .day], from: date)
        self.init(c.year!, c.month!, c.day!)
    }

    /// Parses "2026-10-05".
    public init?(_ s: String) {
        let p = s.split(separator: "-").compactMap { Int($0) }
        guard p.count == 3 else { return nil }
        self.init(p[0], p[1], p[2])
    }

    public static func today() -> Day { Day(Date()) }

    private var date: Date { Day.cal.date(from: DateComponents(year: year, month: month, day: day))! }

    public func plus(_ days: Int) -> Day { Day(Day.cal.date(byAdding: .day, value: days, to: date)!, calendar: Day.cal) }

    public var weekday: Weekday {
        // Calendar: 1 = Sunday … 7 = Saturday
        Weekday.allCases[(Day.cal.component(.weekday, from: date) + 5) % 7]
    }

    /// Start of this day in `calendar`'s zone, at the given time.
    public func at(_ hour: Int, _ minute: Int, calendar: Calendar = .current) -> Date {
        calendar.date(from: DateComponents(year: year, month: month, day: day, hour: hour, minute: minute))!
    }

    public var description: String { String(format: "%04d-%02d-%02d", year, month, day) }

    public static func < (a: Day, b: Day) -> Bool { (a.year, a.month, a.day) < (b.year, b.month, b.day) }
}

public enum Weekday: String, CaseIterable, Codable {
    case monday = "MONDAY", tuesday = "TUESDAY", wednesday = "WEDNESDAY", thursday = "THURSDAY"
    case friday = "FRIDAY", saturday = "SATURDAY", sunday = "SUNDAY"

    public var short: String { rawValue.prefix(3).capitalized }
}

/// When a task is due: on `weekdays`, or only on day `monthDay` of each month when that's set.
public struct Schedule: Hashable, Codable {
    public var weekdays: Set<Weekday>
    public var monthDay: Int?

    public init(weekdays: Set<Weekday> = Set(Weekday.allCases), monthDay: Int? = nil) {
        self.weekdays = weekdays; self.monthDay = monthDay
    }

    public func isDue(_ d: Day) -> Bool {
        if let monthDay { return d.day == monthDay }
        return weekdays.contains(d.weekday)
    }
}

public struct RoutineTask: Hashable, Codable, Identifiable {
    public var id: String
    public var label: String
    public var hint: String
    public var group: String
    public var icon: String
    public var schedule: Schedule
    public var camera: Bool // ticking opens the camera for progress shots

    public init(
        _ id: String, _ label: String, _ hint: String, _ group: String, _ icon: String,
        _ schedule: Schedule = Schedule(), camera: Bool = false
    ) {
        self.id = id; self.label = label; self.hint = hint; self.group = group; self.icon = icon
        self.schedule = schedule; self.camera = camera
    }
}

public let MORNING = "Morning"
public let SCALP = "Scalp care"
public let EVENING = "Evening"
public let ANYTIME = "Anytime"
public let GROUPS = [MORNING, SCALP, EVENING, ANYTIME]

/// Icon keys the editor offers; the app maps them to SF Symbols.
public let ICONS = ["pill", "workout", "meditate", "drop", "spa", "shower", "camera", "food", "water", "sleep", "heart", "check"]

/// Suggested starting routine (the author's own); everyone can edit it in the app.
public let DEFAULT_ROUTINE = [
    RoutineTask("d3", "Vitamin D3", "With a meal that has some fat", MORNING, "pill"),
    RoutineTask("b12", "Vitamin B12", "Any time, with or without food", MORNING, "pill"),
    RoutineTask("iron", "Iron", "With vitamin C, away from tea and coffee", MORNING, "pill"),
    RoutineTask("workout", "Morning workout", "Get the blood flowing", MORNING, "workout"),
    RoutineTask("meditate", "Meditate 15 min", "Lower stress, less shedding", MORNING, "meditate"),
    RoutineTask("minoxidil", "Minoxidil 5%", "On a dry scalp, leave it on", SCALP, "drop"),
    RoutineTask("massage", "Scalp massage", "4 minutes, fingertips, firm circles", SCALP, "spa"),
    RoutineTask("keto", "Ketoconazole shampoo", "Lather, leave 3–5 min, rinse", SCALP, "shower", Schedule(weekdays: [.monday, .thursday])),
    RoutineTask(
        "photos", "Progress photos", "3 shots: crown, hairline, top. Same spot, same light.", SCALP, "camera",
        Schedule(monthDay: 1), camera: true
    ),
]

public func tasksFor(_ routine: [RoutineTask], _ d: Day) -> [RoutineTask] { routine.filter { $0.schedule.isDue(d) } }

public func describe(_ s: Schedule) -> String {
    if let m = s.monthDay { return "Monthly on the \(ordinal(m))" }
    if s.weekdays.count == 7 { return "Every day" }
    if s.weekdays.isEmpty { return "Never" }
    return Weekday.allCases.filter(s.weekdays.contains).map(\.short).joined(separator: ", ")
}

public func ordinal(_ n: Int) -> String {
    let suffix: String
    switch (n % 100, n % 10) {
    case (11...13, _): suffix = "th"
    case (_, 1): suffix = "st"
    case (_, 2): suffix = "nd"
    case (_, 3): suffix = "rd"
    default: suffix = "th"
    }
    return "\(n)\(suffix)"
}

public func encodeRoutine(_ routine: [RoutineTask]) -> Data { try! JSONEncoder().encode(routine) }

public func decodeRoutine(_ data: Data) -> [RoutineTask]? { try? JSONDecoder().decode([RoutineTask].self, from: data) }

/// Consecutive completed days ending today, or yesterday if today isn't done yet (streak still alive).
public func streak(_ done: Set<Day>, _ today: Day) -> Int {
    var d = done.contains(today) ? today : today.plus(-1)
    var n = 0
    while done.contains(d) { n += 1; d = d.plus(-1) }
    return n
}

public func bestStreak(_ done: Set<Day>) -> Int {
    done.filter { !done.contains($0.plus(-1)) }.map { start in
        var d = start, n = 0
        while done.contains(d) { n += 1; d = d.plus(1) }
        return n
    }.max() ?? 0
}

/// The 7 days ending today, oldest first, like Duolingo's week strip.
public func lastWeek(_ today: Day) -> [Day] { (0...6).reversed().map { today.plus(-$0) } }

public struct Milestone: Hashable {
    public let day: Int, title: String, why: String
}

public let MILESTONES = [
    Milestone(day: 3, title: "Getting started", why: "Three days in a row. The habit is forming."),
    Milestone(day: 7, title: "One week strong", why: "A full week. Keep the chain going."),
    Milestone(day: 14, title: "Two weeks", why: "Some shedding now is normal with minoxidil: old hairs making room."),
    Milestone(day: 30, title: "One month", why: "Hair grows about 1 cm a month. Your routine is now automatic."),
    Milestone(day: 60, title: "Two months", why: "Follicles are shifting into the growth phase. Stay consistent."),
    Milestone(day: 90, title: "Three months", why: "Around when early minoxidil results start to show."),
    Milestone(day: 120, title: "Four months", why: "Take comparison photos. Changes become visible here."),
    Milestone(day: 180, title: "Six months", why: "The point where you can fairly judge results."),
    Milestone(day: 365, title: "One year", why: "A full year of showing up for your hair."),
]

public func nextMilestone(_ streak: Int) -> Milestone? { MILESTONES.first { $0.day > streak } }

public enum Urgency { case done, calm, warn, panic }

public func urgency(_ doneToday: Bool, hour: Int) -> Urgency {
    if doneToday { return .done }
    if hour >= 23 { return .panic }
    if hour >= 20 { return .warn }
    return .calm
}

public func headline(_ u: Urgency, _ streak: Int) -> String {
    switch u {
    case .done: return "Streak safe. See you tomorrow!"
    case .calm: return streak == 0 ? "Start your streak today" : "Keep it going today"
    case .warn: return "Your streak is at risk!"
    case .panic: return "Under 1 hour left!"
    }
}

// 00:01 = midnight refresh so the widget rolls over to the new day
public let SLOTS = [(0, 1), (8, 0), (20, 0), (22, 0), (23, 0)]

public func nextSlot(after now: Date, calendar: Calendar = .current) -> Date {
    let today = Day(now, calendar: calendar)
    return [today, today.plus(1)]
        .flatMap { d in SLOTS.map { d.at($0.0, $0.1, calendar: calendar) } }
        .first { $0 > now }!
}
