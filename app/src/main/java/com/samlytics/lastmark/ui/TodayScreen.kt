package com.samlytics.lastmark.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.samlytics.lastmark.calendar.CalendarEvent
import com.samlytics.lastmark.calendar.CalendarRepo
import com.samlytics.lastmark.data.Actions
import com.samlytics.lastmark.data.Store
import com.samlytics.lastmark.data.Task
import com.samlytics.lastmark.ui.theme.Brand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** The task to pick up: a today task with a saved next step first, then the latest today task. */
fun taskToResume(tasks: List<Task>): Task? {
    val open = tasks.filter { !it.done }
    return open.filter { it.today && it.nextStep.isNotBlank() }.maxByOrNull { it.updatedAt }
        ?: open.filter { it.today }.minByOrNull { it.todayOrder }
        ?: open.filter { it.nextStep.isNotBlank() }.maxByOrNull { it.updatedAt }
}

@Composable
fun TodayScreen(store: Store, onOpenTasks: () -> Unit, onOpenSettings: () -> Unit) {
    val ctx = LocalContext.current
    val tasks by store.tasks.collectAsState()
    val s by store.settings.collectAsState()
    var showPlan by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Task?>(null) }
    var events by remember { mutableStateOf(emptyList<CalendarEvent>()) }
    var refresh by remember { mutableIntStateOf(0) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }
    LaunchedEffect(s.showCalendar, s.calendarIds, refresh) {
        events = if (s.showCalendar) withContext(Dispatchers.IO) { CalendarRepo.todayEvents(ctx, s.calendarIds) } else emptyList()
    }
    val calendarPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }

    val todayTasks = tasks.filter { it.today }.sortedWith(compareBy({ it.done }, { it.todayOrder }))
    val resume = taskToResume(tasks)
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Morning"
        hour < 18 -> "Afternoon"
        else -> "Evening"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brand.Cream)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Hint(SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(Date()))
        Text(
            if (s.name.isBlank()) greeting else "$greeting, ${s.name}",
            style = MaterialTheme.typography.headlineSmall, color = Brand.Navy
        )
        Spacer(Modifier.height(16.dp))

        if (resume != null) {
            PickUpCard(resume, onResume = { Actions.startFocus(ctx, resume.id) })
        } else {
            EmptyCard(
                hasTasks = tasks.any { !it.done },
                onPlan = { showPlan = true },
                onAdd = { editing = Task(id = -1, title = "") }
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("Today's ${s.maxToday}", Modifier.weight(1f))
            TextButton(onClick = { showPlan = true }) { Text("Plan today", color = Brand.Blue) }
        }
        if (todayTasks.isEmpty()) {
            Hint("No priorities yet. Tap Plan today to choose up to ${s.maxToday}.")
        }
        todayTasks.forEach { t ->
            TaskRow(
                task = t,
                onToggle = { Actions.toggleDone(ctx, t.id) },
                onClick = { editing = t },
                onStart = { Actions.startFocus(ctx, t.id) }
            )
        }

        if (s.showCalendar) {
            SectionTitle("Calendar")
            if (!CalendarRepo.hasPermission(ctx)) {
                CardBox {
                    Hint("Lastmark needs permission to read the calendars on this phone.")
                    Spacer(Modifier.height(10.dp))
                    BlueButton("Allow calendar access", { calendarPermission.launch(Manifest.permission.READ_CALENDAR) })
                }
            } else if (events.isEmpty()) {
                Hint("Nothing in your calendar today.")
            } else {
                val timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT)
                events.forEach { e ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .background(Brand.BlueTint, CardShape)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(10.dp).background(Color(e.color).copy(alpha = 1f), CircleShape))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (e.allDay) "All day" else "${timeFormat.format(Date(e.begin))} to ${timeFormat.format(Date(e.end))}",
                            style = MaterialTheme.typography.labelLarge, color = Brand.Blue
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(e.title, style = MaterialTheme.typography.bodyMedium, color = Brand.Navy)
                    }
                }
            }
        } else if (tasks.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                "Show your calendar here",
                style = MaterialTheme.typography.bodyMedium, color = Brand.Blue,
                modifier = Modifier.clickable { onOpenSettings() }.padding(vertical = 8.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showPlan) {
        PlanDialog(store, onDismiss = { showPlan = false })
    }
    editing?.let { t ->
        TaskDialog(
            initial = if (t.id == -1L) null else t,
            defaultToday = true,
            onDismiss = { editing = null },
            onSave = { title, next, today ->
                if (t.id == -1L) Actions.addTask(ctx, title, next, today)
                else Actions.saveTask(ctx, t.copy(title = title, nextStep = next))
                editing = null
            },
            onDelete = if (t.id == -1L) null else ({ Actions.deleteTask(ctx, t.id); editing = null })
        )
    }
}

@Composable
private fun PickUpCard(task: Task, onResume: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brand.Navy, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Text("Pick up here", style = MaterialTheme.typography.labelSmall, color = Brand.Lime)
        Spacer(Modifier.height(6.dp))
        Text(
            task.nextStep.ifBlank { task.title },
            style = MaterialTheme.typography.bodyLarge, color = Brand.Cream
        )
        if (task.nextStep.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(task.title, style = MaterialTheme.typography.bodyMedium, color = Brand.Cream.copy(alpha = 0.7f))
        }
        Spacer(Modifier.height(14.dp))
        LimeButton("Resume focus", onResume)
    }
}

@Composable
private fun EmptyCard(hasTasks: Boolean, onPlan: () -> Unit, onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brand.Navy, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Text("Start here", style = MaterialTheme.typography.labelSmall, color = Brand.Lime)
        Spacer(Modifier.height(6.dp))
        Text(
            if (hasTasks) "Choose what matters today" else "Add your first task",
            style = MaterialTheme.typography.bodyLarge, color = Brand.Cream
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (hasTasks) "Pick your priorities and Lastmark will keep your place." else "Then pick it as a priority and start a focus block.",
            style = MaterialTheme.typography.bodyMedium, color = Brand.Cream.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(14.dp))
        if (hasTasks) LimeButton("Plan today", onPlan) else LimeButton("Add a task", onAdd)
    }
}

@Composable
fun PlanDialog(store: Store, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val tasks by store.tasks.collectAsState()
    val s by store.settings.collectAsState()
    val open = tasks.filter { !it.done }
    val selected = remember {
        mutableStateListOf<Long>().apply { addAll(tasks.filter { it.today && !it.done }.sortedBy { it.todayOrder }.map { it.id }) }
    }
    var newTitle by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Brand.Cream,
        title = { Text("Plan today", color = Brand.Navy) },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                Hint("Pick up to ${s.maxToday}. Order follows the order you tap them.")
                Spacer(Modifier.height(12.dp))
                if (open.isEmpty()) Hint("No open tasks yet. Add one below.")
                open.forEach { t ->
                    val index = selected.indexOf(t.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (index >= 0) selected.remove(t.id)
                                else if (selected.size < s.maxToday) selected.add(t.id)
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CheckBox(index >= 0) {
                            if (index >= 0) selected.remove(t.id) else if (selected.size < s.maxToday) selected.add(t.id)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(t.title, style = MaterialTheme.typography.bodyLarge, color = Brand.Navy, modifier = Modifier.weight(1f))
                        if (index >= 0) Text("${index + 1}", style = MaterialTheme.typography.labelLarge, color = Brand.Blue)
                    }
                }
                Spacer(Modifier.height(12.dp))
                BrandField(newTitle, { newTitle = it }, "New task")
                Spacer(Modifier.height(8.dp))
                OutlineButton("Add task", {
                    if (newTitle.isNotBlank()) {
                        Actions.addTask(ctx, newTitle, "", today = false)
                        val added = store.tasks.value.lastOrNull()
                        if (added != null && selected.size < s.maxToday) selected.add(added.id)
                        newTitle = ""
                    }
                })
            }
        },
        confirmButton = {
            TextButton(onClick = { Actions.setToday(ctx, selected.toList()); onDismiss() }) { Text("Save plan", color = Brand.Blue) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Brand.Muted) } }
    )
}
