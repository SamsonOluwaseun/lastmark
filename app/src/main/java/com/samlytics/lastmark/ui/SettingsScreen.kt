package com.samlytics.lastmark.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.samlytics.lastmark.BuildConfig
import com.samlytics.lastmark.calendar.CalendarInfo
import com.samlytics.lastmark.calendar.CalendarRepo
import com.samlytics.lastmark.data.Actions
import com.samlytics.lastmark.data.Store
import com.samlytics.lastmark.data.Strictness
import com.samlytics.lastmark.reminders.Alarms
import com.samlytics.lastmark.shield.InstalledApp
import com.samlytics.lastmark.shield.Shield
import com.samlytics.lastmark.ui.theme.Brand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(store: Store) {
    val ctx = LocalContext.current
    val s by store.settings.collectAsState()
    var refresh by remember { mutableIntStateOf(0) }
    var name by remember(s.name) { mutableStateOf(s.name) }
    var showApps by remember { mutableStateOf(false) }
    var showCalendars by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }
    val calendarPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) store.updateSettings { it.copy(showCalendar = true) }
        refresh++
    }

    // Read permission states (re-read when refresh changes)
    val notificationsOn = remember(refresh) { NotificationManagerCompat.from(ctx).areNotificationsEnabled() }
    val usageOn = remember(refresh) { Shield.hasUsageAccess(ctx) }
    val overlayOn = remember(refresh) { Shield.canOverlay(ctx) }
    val calendarOn = remember(refresh) { CalendarRepo.hasPermission(ctx) }
    val exactOn = remember(refresh) { Alarms.canExact(ctx) }

    fun update(change: (com.samlytics.lastmark.data.AppSettings) -> com.samlytics.lastmark.data.AppSettings) {
        store.updateSettings(change)
        Actions.applyReminders(ctx)
    }

    Column(
        Modifier.fillMaxSize().background(Brand.Cream).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, color = Brand.Navy)
        Hint("Everything is stored on this phone only.")

        SectionTitle("You")
        BrandField(name, { name = it; store.updateSettings { st -> st.copy(name = it.trim()) } }, "Your name (optional)")

        SectionTitle("Focus rhythm")
        CardBox {
            NumberSlider("Focus block", s.focusMinutes, 10f..90f, 15, "min") { v -> update { it.copy(focusMinutes = v) } }
            NumberSlider("Break", s.breakMinutes, 3f..30f, 26, "min") { v -> update { it.copy(breakMinutes = v) } }
            NumberSlider("Priorities per day", s.maxToday, 1f..5f, 3, "") { v -> update { it.copy(maxToday = v) } }
        }

        SectionTitle("Reminders")
        CardBox {
            if (!notificationsOn) {
                Hint("Notifications are off, so reminders won't show.", color = Brand.Navy)
                Spacer(Modifier.height(8.dp))
                BlueButton("Turn on notifications", {
                    if (Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else ctx.startActivity(appSettingsIntent(ctx.packageName))
                })
                Spacer(Modifier.height(8.dp))
            }
            ToggleRow("Plan my day reminder", "Every day at ${formatTime(s.planHour, s.planMinute)}", s.morningReminder) { v ->
                update { it.copy(morningReminder = v) }
            }
            if (s.morningReminder) {
                Text(
                    "Change time", style = MaterialTheme.typography.labelLarge, color = Brand.Blue,
                    modifier = Modifier.clickable {
                        TimePickerDialog(ctx, { _, h, m -> update { it.copy(planHour = h, planMinute = m) } }, s.planHour, s.planMinute, true).show()
                    }.padding(vertical = 6.dp)
                )
            }
            ToggleRow("Break over reminder", "Tells you when it's time to get back to it", s.breakReminder) { v ->
                update { it.copy(breakReminder = v) }
            }
            if (s.breakReminder) {
                Spacer(Modifier.height(4.dp))
                Hint("If I haven't come back, remind me again every")
                Spacer(Modifier.height(8.dp))
                Choice(listOf(0, 5, 10, 15, 30), s.nudgeMinutes, { if (it == 0) "Never" else "$it min" }) { v ->
                    update { it.copy(nudgeMinutes = v) }
                }
            }
            if (!exactOn) {
                Spacer(Modifier.height(10.dp))
                Hint("Reminders may arrive a few minutes late. Allow exact alarms for on-time reminders.")
                Text(
                    "Allow exact alarms", style = MaterialTheme.typography.labelLarge, color = Brand.Blue,
                    modifier = Modifier.clickable {
                        if (Build.VERSION.SDK_INT >= 31) {
                            ctx.startActivity(
                                Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }.padding(vertical = 6.dp)
                )
            }
        }

        SectionTitle("Calendar")
        CardBox {
            Hint("Shows today's events from calendars synced to this phone, such as Google or Outlook. Nothing leaves your phone.")
            Spacer(Modifier.height(6.dp))
            ToggleRow("Show calendar on Today", null, s.showCalendar && calendarOn) { v ->
                if (v && !calendarOn) calendarPermission.launch(Manifest.permission.READ_CALENDAR)
                else store.updateSettings { it.copy(showCalendar = v) }
            }
            if (s.showCalendar && calendarOn) {
                Text(
                    if (s.calendarIds.isEmpty()) "Showing all calendars. Choose calendars" else "Showing ${s.calendarIds.size} calendar(s). Change",
                    style = MaterialTheme.typography.labelLarge, color = Brand.Blue,
                    modifier = Modifier.clickable { showCalendars = true }.padding(vertical = 6.dp)
                )
            }
        }

        SectionTitle("Drift shield")
        CardBox {
            Hint("When you open a distracting app during a focus block, Lastmark shows your task and next step on top of it.")
            Spacer(Modifier.height(10.dp))
            Choice(Strictness.entries, s.strictness, { it.label }) { v -> store.updateSettings { it.copy(strictness = v) } }
            Spacer(Modifier.height(6.dp))
            Hint(s.strictness.description)
            if (s.strictness != Strictness.OFF) {
                Spacer(Modifier.height(12.dp))
                PermissionRow("Usage access", "Lets Lastmark see which app is open", usageOn) { ctx.startActivity(Shield.usageAccessIntent()) }
                PermissionRow("Display over other apps", "Lets Lastmark show the shield", overlayOn) { ctx.startActivity(Shield.overlayIntent(ctx)) }
                Spacer(Modifier.height(8.dp))
                OutlineButton(
                    if (s.shieldedApps.isEmpty()) "Choose distracting apps" else "Distracting apps (${s.shieldedApps.size})",
                    { showApps = true }
                )
                if (s.shieldedApps.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Hint(s.shieldedApps.take(6).joinToString(", ") { Shield.labelFor(ctx, it) } + if (s.shieldedApps.size > 6) "…" else "")
                }
                Spacer(Modifier.height(6.dp))
                Hint(
                    if (Shield.isReady(ctx, s)) "Ready. The shield starts with your next focus block."
                    else "Finish the steps above to turn the shield on.",
                    color = if (Shield.isReady(ctx, s)) Brand.Blue else Brand.Muted
                )
            }
        }

        SectionTitle("Data")
        CardBox {
            Text("Clear completed tasks", style = MaterialTheme.typography.bodyLarge, color = Brand.Navy,
                modifier = Modifier.fillMaxWidth().clickable { Actions.clearCompleted(ctx) }.padding(vertical = 8.dp))
            Text("Run setup again", style = MaterialTheme.typography.bodyLarge, color = Brand.Navy,
                modifier = Modifier.fillMaxWidth().clickable { store.updateSettings { it.copy(onboarded = false) } }.padding(vertical = 8.dp))
            Text("Reset Lastmark", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth().clickable { confirmReset = true }.padding(vertical = 8.dp))
        }

        SectionTitle("About")
        Hint("Lastmark ${BuildConfig.VERSION_NAME}. A free, open-source community project by Samlytics.")
        Text(
            "View the project on GitHub", style = MaterialTheme.typography.labelLarge, color = Brand.Blue,
            modifier = Modifier.clickable {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/SamsonOluwaseun/lastmark")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }.padding(vertical = 8.dp)
        )
        Spacer(Modifier.height(32.dp))
    }

    if (showApps) AppPickerDialog(s.shieldedApps, onDismiss = { showApps = false }) { picked ->
        store.updateSettings { it.copy(shieldedApps = picked) }
        showApps = false
    }
    if (showCalendars) CalendarPickerDialog(s.calendarIds, onDismiss = { showCalendars = false }) { picked ->
        store.updateSettings { it.copy(calendarIds = picked) }
        showCalendars = false
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = Brand.Cream,
            title = { Text("Reset Lastmark?", color = Brand.Navy) },
            text = { Text("This deletes all tasks and settings on this phone. It can't be undone.", color = Brand.Navy) },
            confirmButton = {
                TextButton(onClick = { confirmReset = false; Actions.resetAll(ctx) }) {
                    Text("Reset", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel", color = Brand.Muted) } }
        )
    }
}

fun appSettingsIntent(pkg: String): Intent =
    Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

@Composable
private fun NumberSlider(label: String, value: Int, range: ClosedFloatingPointRange<Float>, steps: Int, unit: String, onDone: (Int) -> Unit) {
    var v by remember(value) { mutableStateOf(value.toFloat()) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = Brand.Navy, modifier = Modifier.weight(1f))
        Text("${v.toInt()}${if (unit.isNotEmpty()) " $unit" else ""}", style = MaterialTheme.typography.labelLarge, color = Brand.Blue)
    }
    Slider(
        value = v, onValueChange = { v = it }, onValueChangeFinished = { onDone(v.toInt()) },
        valueRange = range, steps = steps,
        colors = SliderDefaults.colors(thumbColor = Brand.Blue, activeTrackColor = Brand.Blue)
    )
}

@Composable
private fun PermissionRow(title: String, subtitle: String, granted: Boolean, onGrant: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = Brand.Navy)
            Hint(subtitle)
        }
        Spacer(Modifier.width(8.dp))
        if (granted) Text("Allowed", style = MaterialTheme.typography.labelLarge, color = Brand.Blue)
        else TextButton(onClick = onGrant) { Text("Allow", color = Brand.Blue) }
    }
}

@Composable
fun AppPickerDialog(current: Set<String>, onDismiss: () -> Unit, onSave: (Set<String>) -> Unit) {
    val ctx = LocalContext.current
    var apps by remember { mutableStateOf<List<InstalledApp>?>(null) }
    val picked = remember { mutableStateListOf<String>().apply { addAll(current) } }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { apps = withContext(Dispatchers.IO) { Shield.installedApps(ctx) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Brand.Cream,
        title = { Text("Distracting apps", color = Brand.Navy) },
        text = {
            Column {
                BrandField(query, { query = it }, "Search apps")
                Spacer(Modifier.height(8.dp))
                val list = apps
                if (list == null) Hint("Loading apps…") else {
                    val shown = list.filter { query.isBlank() || it.label.contains(query, ignoreCase = true) }
                    LazyColumn(Modifier.heightIn(max = 380.dp)) {
                        items(shown, key = { it.packageName }) { app ->
                            val on = app.packageName in picked
                            Row(
                                Modifier.fillMaxWidth().clickable { if (on) picked.remove(app.packageName) else picked.add(app.packageName) }.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CheckBox(on) { if (on) picked.remove(app.packageName) else picked.add(app.packageName) }
                                Spacer(Modifier.width(12.dp))
                                Text(app.label, style = MaterialTheme.typography.bodyLarge, color = Brand.Navy)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(picked.toSet()) }) { Text("Save", color = Brand.Blue) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Brand.Muted) } }
    )
}

@Composable
private fun CalendarPickerDialog(current: Set<Long>, onDismiss: () -> Unit, onSave: (Set<Long>) -> Unit) {
    val ctx = LocalContext.current
    var calendars by remember { mutableStateOf<List<CalendarInfo>>(emptyList()) }
    val picked = remember { mutableStateListOf<Long>().apply { addAll(current) } }
    LaunchedEffect(Unit) { calendars = withContext(Dispatchers.IO) { CalendarRepo.calendars(ctx) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Brand.Cream,
        title = { Text("Calendars", color = Brand.Navy) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                Hint("Leave all unticked to show every calendar.")
                Spacer(Modifier.height(8.dp))
                if (calendars.isEmpty()) Hint("No calendars found on this phone.")
                calendars.forEach { c ->
                    val on = c.id in picked
                    Row(
                        Modifier.fillMaxWidth().clickable { if (on) picked.remove(c.id) else picked.add(c.id) }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CheckBox(on) { if (on) picked.remove(c.id) else picked.add(c.id) }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(c.name, style = MaterialTheme.typography.bodyLarge, color = Brand.Navy)
                            if (c.account.isNotBlank()) Hint(c.account)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(picked.toSet()) }) { Text("Save", color = Brand.Blue) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Brand.Muted) } }
    )
}
