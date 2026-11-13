package com.samlytics.lastmark.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** Simple on-device storage. Everything stays on the phone. */
class Store private constructor(context: Context) {

    private val prefs = context.getSharedPreferences("lastmark", Context.MODE_PRIVATE)

    private val _tasks = MutableStateFlow(loadTasks())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _focus = MutableStateFlow(loadFocus())
    val focus: StateFlow<FocusState> = _focus.asStateFlow()

    @Synchronized
    fun updateTasks(change: (List<Task>) -> List<Task>) {
        val next = change(_tasks.value)
        _tasks.value = next
        saveTasks(next)
    }

    @Synchronized
    fun updateSettings(change: (AppSettings) -> AppSettings) {
        val next = change(_settings.value)
        _settings.value = next
        saveSettings(next)
    }

    @Synchronized
    fun updateFocus(change: (FocusState) -> FocusState) {
        val next = change(_focus.value)
        _focus.value = next
        saveFocus(next)
    }

    @Synchronized
    fun clearAll() {
        prefs.edit().clear().apply()
        _tasks.value = emptyList()
        _settings.value = AppSettings()
        _focus.value = FocusState()
    }

    private fun loadTasks(): List<Task> = try {
        val arr = JSONArray(prefs.getString(KEY_TASKS, "[]"))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Task(
                id = o.getLong("id"),
                title = o.optString("title"),
                nextStep = o.optString("nextStep"),
                done = o.optBoolean("done"),
                today = o.optBoolean("today"),
                todayOrder = o.optInt("todayOrder"),
                doneAt = o.optLong("doneAt"),
                updatedAt = o.optLong("updatedAt")
            )
        }
    } catch (e: Exception) {
        emptyList()
    }

    private fun saveTasks(list: List<Task>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("title", it.title)
                    .put("nextStep", it.nextStep)
                    .put("done", it.done)
                    .put("today", it.today)
                    .put("todayOrder", it.todayOrder)
                    .put("doneAt", it.doneAt)
                    .put("updatedAt", it.updatedAt)
            )
        }
        prefs.edit().putString(KEY_TASKS, arr.toString()).apply()
    }

    private fun loadSettings(): AppSettings = try {
        val raw = prefs.getString(KEY_SETTINGS, null)
        if (raw == null) AppSettings() else {
            val o = JSONObject(raw)
            val d = AppSettings()
            AppSettings(
                onboarded = o.optBoolean("onboarded", d.onboarded),
                name = o.optString("name", d.name),
                focusMinutes = o.optInt("focusMinutes", d.focusMinutes),
                breakMinutes = o.optInt("breakMinutes", d.breakMinutes),
                maxToday = o.optInt("maxToday", d.maxToday),
                morningReminder = o.optBoolean("morningReminder", d.morningReminder),
                planHour = o.optInt("planHour", d.planHour),
                planMinute = o.optInt("planMinute", d.planMinute),
                breakReminder = o.optBoolean("breakReminder", d.breakReminder),
                nudgeMinutes = o.optInt("nudgeMinutes", d.nudgeMinutes),
                strictness = runCatching { Strictness.valueOf(o.optString("strictness")) }.getOrDefault(d.strictness),
                shieldedApps = o.optJSONArray("shieldedApps").toStringSet(),
                showCalendar = o.optBoolean("showCalendar", d.showCalendar),
                calendarIds = o.optJSONArray("calendarIds").toLongSet(),
                planDate = o.optString("planDate", d.planDate)
            )
        }
    } catch (e: Exception) {
        AppSettings()
    }

    private fun saveSettings(s: AppSettings) {
        val o = JSONObject()
            .put("onboarded", s.onboarded)
            .put("name", s.name)
            .put("focusMinutes", s.focusMinutes)
            .put("breakMinutes", s.breakMinutes)
            .put("maxToday", s.maxToday)
            .put("morningReminder", s.morningReminder)
            .put("planHour", s.planHour)
            .put("planMinute", s.planMinute)
            .put("breakReminder", s.breakReminder)
            .put("nudgeMinutes", s.nudgeMinutes)
            .put("strictness", s.strictness.name)
            .put("shieldedApps", JSONArray(s.shieldedApps.toList()))
            .put("showCalendar", s.showCalendar)
            .put("calendarIds", JSONArray(s.calendarIds.toList()))
            .put("planDate", s.planDate)
        prefs.edit().putString(KEY_SETTINGS, o.toString()).apply()
    }

    private fun loadFocus(): FocusState = try {
        val raw = prefs.getString(KEY_FOCUS, null)
        if (raw == null) FocusState() else {
            val o = JSONObject(raw)
            FocusState(
                phase = runCatching { Phase.valueOf(o.optString("phase")) }.getOrDefault(Phase.IDLE),
                taskId = if (o.has("taskId")) o.getLong("taskId") else null,
                endsAt = o.optLong("endsAt"),
                paused = o.optBoolean("paused"),
                pausedRemainingMs = o.optLong("pausedRemainingMs"),
                snoozeUntil = o.optLong("snoozeUntil")
            )
        }
    } catch (e: Exception) {
        FocusState()
    }

    private fun saveFocus(f: FocusState) {
        val o = JSONObject()
            .put("phase", f.phase.name)
            .put("endsAt", f.endsAt)
            .put("paused", f.paused)
            .put("pausedRemainingMs", f.pausedRemainingMs)
            .put("snoozeUntil", f.snoozeUntil)
        f.taskId?.let { o.put("taskId", it) }
        prefs.edit().putString(KEY_FOCUS, o.toString()).apply()
    }

    private fun JSONArray?.toStringSet(): Set<String> =
        if (this == null) emptySet() else (0 until length()).map { getString(it) }.toSet()

    private fun JSONArray?.toLongSet(): Set<Long> =
        if (this == null) emptySet() else (0 until length()).map { getLong(it) }.toSet()

    companion object {
        private const val KEY_TASKS = "tasks"
        private const val KEY_SETTINGS = "settings"
        private const val KEY_FOCUS = "focus"

        @Volatile
        private var instance: Store? = null

        fun get(context: Context): Store =
            instance ?: synchronized(this) {
                instance ?: Store(context.applicationContext).also { instance = it }
            }
    }
}
