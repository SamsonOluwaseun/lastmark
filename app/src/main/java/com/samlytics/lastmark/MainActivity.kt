package com.samlytics.lastmark

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.samlytics.lastmark.data.Actions
import com.samlytics.lastmark.reminders.Notify
import com.samlytics.lastmark.ui.LastmarkApp
import com.samlytics.lastmark.ui.theme.LastmarkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Notify.ensureChannels(this)
        enableEdgeToEdge()
        setContent {
            LastmarkTheme {
                LastmarkApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Actions.rollover(this)
        Actions.checkTimers(this)
    }
}
