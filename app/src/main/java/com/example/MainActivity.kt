package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.MainViewModel
import com.example.ui.screens.ProgressDashboardScreen
import com.example.ui.screens.ReminderConfiguratorScreen
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.FocusProgressTheme
import com.example.ui.theme.PurpleContainer
import com.example.ui.theme.PurpleOnContainer
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.TextMuted

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FocusProgressTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Notification Permission Request for Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val habits by viewModel.habits.collectAsState()
    val customConfig by viewModel.customConfig.collectAsState()
    val quotes by viewModel.quotes.collectAsState()
    val latestSummary by viewModel.latestSummary.collectAsState()
    val allSummaries by viewModel.allSummaries.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "dashboard"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentRoute == "dashboard",
                    onClick = {
                        if (currentRoute != "dashboard") {
                            navController.navigate("dashboard") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Dashboard,
                            contentDescription = "Dashboard",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = { Text("Dashboard") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PurpleOnContainer,
                        selectedTextColor = PurplePrimary,
                        indicatorColor = PurpleContainer,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("dashboard_tab")
                )

                NavigationBarItem(
                    selected = currentRoute == "reminders",
                    onClick = {
                        if (currentRoute != "reminders") {
                            navController.navigate("reminders") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Reminders",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = { Text("Reminders") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PurpleOnContainer,
                        selectedTextColor = PurplePrimary,
                        indicatorColor = PurpleContainer,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("reminders_tab")
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("dashboard") {
                ProgressDashboardScreen(
                    habits = habits,
                    latestSummary = latestSummary,
                    allSummaries = allSummaries,
                    onAddHabit = { name, target, progress ->
                        viewModel.addHabit(name, target, progress)
                    },
                    onUpdateProgress = { habit, newProgress ->
                        viewModel.updateHabitProgress(habit, newProgress)
                    },
                    onUpdateTarget = { habit, newTarget ->
                        viewModel.updateHabitTarget(habit, newTarget)
                    },
                    onToggleCompletion = { habit, isCompleted ->
                        viewModel.toggleHabitCompletion(habit, isCompleted)
                    },
                    onDeleteHabit = { habit ->
                        viewModel.deleteHabit(habit)
                    },
                    onTriggerImmediateNightlySummary = {
                        viewModel.triggerImmediateNightlySummary()
                    },
                    snackbarHostState = snackbarHostState
                )
            }

            composable("reminders") {
                ReminderConfiguratorScreen(
                    config = customConfig,
                    quotes = quotes,
                    onSaveCustomMessage = { msg ->
                        viewModel.saveCustomMessage(msg)
                    },
                    onAddQuote = { text, author ->
                        viewModel.addQuote(text, author)
                    },
                    onDeleteQuote = { quote ->
                        viewModel.deleteQuote(quote)
                    },
                    onTriggerImmediateNotification = {
                        viewModel.triggerImmediateReminder()
                    },
                    snackbarHostState = snackbarHostState
                )
            }
        }
    }
}
