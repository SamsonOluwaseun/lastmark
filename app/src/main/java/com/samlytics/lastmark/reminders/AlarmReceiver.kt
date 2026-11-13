package com.samlytics.lastmark.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.samlytics.lastmark.data.Actions
import com.samlytics.lastmark.data.Phase
import com.samlytics.lastmark.data.Store

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val store = Store.get(ctx)
        val now = System.currentTimeMillis()
        when (intent.action) {
            Alarms.ACTION_PHASE_END -> {
                val f = store.focus.value
                val task = store.tasks.value.find { it.id == f.taskId }
                when (f.phase) {
                    Phase.FOCUS -> if (!f.paused) {
                        Actions.blockEnded(ctx)
                        Notify.show(
                            ctx, Notify.ID_BLOCK, "Focus block done",
                            "Did you finish \"${task?.title ?: "your task"}\"? Tap to check in."
                        )
                    }
                    Phase.BREAK -> {
                        val s = store.settings.value
                        val step = task?.nextStep?.takeIf { it.isNotBlank() }
                        Notify.show(
                            ctx, Notify.ID_BREAK, "Break's over",
                            if (step != null) "Pick up here: $step" else "Time to get back to it."
                        )
                        if (s.breakReminder && s.nudgeMinutes > 0) {
                            Alarms.schedulePhaseEnd(ctx, now + s.nudgeMinutes * 60_000L)
                        }
                    }
                    else -> Unit
                }
            }

            Alarms.ACTION_MORNING -> {
                val s = store.settings.value
                if (s.onboarded && s.morningReminder) {
                    Actions.rollover(ctx)
                    Notify.show(ctx, Notify.ID_MORNING, "Plan your day", "Pick up to ${s.maxToday} priorities for today.")
                    Alarms.scheduleMorning(ctx, s.planHour, s.planMinute)
                }
            }

            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Actions.applyReminders(ctx)
                val f = store.focus.value
                if ((f.phase == Phase.FOCUS && !f.paused) || f.phase == Phase.BREAK) {
                    Alarms.schedulePhaseEnd(ctx, maxOf(f.endsAt, now + 5_000))
                }
            }
        }
    }
}
