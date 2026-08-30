package com.tibarra.gymhelper.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow.Companion.Ellipsis
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tibarra.gymhelper.data.GymDatabase
import com.tibarra.gymhelper.data.model.ExerciseVariantEntity
import com.tibarra.gymhelper.ui.components.GymButton
import com.tibarra.gymhelper.ui.components.GymCard
import com.tibarra.gymhelper.ui.components.IncrementalPicker
import com.tibarra.gymhelper.ui.viewmodel.WorkoutViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutEditorScreen(
    workoutId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp
) {
    val context = LocalContext.current
    val db = remember { GymDatabase.getDatabase(context) }
    val viewModel: WorkoutViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return WorkoutViewModel(db.gymDao()) as T
        }
    })

    LaunchedEffect(workoutId) {
        viewModel.loadWorkoutDetails(workoutId)
    }

    val workout by viewModel.currentWorkout.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var availableToImport by remember { mutableStateOf<List<WorkoutViewModel.ExerciseSource>>(emptyList()) }
    var newExName by remember { mutableStateOf("") }
    
    var showExitValidation by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val handleBack = {
        val hasEmpty = exercises.any { it.variants.isEmpty() }
        if (hasEmpty) {
            showExitValidation = true
        } else {
            onBack()
        }
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.systemBars
            .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            .add(WindowInsets(bottom = bottomPadding)),
        topBar = {
            TopAppBar(
                title = { 
                    var tempName by remember(workout?.name) { mutableStateOf(workout?.name ?: "") }
                    val focusRequester = remember { FocusRequester() }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = tempName,
                            onValueChange = { 
                                tempName = it
                                if (it.isNotBlank()) workout?.let { w -> viewModel.updateWorkoutName(w, it) }
                            },
                            modifier = Modifier.weight(1f).focusRequester(focusRequester),
                            textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
                            singleLine = false,
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                        IconButton(onClick = { focusRequester.requestFocus() }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Name", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = handleBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 0.dp, // Remove blue tint from tonal elevation
                shadowElevation = 8.dp,
                modifier = Modifier.imePadding(),
                color = MaterialTheme.colorScheme.surface
            ) {
                GymButton(
                    text = "SAVE CHANGES",
                    onClick = handleBack,
                    modifier = Modifier.padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(exercises) { index, exState ->
                ExerciseEditorItem(
                    index = index + 1,
                    exState = exState,
                    onDelete = { viewModel.deleteExercise(exState.exercise) },
                    onMoveUp = { viewModel.moveExerciseUp(exState.exercise) },
                    onMoveDown = { viewModel.moveExerciseDown(exState.exercise) },
                    onUpdateName = { viewModel.updateExerciseName(exState.exercise, it) },
                    onUpdateRestBetween = { viewModel.updateRestBetweenSets(exState.exercise.id, it) },
                    onUpdateRestAfter = { viewModel.updateRestAfterExercise(exState.exercise.id, it) },
                    onAddVariant = { n, w, s, r, d, dt, nt -> 
                        viewModel.addVariant(exState.exercise.id, n, w, s, r, d, dt, nt) 
                    },
                    onUpdateVariant = { viewModel.updateVariant(it) },
                    onDeleteVariant = { viewModel.deleteVariant(it) },
                )
            }

            item {
                AddExerciseSection(
                    onAddManual = { showAddExerciseDialog = true },
                    onCopyFromSession = {
                        scope.launch {
                            availableToImport = viewModel.getExercisesAvailableToImport(workoutId)
                            showImportDialog = true
                        }
                    }
                )
            }

            item {
                GymCard {
                    Text("WARM-UP CONFIG", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    IncrementalPicker(
                        unit = "min",
                        value = workout?.warmupDurationMinutes ?: 0,
                        onValueChange = { workout?.let { w -> viewModel.updateWorkoutWarmup(w, it) } },
                        min = 0,
                        max = 30,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                GymCard {
                    Text("CARDIO CONFIG", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    
                    var cardioExpanded by remember { mutableStateOf(false) }
                    val cardioOptions = listOf("NONE", "Elliptical", "Stationary Bike", "Mountain Bike", "Walking", "Other")
                    
                    ExposedDropdownMenuBox(
                        expanded = cardioExpanded,
                        onExpandedChange = { cardioExpanded = !cardioExpanded }
                    ) {
                        OutlinedTextField(
                            value = if (workout?.cardioType.isNullOrBlank()) "NONE" else workout?.cardioType!!,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cardioExpanded) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = cardioExpanded,
                            onDismissRequest = { cardioExpanded = false }
                        ) {
                            cardioOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        workout?.let { w -> 
                                            val newDuration = if (option == "NONE") 0 else 20
                                            viewModel.updateWorkoutCardio(workout = w, duration = newDuration, type = option) 
                                        }
                                        cardioExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    IncrementalPicker(
                        value = workout?.cardioDurationMinutes ?: 0,
                        onValueChange = { 
                            workout?.let { w -> 
                                val newType = if (it == 0) "NONE" else if (w.cardioType == "NONE") "Other" else w.cardioType
                                viewModel.updateWorkoutCardio(workout = w, duration = it, type = newType) 
                            }
                        },
                        min = 0,
                        max = 240,
                        step = 1,
                        modifier = Modifier.fillMaxWidth(),
                        formatter = { mins ->
                            if (mins < 60) "$mins min"
                            else "${mins / 60}h ${String.format(Locale.getDefault(), "%02d", mins % 60)}m"
                        }
                    )
                }
            }
        }
    }

    if (showAddExerciseDialog) {
        AlertDialog(
            onDismissRequest = { showAddExerciseDialog = false },
            title = { Text("New Exercise") },
            text = {
                TextField(value = newExName, onValueChange = { newExName = it }, label = { Text("Exercise Name") })
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newExName.isNotBlank()) {
                        viewModel.addExercise(workoutId, newExName)
                        newExName = ""
                        showAddExerciseDialog = false
                    }
                }) { Text("ADD") }
            },
            dismissButton = {
                TextButton(onClick = { showAddExerciseDialog = false }) { Text("CANCEL") }
            }
        )
    }

    if (showImportDialog) {
        ImportExerciseDialog(
            availableExercises = availableToImport,
            onDismiss = { showImportDialog = false },
            onConfirmSelection = { selectedSources ->
                viewModel.importExercises(workoutId, selectedSources)
                showImportDialog = false
            }
        )
    }
}

@Composable
fun AddExerciseSection(onAddManual: () -> Unit, onCopyFromSession: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "EXTEND YOUR SESSION",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                onClick = onAddManual,
                modifier = Modifier.weight(1f).height(100.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Text("New", fontWeight = FontWeight.Bold)
                    Text("Manual entry", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }

            Card(
                onClick = onCopyFromSession,
                modifier = Modifier.weight(1f).height(100.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Text("Copy", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("From other session", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
fun ImportExerciseDialog(
    availableExercises: List<WorkoutViewModel.ExerciseSource>,
    onDismiss: () -> Unit,
    onConfirmSelection: (List<WorkoutViewModel.ExerciseSource>) -> Unit
) {
    val selectedIndices = remember { mutableStateListOf<Int>() }
    val grouped = remember(availableExercises) { 
        availableExercises.mapIndexed { index, source -> index to source }
            .groupBy { it.second.workoutName } 
    }
    val expandedWorkouts = remember { mutableStateMapOf<String, Boolean>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Copy Exercises") },
        text = {
            if (availableExercises.isEmpty()) {
                Text("No exercises found in other sessions to copy.", color = Color.Gray)
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 450.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    grouped.forEach { (workoutName, sources) ->
                        val isExpanded = expandedWorkouts[workoutName] ?: false
                        
                        item(key = workoutName) {
                            Surface(
                                onClick = { expandedWorkouts[workoutName] = !isExpanded },
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        workoutName.uppercase(), 
                                        style = MaterialTheme.typography.labelLarge, 
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(
                                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        
                        if (isExpanded) {
                            items(sources) { (originalIndex, source) ->
                                val isSelected = selectedIndices.contains(originalIndex)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp)
                                        .clickable { 
                                            if (isSelected) selectedIndices.remove(originalIndex) 
                                            else selectedIndices.add(originalIndex) 
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = {
                                            if (it) selectedIndices.add(originalIndex)
                                            else selectedIndices.remove(originalIndex)
                                        }
                                    )
                                    Column(modifier = Modifier.padding(start = 8.dp)) {
                                        Text(source.exercise.name, fontWeight = FontWeight.Medium)
                                        Text(
                                            text = source.variants.joinToString(", ") { it.name },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray,
                                            maxLines = 1,
                                            overflow = Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (selectedIndices.isNotEmpty()) {
                Button(onClick = { 
                    val selectedSources = selectedIndices.map { availableExercises[it] }
                    onConfirmSelection(selectedSources)
                }) {
                    Text("COPY ${selectedIndices.size} SELECTED")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}

@Composable
fun ExerciseEditorItem(
    index: Int,
    exState: WorkoutViewModel.ExerciseWithVariants,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onUpdateName: (String) -> Unit,
    onUpdateRestBetween: (Int) -> Unit,
    onUpdateRestAfter: (Int) -> Unit,
    onAddVariant: (String, Double, Int, Int, Boolean, Long, String) -> Unit,
    onUpdateVariant: (ExerciseVariantEntity) -> Unit,
    onDeleteVariant: (ExerciseVariantEntity) -> Unit
) {
    var showAddVariant by remember { mutableStateOf(false) }
    var variantToDelete by remember { mutableStateOf<ExerciseVariantEntity?>(null) }

    GymCard {
        val focusRequester = remember { FocusRequester() }
        var tempName by remember(exState.exercise.name) { mutableStateOf(exState.exercise.name) }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$index. ", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = tempName,
                onValueChange = { 
                    tempName = it
                    if (it.isNotBlank()) onUpdateName(it)
                },
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                singleLine = false,
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                )
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { focusRequester.requestFocus() }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Exercise", tint = Color.Gray, modifier = Modifier.size(18.dp))
            }

            IconButton(onClick = onMoveUp) {
                Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onMoveDown) {
                Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }

            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Gray) }
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        
        Text("REST TIMES", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        
        RestTimePickerRow(
            label = "Between Sets",
            seconds = exState.exercise.restBetweenSetsSeconds,
            onValueChange = onUpdateRestBetween
        )
        RestTimePickerRow(
            label = "End Exercise",
            seconds = exState.exercise.restAfterExerciseSeconds,
            onValueChange = onUpdateRestAfter
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text("VARIANTS", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        
        exState.variants.forEach { variant ->
            VariantEditor(variant, onUpdateVariant, onDelete = { variantToDelete = it })
        }
        
        TextButton(onClick = { showAddVariant = true }) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text("Add Variant")
        }
    }
    
    if (showAddVariant) {
        VariantDialog(onDismiss = { showAddVariant = false }, onAdd = onAddVariant)
    }

    variantToDelete?.let { variant ->
        AlertDialog(
            onDismissRequest = { variantToDelete = null },
            title = { Text("Delete Variant?") },
            text = { Text("Are you sure you want to delete the variant '${variant.name}'?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteVariant(variant)
                    variantToDelete = null
                }) { Text("DELETE", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { variantToDelete = null }) { Text("CANCEL") }
            }
        )
    }
}

@Composable
fun RestTimePickerRow(label: String, seconds: Int, onValueChange: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = label, 
            style = MaterialTheme.typography.bodySmall, 
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        IncrementalPicker(
            value = seconds,
            onValueChange = { onValueChange(it) },
            min = 0,
            max = 600,
            step = 5,
            modifier = Modifier.fillMaxWidth(),
            formatter = { s ->
                if (s < 60) "${s}s"
                else "${s / 60}m ${String.format(Locale.getDefault(), "%02d", s % 60)}s"
            }
        )
    }
}

@Composable
fun VariantDialog(onDismiss: () -> Unit, onAdd: (String, Double, Int, Int, Boolean, Long, String) -> Unit) {
    val context = LocalContext.current
    var vName by remember { mutableStateOf("") }
    var vWeight by remember { mutableStateOf("") }
    var vSets by remember { mutableStateOf("3") }
    var vReps by remember { mutableStateOf("10") }
    var vDrop by remember { mutableStateOf(false) }
    var vNotes by remember { mutableStateOf("") }
    var vDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Variant") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(value = vName, onValueChange = { vName = it }, label = { Text("Name (e.g. Smith Machine)") })
                TextField(value = vNotes, onValueChange = { vNotes = it }, label = { Text("Notes (optional tip)") })
                TextField(value = vWeight, onValueChange = { vWeight = it }, label = { Text("Initial Weight") }, placeholder = { Text("0.0") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(value = vSets, onValueChange = { vSets = it }, label = { Text("Sets") }, modifier = Modifier.weight(1f))
                    TextField(value = vReps, onValueChange = { vReps = it }, label = { Text("Reps") }, modifier = Modifier.weight(1f))
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = vDrop, onCheckedChange = { vDrop = it })
                    Text("Include Drop Set")
                }
                
                OutlinedButton(
                    onClick = {
                        val calendar = Calendar.getInstance()
                        DatePickerDialog(context, { _, year, month, day ->
                            calendar.set(year, month, day)
                            vDate = calendar.timeInMillis
                        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start Date: ${dateFormatter.format(Date(vDate))}")
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = vName.isNotBlank(),
                onClick = {
                    onAdd(
                        vName, 
                        vWeight.toDoubleOrNull() ?: 0.0, 
                        vSets.toIntOrNull() ?: 3, 
                        vReps.toIntOrNull() ?: 10,
                        vDrop, 
                        vDate,
                        vNotes
                    )
                    onDismiss()
                }
            ) { Text("ADD") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}

@Composable
fun VariantEditor(variant: ExerciseVariantEntity, onUpdate: (ExerciseVariantEntity) -> Unit, onDelete: (ExerciseVariantEntity) -> Unit) {
    var showNoteEditor by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            var tempName by remember(variant.name) { mutableStateOf(variant.name) }
            OutlinedTextField(
                value = tempName,
                onValueChange = { 
                    tempName = it
                    if (it.isNotBlank()) onUpdate(variant.copy(name = it))
                },
                label = { Text("Variant Name") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            IconButton(onClick = { onDelete(variant) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Variant", tint = Color.Gray, modifier = Modifier.size(20.dp))
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clickable { showNoteEditor = true }
                .padding(vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.EditNote,
                contentDescription = null,
                tint = if (variant.notes.isBlank()) Color.Gray else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (variant.notes.isBlank()) "Add notes/tips..." else variant.notes,
                style = MaterialTheme.typography.bodyMedium,
                color = if (variant.notes.isBlank()) Color.Gray else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = Ellipsis
            )
        }

        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(variant.initialWeightDate))
        Text("Started: $dateStr", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            var tempSets by remember(variant.defaultSetsCount) { mutableStateOf(variant.defaultSetsCount.toString()) }
            OutlinedTextField(
                value = tempSets,
                onValueChange = { 
                    tempSets = it
                    it.toIntOrNull()?.let { v -> onUpdate(variant.copy(defaultSetsCount = v)) }
                },
                label = { Text("Sets") },
                modifier = Modifier.width(65.dp)
            )
            var tempReps by remember(variant.defaultRepsCount) { mutableStateOf(variant.defaultRepsCount.toString()) }
            OutlinedTextField(
                value = tempReps,
                onValueChange = { 
                    tempReps = it
                    it.toIntOrNull()?.let { v -> onUpdate(variant.copy(defaultRepsCount = v)) }
                },
                label = { Text("Reps") },
                modifier = Modifier.width(65.dp)
            )
            
            var tempWeight by remember(variant.currentWeight) { mutableStateOf(if (variant.currentWeight == 0.0) "" else variant.currentWeight.toString()) }
            OutlinedTextField(
                value = tempWeight,
                onValueChange = { 
                    tempWeight = it
                    if (it.isNotEmpty()) {
                        it.toDoubleOrNull()?.let { v -> onUpdate(variant.copy(currentWeight = v)) }
                    }
                },
                label = { Text("Weight") },
                modifier = Modifier.weight(1f)
            )
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Drop", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                IconButton(onClick = { onUpdate(variant.copy(hasDropSet = !variant.hasDropSet)) }) {
                    Icon(
                        if (variant.hasDropSet) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Drop Set",
                        tint = if (variant.hasDropSet) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
            }
        }
    }

    if (showNoteEditor) {
        var tempNotes by remember { mutableStateOf(variant.notes) }
        Dialog(
            onDismissRequest = { showNoteEditor = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Edit Tips",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { showNoteEditor = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    
                    Text(
                        text = variant.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = tempNotes,
                        onValueChange = { tempNotes = it },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        placeholder = { Text("Add any tips or observations...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    GymButton(
                        text = "SAVE CHANGES",
                        onClick = {
                            onUpdate(variant.copy(notes = tempNotes))
                            showNoteEditor = false
                        }
                    )
                }
            }
        }
    }
}
