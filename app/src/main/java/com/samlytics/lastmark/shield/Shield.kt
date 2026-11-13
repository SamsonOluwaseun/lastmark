package com.samlytics.lastmark.shield

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import androidx.core.content.ContextCompat
import com.samlytics.lastmark.data.AppSettings
import com.samlytics.lastmark.data.Store
import com.samlytics.lastmark.data.Strictness

data class InstalledApp(val packageName: String, val label: String)

object Shield {

    fun hasUsageAccess(ctx: Context): Boolean {
        val ops = ctx.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= 29) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun canOverlay(ctx: Context): Boolean = android.provider.Settings.canDrawOverlays(ctx)

    fun usageAccessIntent(): Intent =
        Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun overlayIntent(ctx: Context): Intent =
        Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun isReady(ctx: Context, s: AppSettings): Boolean =
        s.strictness != Strictness.OFF && s.shieldedApps.isNotEmpty() && hasUsageAccess(ctx) && canOverlay(ctx)

    fun startIfReady(ctx: Context) {
        if (!isReady(ctx, Store.get(ctx).settings.value)) return
        try {
            ContextCompat.startForegroundService(ctx, Intent(ctx, ShieldService::class.java))
        } catch (e: Exception) {
            // Android may refuse if the app is in the background. The shield will start next time.
        }
    }

    fun stop(ctx: Context) {
        ctx.stopService(Intent(ctx, ShieldService::class.java))
    }

    fun installedApps(ctx: Context): List<InstalledApp> {
        val pm = ctx.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        return pm.queryIntentActivities(launcher, 0)
            .map { InstalledApp(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .filter { it.packageName != ctx.packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    fun labelFor(ctx: Context, pkg: String): String = try {
        val pm = ctx.packageManager
        @Suppress("DEPRECATION")
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) {
        pkg
    }
}
