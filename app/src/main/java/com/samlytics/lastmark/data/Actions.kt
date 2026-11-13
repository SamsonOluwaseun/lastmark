package com.samlytics.lastmark.data

import android.content.Context
import com.samlytics.lastmark.reminders.Alarms
import com.samlytics.lastmark.shield.Shield
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/** Everything the app can do, in one place, so screens, alarms and the shield share the same rules. */
object Actions {

    private fun store(ctx: Context) = Store.get(ctx)
    private fun now() = System.currentTimeMillis()
    private fun newId() = now() * 1000 + Random.nextInt(1000)
    fun todayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    // Tasks

    fun addTask(ctx: Context, title: String, nextStep: String, today: Boolean) {
        if (title.isBlank()) return
        val max = store(ctx).settings.value.maxToday
        store(ctx).updateTasks { list ->
            val todayCount = list.count { it.today && !it.done }
            list + Task(
                id = newId(),
                title = title.trim(),
                nextStep = nextStep.trim(),
                today = today && todayCount < max,
                todayOrder = todayCount
            )
        }
    }

    fun saveTask(ctx: Context, task: Task) {
        store(ctx).updateTasks { list ->
            list.map { if (it.id == task.id) task.copy(title = task.title.trim(), nextStep = task.nextStep.trim(), updatedAt = now()) else it }
        }
    }

    fun deleteTask(ctx: Context, id: Long) {
        if (store(ctx).focus.value.taskId == id) stop(ctx)
        store(ctx).updateTasks { list -> list.filterNot { it.id == id } }
    }

    fun toggleDone(ctx: Context, id: Long) {
        store(ctx).updateTasks { list ->
            list.map { if (it.id == id) it.copy(done = !it.done, doneAt = if (!it.done) now() else 0L, updatedAt = now()) else it }
        }
    }

    fun toggleToday(ctx: Context, id: Long): Boolean {
        val s = store(ctx).settings.value
        val list = store(ctx).tasks.value
        val task = list.find { it.id == id } ?: return false
        if (!task.today && list.count { it.today && !it.done } >= s.maxToday) return false
        val order = list.count { it.today }
        store(ctx).updateTasks { l -> l.map { if (it.id == id) it.copy(today = !it.today, todayOrder = order) else it } }
        return true
    }

    fun setToday(ctx: Context, ids: List<Long>) {
        store(ctx).updateTasks { list ->
            list.map {
                val i = ids.indexOf(it.id)
                if (i >= 0) it.copy(today = true, todayOrder = i)
                else if (it.today && it.done) it
                else it.copy(today = false)
            }
        }
        store(ctx).updateSettings { it.copy(planDate = todayKey()) }
    }

    fun clearCompleted(ctx: Context) {
        store(ctx).updateTasks { list -> list.filterNot { it.done } }
    }

    /** New day: keep unfinished priorities, drop finished ones from today's list. */
    fun rollover(ctx: Context) {
        val s = store(ctx).settings.value
        val key = todayKey()
        if (s.planDate == key || s.planDate.isEmpty()) {
            if (s.planDate.isEmpty()) store(ctx).updateSettings { it.copy(planDate = key) }
            return
        }
        store(ctx).updateTasks { list -> list.map { if (it.today && it.done) it.copy(today = false) else it } }
        store(ctx).updateSettings { it.copy(planDate = key) }
    }

    fun needsPlan(ctx: Context): Boolean = store(ctx).tasks.value.none { it.today && !it.done }

    // Focus

    fun startFocus(ctx: Context, taskId: Long) {
        val s = store(ctx).settings.value
        val endsAt = now() + s.focusMinutes * 60_000L
        store(ctx).updateFocus { FocusState(phase = Phase.FOCUS, taskId = taskId, endsAt = endsAt) }
        store(ctx).updateTasks { list -> list.map { if (it.id == taskId) it.copy(updatedAt = now()) else it } }
        Alarms.schedulePhaseEnd(ctx, endsAt)
        Shield.startIfReady(ctx)
    }

    fun pause(ctx: Context) {
        val f = store(ctx).focus.value
        if (f.phase != Phase.FOCUS || f.paused) return
        store(ctx).updateFocus { it.copy(paused = true, pausedRemainingMs = (it.endsAt - now()).coerceAtLeast(0)) }
        Alarms.cancelPhaseEnd(ctx)
        Shield.stop(ctx)
    }

    fun resume(ctx: Context) {
        val f = store(ctx).focus.value
        if (f.phase != Phase.FOCUS || !f.paused) return
        val endsAt = now() + f.pausedRemainingMs
        store(ctx).updateFocus { it.copy(paused = false, endsAt = endsAt, pausedRemainingMs = 0) }
        Alarms.schedulePhaseEnd(ctx, endsAt)
        Shield.startIfReady(ctx)
    }

    fun saveNextStep(ctx: Context, taskId: Long?, nextStep: String) {
        if (taskId == null) return
        store(ctx).updateTasks { list -> list.map { if (it.id == taskId) it.copy(nextStep = nextStep.trim(), updatedAt = now()) else it } }
    }

    fun startBreak(ctx: Context, nextStep: String, minutes: Int) {
        val f = store(ctx).focus.value
        saveNextStep(ctx, f.taskId, nextStep)
        val endsAt = now() + minutes * 60_000L
        store(ctx).updateFocus { it.copy(phase = Phase.BREAK, endsAt = endsAt, paused = false, pausedRemainingMs = 0) }
        Shield.stop(ctx)
        if (store(ctx).settings.value.breakReminder) Alarms.schedulePhaseEnd(ctx, endsAt) else Alarms.cancelPhaseEnd(ctx)
    }

    fun blockEnded(ctx: Context) {
        if (store(ctx).focus.value.phase != Phase.FOCUS) return
        store(ctx).updateFocus { it.copy(phase = Phase.BLOCK_DONE) }
        Alarms.cancelPhaseEnd(ctx)
        Shield.stop(ctx)
    }

    fun finishBlock(ctx: Context, done: Boolean, nextStep: String) {
        val id = store(ctx).focus.value.taskId
        if (id != null) {
            store(ctx).updateTasks { list ->
                list.map {
                    if (it.id != id) it
                    else if (done) it.copy(done = true, doneAt = now(), nextStep = "", updatedAt = now())
                    else it.copy(nextStep = nextStep.trim(), updatedAt = now())
                }
            }
        }
        stop(ctx)
    }

    fun stop(ctx: Context) {
        Alarms.cancelPhaseEnd(ctx)
        Shield.stop(ctx)
        store(ctx).updateFocus { FocusState() }
    }

    fun snooze(ctx: Context, minutes: Int) {
        store(ctx).updateFocus { it.copy(snoozeUntil = now() + minutes * 60_000L) }
    }

    fun checkTimers(ctx: Context) {
        val f = store(ctx).focus.value
        if (f.phase == Phase.FOCUS && !f.paused && now() >= f.endsAt) blockEnded(ctx)
    }

    // Settings side effects

    fun applyReminders(ctx: Context) {
        val s = store(ctx).settings.value
        if (s.onboarded && s.morningReminder) Alarms.scheduleMorning(ctx, s.planHour, s.planMinute)
        else Alarms.cancelMorning(ctx)
    }

    fun resetAll(ctx: Context) {
        Alarms.cancelPhaseEnd(ctx)
        Alarms.cancelMorning(ctx)
        Shield.stop(ctx)
        store(ctx).clearAll()
    }
}
