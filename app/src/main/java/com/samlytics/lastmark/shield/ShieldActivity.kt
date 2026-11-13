package com.samlytics.lastmark.shield

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.samlytics.lastmark.MainActivity
import com.samlytics.lastmark.data.Actions
import com.samlytics.lastmark.data.Store
import com.samlytics.lastmark.data.Strictness
import com.samlytics.lastmark.ui.LimeButton
import com.samlytics.lastmark.ui.OutlineButton
import com.samlytics.lastmark.ui.formatClock
import com.samlytics.lastmark.ui.theme.Brand
import com.samlytics.lastmark.ui.theme.LastmarkTheme

/** Full-screen reminder shown over a distracting app during a focus block. */
class ShieldActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val pkg = intent.getStringExtra(EXTRA_PACKAGE).orEmpty()
        val appName = Shield.labelFor(this, pkg)
        val store = Store.get(this)

        setContent {
            LastmarkTheme {
                val f by store.focus.collectAsState()
                val s by store.settings.collectAsState()
                val tasks by store.tasks.collectAsState()
                val task = tasks.find { it.id == f.taskId }
                val remaining = f.endsAt - System.currentTimeMillis()

                BackHandler { backToWork() }

                Column(
                    Modifier.fillMaxSize().background(Brand.Navy).safeDrawingPadding().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("$appName is paused · ${s.strictness.label}", style = MaterialTheme.typography.bodyMedium, color = Brand.Cream.copy(alpha = 0.7f))
                    Spacer(Modifier.height(20.dp))
                    Text("You were on a roll", style = MaterialTheme.typography.headlineSmall, color = Brand.Cream, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(24.dp))
                    Column(
                        Modifier.fillMaxWidth().background(Color(0xFF0D3341), RoundedCornerShape(16.dp)).padding(16.dp)
                    ) {
                        Text("You're on", style = MaterialTheme.typography.labelSmall, color = Brand.Lime)
                        Text(task?.title ?: "Your focus task", style = MaterialTheme.typography.bodyLarge, color = Brand.Cream)
                        if (!task?.nextStep.isNullOrBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text("Next step", style = MaterialTheme.typography.labelSmall, color = Brand.Cream.copy(alpha = 0.7f))
                            Text(task!!.nextStep, style = MaterialTheme.typography.bodyMedium, color = Brand.Cream)
                        }
                    }
                    if (remaining > 0) {
                        Spacer(Modifier.height(12.dp))
                        Text("${formatClock(remaining)} left in this block", style = MaterialTheme.typography.bodyMedium, color = Brand.Cream.copy(alpha = 0.7f))
                    }
                    Spacer(Modifier.height(32.dp))
                    LimeButton("Back to it", { backToWork() })
                    when (s.strictness) {
                        Strictness.GENTLE -> {
                            Spacer(Modifier.height(10.dp))
                            OutlineButton("Carry on for 10 minutes", { snoozeAndLeave(10) }, color = Brand.Cream)
                        }
                        Strictness.FIRM -> {
                            Spacer(Modifier.height(10.dp))
                            OutlineButton("5 more minutes", { snoozeAndLeave(5) }, color = Brand.Cream)
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun backToWork() {
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
        finish()
    }

    private fun snoozeAndLeave(minutes: Int) {
        Actions.snooze(this, minutes)
        finish()
    }

    companion object {
        const val EXTRA_PACKAGE = "package"
    }
}
