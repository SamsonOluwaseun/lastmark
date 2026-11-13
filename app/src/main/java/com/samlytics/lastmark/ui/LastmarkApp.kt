package com.samlytics.lastmark.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.samlytics.lastmark.data.Phase
import com.samlytics.lastmark.data.Store
import com.samlytics.lastmark.ui.theme.Brand

enum class Tab(val label: String) { TODAY("Today"), TASKS("Tasks"), SETTINGS("Settings") }

@Composable
fun LastmarkApp() {
    val ctx = LocalContext.current
    val store = remember { Store.get(ctx) }
    val settings by store.settings.collectAsState()
    val focus by store.focus.collectAsState()

    if (!settings.onboarded) {
        OnboardingScreen(store)
        return
    }
    if (focus.phase != Phase.IDLE) {
        FocusScreen(store)
        return
    }

    var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }

    Scaffold(
        containerColor = Brand.Cream,
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = {
                            Icon(
                                when (t) {
                                    Tab.TODAY -> Icons.Filled.Home
                                    Tab.TASKS -> Icons.Filled.List
                                    Tab.SETTINGS -> Icons.Filled.Settings
                                },
                                contentDescription = t.label
                            )
                        },
                        label = { Text(t.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Brand.Navy,
                            indicatorColor = Brand.Lime,
                            unselectedIconColor = Brand.Muted,
                            selectedTextColor = Brand.Navy,
                            unselectedTextColor = Brand.Muted
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                Tab.TODAY -> TodayScreen(store, onOpenTasks = { tab = Tab.TASKS }, onOpenSettings = { tab = Tab.SETTINGS })
                Tab.TASKS -> TasksScreen(store)
                Tab.SETTINGS -> SettingsScreen(store)
            }
        }
    }
}
