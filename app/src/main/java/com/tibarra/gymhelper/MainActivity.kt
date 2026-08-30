package com.tibarra.gymhelper

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tibarra.gymhelper.ui.navigation.BottomNavItems
import com.tibarra.gymhelper.ui.navigation.GymNavHost
import com.tibarra.gymhelper.ui.navigation.Screen
import com.tibarra.gymhelper.ui.theme.GymHelperTheme
import com.tibarra.gymhelper.util.PreferencesManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefsManager = PreferencesManager(this)
        
        setContent {
            var themeMode by remember { mutableIntStateOf(prefsManager.themeMode) }
            var accentColorIndex by remember { mutableIntStateOf(prefsManager.accentColorIndex) }
            
            GymHelperTheme(themeMode = themeMode, accentColorIndex = accentColorIndex) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                // Resume logic
                LaunchedEffect(Unit) {
                    val activeId = prefsManager.activeWorkoutId
                    if (activeId != -1L) {
                        navController.navigate(Screen.ActiveSession.createRoute(activeId)) {
                            popUpTo(Screen.WorkoutList.route) { inclusive = false }
                        }
                    }
                    
                    // Handle Intent actions
                    intent?.getStringExtra("ACTION")?.let { action ->
                        if (action == "FINISH_WORKOUT") {
                            // We are already navigating to ActiveSession if activeId != -1
                        }
                    }
                }
                var hasNotificationPermission by remember {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        mutableStateOf(
                            ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                        )
                    } else {
                        mutableStateOf(true)
                    }
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    hasNotificationPermission = isGranted
                }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                // Show bar only on main screens
                val showBottomBar = currentDestination?.route in listOf(
                    Screen.WorkoutList.route,
                    Screen.History.route,
                    Screen.Settings.route
                )

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 0.dp
                            ) {
                                BottomNavItems.forEach { item ->
                                    val isSelected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                                    NavigationBarItem(
                                        icon = { 
                                            Icon(
                                                item.icon, 
                                                contentDescription = item.name
                                            ) 
                                        },
                                        label = { 
                                            Text(
                                                item.name
                                            ) 
                                        },
                                        selected = isSelected,
                                        onClick = {
                                            navController.navigate(item.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    val layoutDirection = LocalLayoutDirection.current
                    GymNavHost(
                        navController = navController,
                        modifier = Modifier.padding(
                            start = innerPadding.calculateStartPadding(layoutDirection),
                            end = innerPadding.calculateEndPadding(layoutDirection)
                        ),
                        bottomPadding = innerPadding.calculateBottomPadding(),
                        onThemeChange = { newMode -> 
                            themeMode = newMode
                            prefsManager.themeMode = newMode
                        },
                        onAccentChange = { newAccent ->
                            accentColorIndex = newAccent
                            prefsManager.accentColorIndex = newAccent
                        }
                    )
                }
            }
        }
    }
}
