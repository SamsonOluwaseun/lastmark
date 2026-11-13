package com.samlytics.lastmark.ui

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.samlytics.lastmark.calendar.CalendarRepo
import com.samlytics.lastmark.data.Actions
import com.samlytics.lastmark.data.Store
import com.samlytics.lastmark.data.Strictness
import com.samlytics.lastmark.ui.theme.Brand

private const val STEPS = 6

@Composable
fun OnboardingScreen(store: Store) {
    val ctx = LocalContext.current
    val s by store.settings.collectAsState()
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf(s.name) }
    val firstTasks = remember { mutableStateListOf("", "", "") }
    var refresh by remember { mutableIntStateOf(0) }

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }
    val calendarPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        store.updateSettings { it.copy(showCalendar = granted) }
        refresh++
    }
    val notificationsOn = remember(refresh) { NotificationManagerCompat.from(ctx).areNotificationsEnabled() }
    val calendarOn = remember(refresh) { CalendarRepo.hasPermission(ctx) }

    BackHandler(enabled = step > 0) { step-- }

    fun finish() {
        store.updateSettings { it.copy(name = name.trim(), onboarded = true, planDate = Actions.todayKey()) }
        firstTasks.filter { it.isNotBlank() }.forEach { Actions.addTask(ctx, it, "", today = true) }
        Actions.applyReminders(ctx)
    }

    Column(Modifier.fillMaxSize().background(Brand.Cream).safeDrawingPadding()) {
        // Progress
        Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(STEPS) { i ->
                Box(
                    Modifier.weight(1f).height(4.dp)
                        .background(if (i <= step) Brand.Blue else Brand.Line, RoundedCornerShape(2.dp))
                )
            }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
        ) {
            when (step) {
                0 -> {
                    Logo()
                    Spacer(Modifier.height(20.dp))
                    Title("Welcome to Lastmark")
                    Hint("A planner that helps you pick up where you left off. Choose a few priorities, focus in blocks, and save your next step before every break.")
                    Spacer(Modifier.height(20.dp))
                    BrandField(name, { name = it }, "What should we call you? (optional)")
                    Spacer(Modifier.height(8.dp))
                    Hint("Everything you set up stays on this phone. You can change it all later in Settings.")
                }
                1 -> {
                    Title("Your focus rhythm")
                    Hint("How long can you usually focus before you need a break?")
                    SectionTitle("Focus block")
                    Choice(listOf(15, 25, 45, 60, 90), s.focusMinutes, { "$it min" }) { v -> store.updateSettings { it.copy(focusMinutes = v) } }
                    SectionTitle("Break")
                    Choice(listOf(5, 10, 15, 20), s.breakMinutes, { "$it min" }) { v -> store.updateSettings { it.copy(breakMinutes = v) } }
                    SectionTitle("Priorities per day")
                    Choice(listOf(1, 2, 3, 4, 5), s.maxToday, { "$it" }) { v -> store.updateSettings { it.copy(maxToday = v) } }
                    Spacer(Modifier.height(8.dp))
                    Hint("Fewer priorities usually means more gets done.")
                }
                2 -> {
                    Title("Reminders")
                    Hint("Lastmark can remind you to plan your day and to come back after a break.")
                    Spacer(Modifier.height(12.dp))
                    CardBox {
                        ToggleRow("Plan my day", "Every day at ${formatTime(s.planHour, s.planMinute)}", s.morningReminder) { v ->
                            store.updateSettings { it.copy(morningReminder = v) }
                        }
                        if (s.morningReminder) {
                            TextButton(onClick = {
                                TimePickerDialog(ctx, { _, h, m -> store.updateSettings { it.copy(planHour = h, planMinute = m) } }, s.planHour, s.planMinute, true).show()
                            }) { Text("Change time", color = Brand.Blue) }
                        }
                        ToggleRow("Break over", "Nudges you to get back to it", s.breakReminder) { v ->
                            store.updateSettings { it.copy(breakReminder = v) }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    if (notificationsOn) Hint("Notifications are on.", color = Brand.Blue)
                    else BlueButton("Allow notifications", {
                        if (Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else ctx.startActivity(appSettingsIntent(ctx.packageName))
                    })
                }
                3 -> {
                    Title("Your calendar (optional)")
                    Hint("See today's events next to your priorities. Lastmark reads calendars already synced to this phone, such as Google, Outlook or Samsung. No sign-in, and nothing leaves your phone.")
                    Spacer(Modifier.height(20.dp))
                    if (calendarOn) Hint("Calendar connected. You can choose which calendars to show in Settings.", color = Brand.Blue)
                    else BlueButton("Connect calendar", { calendarPermission.launch(Manifest.permission.READ_CALENDAR) })
                }
                4 -> {
                    Title("Drift shield")
                    Hint("If your phone pulls you away, Lastmark can step in when you open a distracting app during a focus block. How strict should it be?")
                    Spacer(Modifier.height(16.dp))
                    Strictness.entries.forEach { level ->
                        val on = s.strictness == level
                        Row(
                            Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                .background(if (on) Brand.BlueTint else Color.White, CardShape)
                                .clickable { store.updateSettings { it.copy(strictness = level) } }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CheckBox(on) { store.updateSettings { it.copy(strictness = level) } }
                            Spacer(Modifier.size(12.dp))
                            Column {
                                Text(level.label, style = MaterialTheme.typography.bodyLarge, color = Brand.Navy)
                                Hint(level.description)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Hint("You'll pick the apps and allow two phone permissions in Settings, under Drift shield.")
                }
                5 -> {
                    Title("What's on your plate today?")
                    Hint("Add up to 3 things. They'll be today's priorities. You can skip this and add tasks later.")
                    Spacer(Modifier.height(12.dp))
                    firstTasks.forEachIndexed { i, t ->
                        BrandField(t, { firstTasks[i] = it }, "Priority ${i + 1}")
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        Column(Modifier.padding(20.dp)) {
            if (step < STEPS - 1) LimeButton("Continue", { step++ }) else LimeButton("Start using Lastmark", { finish() })
            if (step > 0) {
                TextButton(onClick = { step-- }, modifier = Modifier.fillMaxWidth()) { Text("Back", color = Brand.Muted) }
            }
        }
    }
}

@Composable
private fun Title(text: String) {
    Text(text, style = MaterialTheme.typography.headlineSmall, color = Brand.Navy, modifier = Modifier.padding(bottom = 6.dp))
}

@Composable
private fun Logo() {
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(com.samlytics.lastmark.R.drawable.ic_logo),
        contentDescription = "Lastmark logo",
        modifier = Modifier.size(72.dp)
    )
}
