package com.samlytics.lastmark

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.samlytics.lastmark.ui.TodayScreen
import com.samlytics.lastmark.ui.theme.LastmarkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LastmarkTheme {
                TodayScreen()
            }
        }
    }
}
