package com.samlytics.lastmark.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object Alarms {
    const val ACTION_PHASE_END = "com.samlytics.lastmark.PHASE_END"
    const val ACTION_MORNING = "com.samlytics.lastmark.MORNING"

    private fun manager(ctx: Context) = ctx.getSystemService(AlarmManager::class.java)

    private fun intent(ctx: Context, action: String, code: Int): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, code,
            Intent(ctx, AlarmReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun set(ctx: Context, at: Long, pi: PendingIntent) {
        val am = manager(ctx)
        try {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    fun schedulePhaseEnd(ctx: Context, at: Long) = set(ctx, at, intent(ctx, ACTION_PHASE_END, 1))

    fun cancelPhaseEnd(ctx: Context) = manager(ctx).cancel(intent(ctx, ACTION_PHASE_END, 1))

    fun scheduleMorning(ctx: Context, hour: Int, minute: Int) {
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        set(ctx, next.timeInMillis, intent(ctx, ACTION_MORNING, 2))
    }

    fun cancelMorning(ctx: Context) = manager(ctx).cancel(intent(ctx, ACTION_MORNING, 2))

    fun canExact(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 31 || manager(ctx).canScheduleExactAlarms()
}
