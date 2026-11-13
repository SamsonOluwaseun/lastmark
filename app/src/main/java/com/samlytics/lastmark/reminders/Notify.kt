package com.samlytics.lastmark.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.samlytics.lastmark.MainActivity
import com.samlytics.lastmark.R

object Notify {
    const val CHANNEL_REMINDERS = "reminders"
    const val CHANNEL_SHIELD = "shield"
    const val ID_BLOCK = 10
    const val ID_BREAK = 11
    const val ID_MORNING = 12
    const val ID_SHIELD = 20

    fun ensureChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDERS, "Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Daily planning, break over and focus block reminders"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_SHIELD, "Drift shield", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shown while the drift shield is watching during a focus block"
            }
        )
    }

    fun openApp(ctx: Context): PendingIntent = PendingIntent.getActivity(
        ctx, 0,
        Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun show(ctx: Context, id: Int, title: String, text: String) {
        ensureChannels(ctx)
        val nmc = NotificationManagerCompat.from(ctx)
        if (!nmc.areNotificationsEnabled()) return
        val n = NotificationCompat.Builder(ctx, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openApp(ctx))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        try {
            nmc.notify(id, n)
        } catch (e: SecurityException) {
            // Notification permission was withdrawn. Nothing to do.
        }
    }
}
