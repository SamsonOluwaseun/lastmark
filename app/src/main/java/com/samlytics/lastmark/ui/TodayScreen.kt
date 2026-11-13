package com.samlytics.lastmark.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samlytics.lastmark.ui.theme.Brand
import com.samlytics.lastmark.ui.theme.LastmarkTheme

data class Priority(val title: String, val done: Boolean = false)

@Composable
fun TodayScreen() {
    val priorities = remember {
        mutableStateListOf(
            Priority("Review governance policy", done = true),
            Priority("Client data model"),
            Priority("Clink release notes")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brand.Cream)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text("Fri 13 Nov", style = MaterialTheme.typography.bodyMedium, color = Brand.Muted)
        Text("Morning, Oluwaseun", style = MaterialTheme.typography.headlineSmall)

        Spacer(Modifier.height(16.dp))
        PickUpCard(
            nextStep = "Finish the customer entity, then link it to orders",
            context = "Client data model · left off 25 min ago"
        )

        Spacer(Modifier.height(20.dp))
        Text("Today's 3", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        priorities.forEachIndexed { i, p ->
            PriorityRow(p) { priorities[i] = p.copy(done = !p.done) }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(12.dp))
        Text("From Google Calendar", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        CalendarRow("10:00", "Stand-up · 15 min")
    }
}

@Composable
private fun PickUpCard(nextStep: String, context: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brand.Navy, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Text("Pick up here", style = MaterialTheme.typography.labelSmall, color = Brand.Lime)
        Spacer(Modifier.height(6.dp))
        Text(nextStep, style = MaterialTheme.typography.bodyLarge, color = Brand.Cream)
        Spacer(Modifier.height(4.dp))
        Text(context, style = MaterialTheme.typography.bodyMedium, color = Brand.Cream.copy(alpha = 0.7f))
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = { },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = Brand.Lime, contentColor = Brand.Navy)
        ) {
            Text("Resume focus", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun PriorityRow(p: Priority, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(androidx.compose.ui.graphics.Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, Brand.Line, RoundedCornerShape(14.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(if (p.done) Brand.Blue else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(5.dp))
                .border(2.dp, Brand.Blue, RoundedCornerShape(5.dp))
        )
        Text(
            p.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (p.done) Brand.Muted else Brand.Navy,
            textDecoration = if (p.done) TextDecoration.LineThrough else null
        )
    }
}

@Composable
private fun CalendarRow(time: String, title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brand.BlueTint, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(time, style = MaterialTheme.typography.labelLarge, color = Brand.Blue)
        Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun TodayPreview() {
    LastmarkTheme { TodayScreen() }
}
