package com.tibarra.gymhelper.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tibarra.gymhelper.data.GymDatabase
import com.tibarra.gymhelper.data.model.WorkoutEntity
import com.tibarra.gymhelper.ui.components.GymButton
import com.tibarra.gymhelper.ui.components.GymCard
import com.tibarra.gymhelper.ui.viewmodel.WorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutListScreen(
    onEditWorkout: (Long) -> Unit,
    onStartWorkout: (Long) -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp,
) {
    val context = LocalContext.current
    val db = remember { GymDatabase.getDatabase(context) }
    val viewModel: WorkoutViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return WorkoutViewModel(db.gymDao()) as T
            }
        },
    )

    val workouts by viewModel.workouts.collectAsState()
    val exercisesMap by viewModel.exercisesCountMap.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(value = false) }
    var newWorkoutName by remember { mutableStateOf("") }
    
    var workoutToDelete by remember { mutableStateOf<WorkoutEntity?>(null) }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.systemBars
            .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            .add(WindowInsets(bottom = bottomPadding)),
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("MY SESSIONS", fontWeight = FontWeight.Bold)
                        Text("${workouts.size} sessions created", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Workout")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 160.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(workouts) { workout ->
                val count = exercisesMap[workout.id] ?: 0
                val hasCardio = workout.cardioDurationMinutes > 0
                
                val label = when {
                    (count == 0) && !hasCardio -> "Empty"
                    (count == 0) && hasCardio -> "Cardio"
                    (count > 0) && !hasCardio -> "$count Exercises"
                    else -> "$count Exercises & Cardio"
                }
                
                GymCard {
                    Text(
                        text = workout.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = label,
                        color = if (label == "Empty") Color.Gray else MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Row 1: Primary Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GymButton(
                            text = "Start",
                            onClick = { onStartWorkout(workout.id) },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = { onEditWorkout(workout.id) },
                            modifier = Modifier.height(56.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                        ) {
                            Text("EDIT")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 2: Management Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.moveWorkoutUp(workout) }) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { viewModel.moveWorkoutDown(workout) }) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { workoutToDelete = workout }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("New Workout") },
            text = {
                TextField(
                    value = newWorkoutName,
                    onValueChange = { newWorkoutName = it },
                    placeholder = { Text("Name") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newWorkoutName.isNotBlank()) {
                            viewModel.addWorkout(newWorkoutName) { id ->
                                onEditWorkout(id)
                            }
                            newWorkoutName = ""
                            showAddDialog = false
                        }
                    }
                ) { Text("ADD") }
            }
        )
    }

    workoutToDelete?.let { workout ->
        AlertDialog(
            onDismissRequest = { workoutToDelete = null },
            title = { Text("Delete Workout?") },
            text = { Text("This will permanently remove '${workout.name}'.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteWorkout(workout)
                        workoutToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("DELETE") }
            },
            dismissButton = {
                TextButton(onClick = { workoutToDelete = null }) { Text("CANCEL") }
            }
        )
    }
}
