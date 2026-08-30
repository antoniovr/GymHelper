package com.tibarra.gymhelper.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.tibarra.gymhelper.ui.screens.*

sealed class Screen(val route: String) {
    object WorkoutList : Screen("workout_list")
    object WorkoutEditor : Screen("workout_editor/{workoutId}") {
        fun createRoute(workoutId: Long) = "workout_editor/$workoutId"
    }
    object ActiveSession : Screen("active_session/{workoutId}") {
        fun createRoute(workoutId: Long) = "active_session/$workoutId"
    }
    object History : Screen("history")
    object Settings : Screen("settings")
}

data class BottomNavItem(
    val name: String,
    val route: String,
    val icon: ImageVector
)

val BottomNavItems = listOf(
    BottomNavItem("Sessions", Screen.WorkoutList.route, Icons.Default.FitnessCenter),
    BottomNavItem("History", Screen.History.route, Icons.Default.History),
    BottomNavItem("Settings", Screen.Settings.route, Icons.Default.Settings)
)

@Composable
fun GymNavHost(
    navController: NavHostController, 
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp,
    onThemeChange: (Int) -> Unit = {},
    onAccentChange: (Int) -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = Screen.WorkoutList.route,
        modifier = modifier
    ) {
        composable(Screen.WorkoutList.route) {
            WorkoutListScreen(
                modifier = modifier,
                bottomPadding = bottomPadding,
                onEditWorkout = { id -> 
                    navController.navigate(Screen.WorkoutEditor.createRoute(id)) 
                },
                onStartWorkout = { id -> 
                    navController.navigate(Screen.ActiveSession.createRoute(id)) 
                }
            )
        }
        
        composable(
            route = Screen.WorkoutEditor.route,
            arguments = listOf(navArgument("workoutId") { type = NavType.LongType })
        ) { backStackEntry ->
            val workoutId = backStackEntry.arguments?.getLong("workoutId") ?: 0L
            WorkoutEditorScreen(
                workoutId = workoutId,
                modifier = modifier,
                bottomPadding = bottomPadding,
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.ActiveSession.route,
            arguments = listOf(navArgument("workoutId") { type = NavType.LongType })
        ) { backStackEntry ->
            val workoutId = backStackEntry.arguments?.getLong("workoutId") ?: 0L
            ActiveSessionScreen(
                workoutId = workoutId,
                modifier = modifier,
                bottomPadding = bottomPadding,
                onSessionEnd = { 
                    navController.navigate(Screen.WorkoutList.route) {
                        popUpTo(Screen.WorkoutList.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.History.route) {
            HistoryScreen(
                modifier = modifier,
                bottomPadding = bottomPadding
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                modifier = modifier,
                bottomPadding = bottomPadding,
                onThemeChange = onThemeChange, 
                onAccentChange = onAccentChange
            )
        }
    }
}
