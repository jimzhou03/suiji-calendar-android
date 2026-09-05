package com.jimzhou03.suijicalendar.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jimzhou03.suijicalendar.ui.calendar.CalendarScreen
import com.jimzhou03.suijicalendar.ui.task.TodayScreen

private enum class MainDestination(val route: String, val label: String) {
    CALENDAR("calendar", "日历"), COUNTDOWN("countdown", "倒数日"),
    TODAY("today", "今日"), SETTINGS("settings", "设置")
}

@Composable
fun SuijiApp() {
    val viewModel: SuijiViewModel = viewModel()
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val current = entry?.destination?.route ?: MainDestination.CALENDAR.route
    Scaffold(
        bottomBar = {
            NavigationBar {
                MainDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = current == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = when (destination) {
                                    MainDestination.CALENDAR -> Icons.Outlined.CalendarMonth
                                    MainDestination.COUNTDOWN -> Icons.Outlined.Event
                                    MainDestination.TODAY -> Icons.Outlined.CheckCircle
                                    MainDestination.SETTINGS -> Icons.Outlined.Settings
                                },
                                contentDescription = destination.label,
                            )
                        },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = MainDestination.CALENDAR.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(MainDestination.CALENDAR.route) { CalendarScreen(viewModel) }
            composable(MainDestination.COUNTDOWN.route) { PlaceholderScreen("倒数日", "重要日子会以卡片呈现") }
            composable(MainDestination.TODAY.route) { TodayScreen(viewModel) }
            composable(MainDestination.SETTINGS.route) { PlaceholderScreen("设置", "提醒、备份与隐私") }
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String, subtitle: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
        androidx.compose.foundation.layout.Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            Text(title, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
            Text(subtitle, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
