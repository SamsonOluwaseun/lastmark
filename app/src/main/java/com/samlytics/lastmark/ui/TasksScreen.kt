package com.samlytics.lastmark.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.samlytics.lastmark.data.Actions
import com.samlytics.lastmark.data.Store
import com.samlytics.lastmark.data.Task
import com.samlytics.lastmark.ui.theme.Brand

@Composable
fun TasksScreen(store: Store) {
    val ctx = LocalContext.current
    val tasks by store.tasks.collectAsState()
    val s by store.settings.collectAsState()
    var editing by remember { mutableStateOf<Task?>(null) }
    var showDone by remember { mutableStateOf(false) }

    val open = tasks.filter { !it.done }.sortedWith(compareBy({ !it.today }, { it.todayOrder }, { -it.updatedAt }))
    val done = tasks.filter { it.done }.sortedByDescending { it.doneAt }

    Box(Modifier.fillMaxSize().background(Brand.Cream)) {
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp)) {
            item {
                Text("Tasks", style = MaterialTheme.typography.headlineSmall, color = Brand.Navy)
                Hint("Star up to ${s.maxToday} for today. Tap a task to edit it.")
                Spacer(Modifier.height(12.dp))
                if (open.isEmpty()) {
                    CardBox {
                        Text("Nothing on your list", style = MaterialTheme.typography.bodyLarge, color = Brand.Navy)
                        Hint("Add the things on your mind. You don't have to sort them yet.")
                    }
                }
            }
            items(open, key = { it.id }) { t ->
                TaskRow(
                    task = t,
                    onToggle = { Actions.toggleDone(ctx, t.id) },
                    onClick = { editing = t },
                    onStart = { Actions.startFocus(ctx, t.id) },
                    onStar = {
                        if (!Actions.toggleToday(ctx, t.id)) {
                            Toast.makeText(ctx, "You already have ${s.maxToday} priorities today", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
            if (done.isNotEmpty()) {
                item {
                    Row(
                        Modifier.clickable { showDone = !showDone }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            (if (showDone) "Hide" else "Show") + " completed (${done.size})",
                            style = MaterialTheme.typography.labelLarge, color = Brand.Blue
                        )
                        Spacer(Modifier.width(16.dp))
                        if (showDone) {
                            Text(
                                "Clear completed", style = MaterialTheme.typography.labelLarge, color = Brand.Muted,
                                modifier = Modifier.clickable { Actions.clearCompleted(ctx) }
                            )
                        }
                    }
                }
                if (showDone) {
                    items(done, key = { it.id }) { t ->
                        TaskRow(task = t, onToggle = { Actions.toggleDone(ctx, t.id) }, onClick = { editing = t })
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { editing = Task(id = -1, title = "") },
            containerColor = Brand.Lime,
            contentColor = Brand.Navy,
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Add task") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
        )
    }

    editing?.let { t ->
        TaskDialog(
            initial = if (t.id == -1L) null else t,
            defaultToday = false,
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
fun TaskDialog(
    initial: Task?,
    defaultToday: Boolean,
    onDismiss: () -> Unit,
    onSave: (title: String, nextStep: String, today: Boolean) -> Unit,
    onDelete: (() -> Unit)?
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var next by remember { mutableStateOf(initial?.nextStep ?: "") }
    var today by remember { mutableStateOf(defaultToday) }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Brand.Cream,
        title = { Text(if (initial == null) "New task" else "Edit task", color = Brand.Navy) },
        text = {
            Column {
                BrandField(title, { title = it; error = false }, "Task", placeholder = "Write the project proposal")
                if (error) Text("Give the task a name first", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))
                BrandField(next, { next = it }, "First or next step (optional)", singleLine = false, placeholder = "Open the template and fill in the summary")
                if (initial == null) {
                    Spacer(Modifier.height(4.dp))
                    ToggleRow("Add to today", null, today) { today = it }
                }
                if (onDelete != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Delete task", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.clickable { onDelete() }.padding(vertical = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (title.isBlank()) error = true else onSave(title, next, today) }) {
                Text("Save", color = Brand.Blue)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Brand.Muted) } }
    )
}
