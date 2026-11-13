package com.samlytics.lastmark.calendar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.util.Calendar

data class CalendarInfo(val id: Long, val name: String, val account: String, val color: Int)
data class CalendarEvent(val title: String, val begin: Long, val end: Long, val allDay: Boolean, val color: Int)

/**
 * Reads calendars already synced to the phone (Google, Outlook, Samsung and others).
 * No API keys or sign-in needed, which keeps the app easy to run for everyone.
 */
object CalendarRepo {

    fun hasPermission(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    fun calendars(ctx: Context): List<CalendarInfo> {
        if (!hasPermission(ctx)) return emptyList()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR
        )
        return try {
            ctx.contentResolver.query(CalendarContract.Calendars.CONTENT_URI, projection, null, null, null)?.use { c ->
                val out = mutableListOf<CalendarInfo>()
                while (c.moveToNext()) {
                    out += CalendarInfo(c.getLong(0), c.getString(1) ?: "Calendar", c.getString(2) ?: "", c.getInt(3))
                }
                out
            } ?: emptyList()
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    fun todayEvents(ctx: Context, calendarIds: Set<Long>): List<CalendarEvent> {
        if (!hasPermission(ctx)) return emptyList()
        val start = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val end = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.DISPLAY_COLOR
        )
        return try {
            val cursor = CalendarContract.Instances.query(ctx.contentResolver, projection, start.timeInMillis, end.timeInMillis)
                ?: return emptyList()
            cursor.use { c ->
                val out = mutableListOf<CalendarEvent>()
                while (c.moveToNext()) {
                    val calId = c.getLong(4)
                    if (calendarIds.isNotEmpty() && calId !in calendarIds) continue
                    out += CalendarEvent(c.getString(0) ?: "(No title)", c.getLong(1), c.getLong(2), c.getInt(3) == 1, c.getInt(5))
                }
                out.sortedWith(compareBy({ !it.allDay }, { it.begin }))
            }
        } catch (e: SecurityException) {
            emptyList()
        }
    }
}
