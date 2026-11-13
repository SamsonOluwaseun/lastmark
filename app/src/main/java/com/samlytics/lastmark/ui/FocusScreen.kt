package com.samlytics.lastmark.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samlytics.lastmark.data.Actions
import com.samlytics.lastmark.data.FocusState
import com.samlytics.lastmark.data.Phase
import com.samlytics.lastmark.data.Store
import com.samlytics.lastmark.data.Strictness
import com.samlytics.lastmark.data.Task
import com.samlytics.lastmark.shield.Shield
import com.samlytics.lastmark.ui.theme.Brand
import kotlinx.coroutines.delay

@Composable
fun FocusScreen(store: Store) {
    val ctx = LocalContext.current
    val f by store.focus.collectAsState()
    val tasks by store.tasks.collectAsState()
    val s by store.settings.collectAsState()
    val task = tasks.find { it.id == f.taskId }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var capturing by remember(f.phase) { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            Actions.checkTimers(ctx)
            delay(500)
        }
    }

    if (task == null) {
        Column(
            Modifier.fillMaxSize().background(Brand.Cream).safeDrawingPadding().padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("This task was removed", style = MaterialTheme.typography.headlineSmall, color = Brand.Navy)
            Spacer(Modifier.height(16.dp))
            LimeButton("Back to today", { Actions.stop(ctx) })
        }
        return
    }

    when {
        capturing -> BreakCapture(task, s.breakMinutes, onCancel = { capturing = false }) { next, mins ->
            Actions.startBreak(ctx, next, mins)
        }
        f.phase == Phase.FOCUS -> FocusRunning(
            task = task, f = f, now = now, totalMs = s.focusMinutes * 60_000L,
            shieldOn = Shield.isReady(ctx, s), strictness = s.strictness,
            onPause = { Actions.pause(ctx) }, onResume = { Actions.resume(ctx) },
            onBreak = { capturing = true },
            onDone = { Actions.finishBlock(ctx, done = true, nextStep = "") },
            onStop = { Actions.stop(ctx) }
        )
        f.phase == Phase.BREAK -> BreakRunning(
            task = task, remaining = f.endsAt - now,
            onResume = { Actions.startFocus(ctx, task.id) },
            onOther = { Actions.stop(ctx) }
        )
        f.phase == Phase.BLOCK_DONE -> DoneCheck(
            task = task, breakMinutes = s.breakMinutes,
            onDone = { Actions.finishBlock(ctx, done = true, nextStep = "") },
            onBreak = { next -> Actions.startBreak(ctx, next, s.breakMinutes) },
            onKeepGoing = { next -> Actions.saveNextStep(ctx, task.id, next); Actions.startFocus(ctx, task.id) },
            onStop = { next -> Actions.finishBlock(ctx, done = false, nextStep = next) }
        )
        else -> Unit
    }
}

@Composable
private fun FocusRunning(
    task: Task, f: FocusState, now: Long, totalMs: Long, shieldOn: Boolean, strictness: Strictness,
    onPause: () -> Unit, onResume: () -> Unit, onBreak: () -> Unit, onDone: () -> Unit, onStop: () -> Unit
) {
    val remaining = if (f.paused) f.pausedRemainingMs else (f.endsAt - now).coerceAtLeast(0)
    val progress = if (totalMs > 0) (remaining.toFloat() / totalMs).coerceIn(0f, 1f) else 0f

    Column(
        Modifier.fillMaxSize().background(Brand.Blue).safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.border(1.5.dp, Brand.Lime, RoundedCornerShape(50)).padding(horizontal = 14.dp, vertical = 5.dp)
        ) { Text(if (f.paused) "Paused" else "Focus block", style = MaterialTheme.typography.labelSmall, color = Brand.Lime) }
        Spacer(Modifier.height(20.dp))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
            val track = Color(0xFF6AA8F4)
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 14.dp.toPx()
                val inset = stroke / 2
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                drawArc(Brand.Lime, -90f, 360f * progress, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(formatClock(remaining), color = Brand.Cream, fontSize = 44.sp, style = MaterialTheme.typography.headlineSmall)
                Text("of ${totalMs / 60_000} min", color = Brand.Cream, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(task.title, style = MaterialTheme.typography.titleMedium, color = Brand.Cream, textAlign = TextAlign.Center)
        if (task.nextStep.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text("Next: ${task.nextStep}", style = MaterialTheme.typography.bodyMedium, color = Brand.Cream, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(16.dp))
        Box(
            Modifier.fillMaxWidth().background(Color(0xFF3A82D8), RoundedCornerShape(14.dp)).padding(12.dp)
        ) {
            Text(
                when {
                    strictness == Strictness.OFF -> "Drift shield is off. You can turn it on in Settings."
                    shieldOn -> "Drift shield is on (${strictness.label})"
                    else -> "Drift shield isn't set up yet. Finish setup in Settings."
                },
                style = MaterialTheme.typography.bodyMedium, color = Brand.Cream
            )
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onBreak, text = "Take a break", filled = true)
        Spacer(Modifier.height(10.dp))
        OutlineButton(if (f.paused) "Resume" else "Pause", if (f.paused) onResume else onPause, color = Brand.Cream)
        Spacer(Modifier.height(10.dp))
        OutlineButton("Mark task done", onDone, color = Brand.Cream)
        TextButton(onClick = onStop) { Text("Stop focus", color = Brand.Cream) }
    }
}

@Composable
private fun Button(onClick: () -> Unit, text: String, filled: Boolean) {
    androidx.compose.material3.Button(
        onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(50),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = if (filled) Brand.Cream else Color.Transparent, contentColor = Brand.Navy
        )
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

@Composable
private fun BreakCapture(task: Task, defaultMinutes: Int, onCancel: () -> Unit, onStart: (String, Int) -> Unit) {
    var next by remember { mutableStateOf(task.nextStep) }
    var minutes by remember { mutableIntStateOf(defaultMinutes) }
    val options = (listOf(5, 10, 15) + defaultMinutes).distinct().sorted()

    Column(
        Modifier.fillMaxSize().background(Brand.Cream).safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        Text("Before you go", style = MaterialTheme.typography.headlineSmall, color = Brand.Navy)
        Hint("What's your next step? You'll see this first when you're back.")
        Spacer(Modifier.height(16.dp))
        BrandField(next, { next = it }, "Next step", singleLine = false, placeholder = "Finish the second paragraph")
        SectionTitle("Break length")
        Choice(options, minutes, { "$it min" }) { minutes = it }
        Spacer(Modifier.height(28.dp))
        BlueButton("Save and start break", { onStart(next, minutes) })
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Back to focus", color = Brand.Muted) }
    }
}

@Composable
private fun BreakRunning(task: Task, remaining: Long, onResume: () -> Unit, onOther: () -> Unit) {
    val over = remaining <= 0
    Column(
        Modifier.fillMaxSize().background(if (over) Brand.Navy else Brand.Cream).safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        val text = if (over) Brand.Cream else Brand.Navy
        Text(if (over) "Break's over" else "On a break", style = MaterialTheme.typography.headlineSmall, color = text)
        Spacer(Modifier.height(8.dp))
        if (!over) Text(formatClock(remaining), fontSize = 44.sp, color = Brand.Blue, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(20.dp))
        Column(
            Modifier.fillMaxWidth().background(if (over) Color(0xFF0D3341) else androidx.compose.ui.graphics.Color.White, RoundedCornerShape(16.dp)).padding(16.dp)
        ) {
            Text("Pick up here", style = MaterialTheme.typography.labelSmall, color = if (over) Brand.Lime else Brand.Blue)
            Spacer(Modifier.height(4.dp))
            Text(task.nextStep.ifBlank { task.title }, style = MaterialTheme.typography.bodyLarge, color = text)
            if (task.nextStep.isNotBlank()) Text(task.title, style = MaterialTheme.typography.bodyMedium, color = if (over) Brand.Cream.copy(alpha = 0.7f) else Brand.Muted)
        }
        Spacer(Modifier.height(24.dp))
        LimeButton(if (over) "Resume focus" else "End break and resume", onResume)
        Spacer(Modifier.height(10.dp))
        OutlineButton("Choose another task", onOther, color = if (over) Brand.Cream else Brand.Navy)
    }
}

@Composable
private fun DoneCheck(
    task: Task, breakMinutes: Int,
    onDone: () -> Unit, onBreak: (String) -> Unit, onKeepGoing: (String) -> Unit, onStop: (String) -> Unit
) {
    var next by remember { mutableStateOf(task.nextStep) }
    Column(
        Modifier.fillMaxSize().background(Brand.Cream).safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        Text("Block finished", style = MaterialTheme.typography.headlineSmall, color = Brand.Navy)
        Hint("Did you finish \"${task.title}\"?")
        Spacer(Modifier.height(16.dp))
        LimeButton("Yes, it's done", onDone)
        SectionTitle("Not yet? Save your place")
        BrandField(next, { next = it }, "Next step", singleLine = false)
        Spacer(Modifier.height(16.dp))
        BlueButton("Save and take a $breakMinutes min break", { onBreak(next) })
        Spacer(Modifier.height(10.dp))
        OutlineButton("Save and keep going", { onKeepGoing(next) })
        TextButton(onClick = { onStop(next) }, modifier = Modifier.fillMaxWidth()) { Text("Save and stop for now", color = Brand.Muted) }
    }
}
