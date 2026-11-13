package com.samlytics.lastmark.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.samlytics.lastmark.data.Task
import com.samlytics.lastmark.ui.theme.Brand

val CardShape = RoundedCornerShape(14.dp)

@Composable
fun LimeButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier.fillMaxWidth(), enabled: Boolean = true) {
    Button(
        onClick = onClick, modifier = modifier, enabled = enabled, shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(containerColor = Brand.Lime, contentColor = Brand.Navy)
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun BlueButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier.fillMaxWidth(), enabled: Boolean = true) {
    Button(
        onClick = onClick, modifier = modifier, enabled = enabled, shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(containerColor = Brand.Blue, contentColor = Brand.Cream)
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun OutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier.fillMaxWidth(), color: Color = Brand.Navy) {
    OutlinedButton(
        onClick = onClick, modifier = modifier, shape = RoundedCornerShape(50),
        border = BorderStroke(1.5.dp, color),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color)
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = Brand.Navy, modifier = modifier.padding(top = 20.dp, bottom = 8.dp))
}

@Composable
fun Hint(text: String, modifier: Modifier = Modifier, color: Color = Brand.Muted) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = color, modifier = modifier)
}

@Composable
fun CardBox(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White, CardShape)
            .border(1.dp, Brand.Line, CardShape)
            .padding(14.dp),
        content = content
    )
}

@Composable
fun BrandField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, singleLine: Boolean = true, placeholder: String = "") {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) },
        placeholder = { if (placeholder.isNotEmpty()) Text(placeholder) },
        singleLine = singleLine, minLines = if (singleLine) 1 else 2,
        modifier = modifier.fillMaxWidth(), shape = CardShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Brand.Blue, focusedLabelColor = Brand.Blue, cursorColor = Brand.Blue,
            unfocusedContainerColor = Color.White, focusedContainerColor = Color.White
        )
    )
}

@Composable
fun ToggleRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = Brand.Navy)
            if (subtitle != null) Hint(subtitle)
        }
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Brand.Blue, checkedThumbColor = Brand.Cream)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> Choice(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o ->
            val on = o == selected
            Box(
                modifier = Modifier
                    .background(if (on) Brand.BlueTint else Color.White, RoundedCornerShape(10.dp))
                    .border(1.5.dp, if (on) Brand.Blue else Brand.Line, RoundedCornerShape(10.dp))
                    .clickable { onSelect(o) }
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Text(label(o), style = if (on) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium, color = Brand.Navy)
            }
        }
    }
}

@Composable
fun CheckBox(checked: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .background(if (checked) Brand.Blue else Color.Transparent, RoundedCornerShape(6.dp))
            .border(2.dp, Brand.Blue, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (checked) Icon(Icons.Filled.Check, contentDescription = "Done", tint = Brand.Cream, modifier = Modifier.size(16.dp))
    }
}

@Composable
fun TaskRow(
    task: Task,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onStart: (() -> Unit)? = null,
    onStar: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .background(Color.White, CardShape)
            .border(1.dp, Brand.Line, CardShape)
            .clickable(onClick = onClick)
            .padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CheckBox(task.done, onToggle)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text(
                task.title, style = MaterialTheme.typography.bodyLarge,
                color = if (task.done) Brand.Muted else Brand.Navy,
                textDecoration = if (task.done) TextDecoration.LineThrough else null,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            if (task.nextStep.isNotBlank() && !task.done) {
                Text("Next: ${task.nextStep}", style = MaterialTheme.typography.bodyMedium, color = Brand.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (onStar != null) {
            IconButton(onClick = onStar) {
                Icon(Icons.Filled.Star, contentDescription = if (task.today) "Remove from today" else "Add to today", tint = if (task.today) Brand.LimeDark else Brand.Line)
            }
        }
        if (onStart != null && !task.done) {
            IconButton(onClick = onStart) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Start focus", tint = Brand.Blue)
            }
        }
    }
}

fun formatClock(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

fun formatTime(hour: Int, minute: Int): String = "%02d:%02d".format(hour, minute)
