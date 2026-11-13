package com.samlytics.lastmark.shield

import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.samlytics.lastmark.R
import com.samlytics.lastmark.data.Phase
import com.samlytics.lastmark.data.Store
import com.samlytics.lastmark.data.Strictness
import com.samlytics.lastmark.reminders.Notify

/** Watches which app is in front during a focus block and shows the shield over chosen apps. */
class ShieldService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var currentPackage: String? = null
    private var lastShownFor: String? = null
    private var lastShownAt = 0L

    private val loop = object : Runnable {
        override fun run() {
            if (check()) handler.postDelayed(this, 1_500)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Notify.ensureChannels(this)
        val notification = NotificationCompat.Builder(this, Notify.CHANNEL_SHIELD)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Focus block in progress")
            .setContentText("Drift shield is on")
            .setContentIntent(Notify.openApp(this))
            .setOngoing(true)
            .build()
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, Notify.ID_SHIELD, notification, type)
        handler.removeCallbacks(loop)
        handler.post(loop)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(loop)
        super.onDestroy()
    }

    /** Returns false when the service should stop. */
    private fun check(): Boolean {
        val store = Store.get(this)
        val f = store.focus.value
        val s = store.settings.value
        if (f.phase != Phase.FOCUS || f.paused || s.strictness == Strictness.OFF) {
            stopSelf()
            return false
        }
        val now = System.currentTimeMillis()
        if (now >= f.endsAt) {
            stopSelf()
            return false
        }
        updateForeground(now)
        val pkg = currentPackage ?: return true
        if (now < f.snoozeUntil) return true
        if (pkg !in s.shieldedApps) return true
        if (pkg == lastShownFor && now - lastShownAt < 3_000) return true

        lastShownFor = pkg
        lastShownAt = now
        startActivity(
            Intent(this, ShieldActivity::class.java)
                .putExtra(ShieldActivity.EXTRA_PACKAGE, pkg)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
        return true
    }

    private fun updateForeground(now: Long) {
        val usm = getSystemService(UsageStatsManager::class.java) ?: return
        val events = try {
            usm.queryEvents(now - 10_000, now)
        } catch (e: Exception) {
            return
        }
        val e = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            @Suppress("DEPRECATION")
            if (e.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) currentPackage = e.packageName
        }
    }
}
