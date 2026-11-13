package com.samlytics.lastmark.data

enum class Strictness(val label: String, val description: String) {
    OFF("Off", "No shield. Lastmark only reminds you."),
    GENTLE("Gentle", "A reminder appears. One tap to carry on for 10 minutes."),
    FIRM("Firm", "Choose: back to work, or 5 more minutes."),
    STRICT("Strict", "The app stays blocked until the focus block ends.")
}

enum class Phase { IDLE, FOCUS, BREAK, BLOCK_DONE }

data class Task(
    val id: Long,
    val title: String,
    val nextStep: String = "",
    val done: Boolean = false,
    val today: Boolean = false,
    val todayOrder: Int = 0,
    val doneAt: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis()
)

data class AppSettings(
    val onboarded: Boolean = false,
    val name: String = "",
    val focusMinutes: Int = 25,
    val breakMinutes: Int = 5,
    val maxToday: Int = 3,
    val morningReminder: Boolean = true,
    val planHour: Int = 8,
    val planMinute: Int = 30,
    val breakReminder: Boolean = true,
    val nudgeMinutes: Int = 10,
    val strictness: Strictness = Strictness.FIRM,
    val shieldedApps: Set<String> = emptySet(),
    val showCalendar: Boolean = false,
    val calendarIds: Set<Long> = emptySet(),
    val planDate: String = ""
)

data class FocusState(
    val phase: Phase = Phase.IDLE,
    val taskId: Long? = null,
    val endsAt: Long = 0L,
    val paused: Boolean = false,
    val pausedRemainingMs: Long = 0L,
    val snoozeUntil: Long = 0L
)
