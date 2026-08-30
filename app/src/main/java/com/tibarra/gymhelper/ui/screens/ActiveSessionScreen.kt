package com.tibarra.gymhelper.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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
import com.tibarra.gymhelper.ui.components.EffortRatingBar
import com.tibarra.gymhelper.ui.components.GymButton
import com.tibarra.gymhelper.ui.components.GymCard
import com.tibarra.gymhelper.ui.theme.DarkBackground
import com.tibarra.gymhelper.ui.viewmodel.SetActiveState
import com.tibarra.gymhelper.ui.viewmodel.SessionViewModel
import com.tibarra.gymhelper.util.TimeUtils
import com.tibarra.gymhelper.util.WakeLockManager
import java.util.Locale

@Composable
fun ActiveSessionScreen(
    workoutId: Long,
    onSessionEnd: () -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp
) {
    val context = LocalContext.current
    val db = remember { GymDatabase.getDatabase(context) }
    val viewModel: SessionViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return SessionViewModel(db.gymDao()) as T
            }
        },
    )

    val uiState by viewModel.uiState.collectAsState()
    var showExitDialog by remember { mutableStateOf(value = false) }
    var showFinishConfirm by remember { mutableStateOf(false) }
    var setIndexToUnmark by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var editingNoteIndex by remember { mutableStateOf<Int?>(null) }
    
    LaunchedEffect(workoutId) {
        viewModel.startSession(workoutId)
        viewModel.initWearSync(context)
    }

    DisposableEffect(Unit) {
        val activity = context as? android.app.Activity
        activity?.let { WakeLockManager.keepScreenOn(activity = it, on = true) }
        onDispose {
            activity?.let { WakeLockManager.keepScreenOn(activity = it, on = false) }
            // Always ensure cleanup when leaving the screen
            viewModel.endSessionCleanup(context)
        }
    }

    BackHandler(enabled = uiState.isStarted && !uiState.isFinished) {
        showExitDialog = true
    }

    if (uiState.isFinished) {
        LaunchedEffect(Unit) { onSessionEnd() }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(bottom = bottomPadding)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    if (!uiState.isStarted) {
                        IconButton(onClick = onSessionEnd) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Gray)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.workout?.name ?: "",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = Ellipsis
                        )
                        if (uiState.isStarted) {
                            Text(
                                text = TimeUtils.formatTime(uiState.totalSessionTimeSeconds),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                if (!uiState.isStarted) {
                    GymButton(
                        text = "START",
                        onClick = { viewModel.startWorkout(context) },
                        modifier = Modifier.widthIn(min = 100.dp).height(48.dp),
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f).imePadding(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                userScrollEnabled = true
            ) {
                item {
                    val warmupMinutes = uiState.workout?.warmupDurationMinutes ?: 0
                    if (warmupMinutes > 0) {
                        val isFinished = !uiState.isWarmupActive && (uiState.warmupTimeSeconds > 0)
                        GymCard(modifier = Modifier.alpha(if (uiState.isStarted && !isFinished) 1f else 0.5f)) {
                            Text("WARM-UP", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            
                            val elapsedSeconds = uiState.warmupTimeSeconds
                            val configuredSeconds = warmupMinutes * 60
                            val remainingSeconds = configuredSeconds - elapsedSeconds
                            
                            val displayTime = if ((remainingSeconds >= 0) && !isFinished) {
                                TimeUtils.formatTime(remainingSeconds)
                            } else {
                                (if (remainingSeconds < 0) "EXTRA: " else "") + TimeUtils.formatTime(if (isFinished) elapsedSeconds else -remainingSeconds)
                            }

                            Text(
                                text = displayTime,
                                style = MaterialTheme.typography.displayMedium,
                                color = if (isFinished) Color.Gray else if (remainingSeconds >= 0) MaterialTheme.colorScheme.primary else Color.Yellow,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )

                            if (uiState.isStarted && !uiState.isResting && !isFinished) {
                                GymButton(
                                    text = if (uiState.isWarmupActive) "FINISH WARM-UP" else "START WARM-UP",
                                    onClick = { if (uiState.isWarmupActive) viewModel.stopWarmup(context) else viewModel.startWarmup(context) },
                                    containerColor = if (uiState.isWarmupActive) Color.Red else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.fillMaxWidth().height(64.dp)
                                )
                            }
                        }
                    }
                }

                itemsIndexed(uiState.exercises) { exIndex, exState ->
                    val isLockedByWarmup = uiState.isWarmupActive
                    val isLockedByCardio = uiState.isCardioActive
                    
                    // Logic to find if any other exercise is already in progress
                    val activeExerciseIndex = uiState.exercises.indexOfFirst { ex ->
                        ex.sets.any { it.isCompleted } && !ex.sets.all { it.isCompleted }
                    }
                    
                    val isInteractionAllowed = activeExerciseIndex == -1 || activeExerciseIndex == exIndex

                    ExerciseSessionCard(
                        index = exIndex + 1,
                        exState = exState,
                        isEnabled = uiState.isStarted && !uiState.isResting && !isLockedByWarmup && !isLockedByCardio && isInteractionAllowed,
                        onSelectVariant = { viewModel.selectVariant(exIndex, it) },
                        onEditNotes = { editingNoteIndex = exIndex },
                        onToggleSet = { setIndex ->
                            if (exState.sets[setIndex].isCompleted) {
                                setIndexToUnmark = exIndex to setIndex
                            } else {
                                viewModel.toggleSet(exIndex, setIndex, context)
                            }
                        },
                        onUpdateSet = { sIndex, w, r -> 
                            viewModel.updateSetValues(exIndex, sIndex, w, r) 
                        }
                    )
                }

                item {
                    val cardioMinutes = uiState.workout?.cardioDurationMinutes ?: 0
                    if (cardioMinutes > 0) {
                        GymCard(modifier = Modifier.alpha(if (uiState.isStarted && !uiState.isCardioFinished) 1f else 0.5f)) {
                            Text("CARDIO: ${uiState.workout?.cardioType?.uppercase()}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            
                            val elapsedSeconds = uiState.cardioTimeSeconds
                            val configuredSeconds = cardioMinutes * 60
                            val remainingSeconds = configuredSeconds - elapsedSeconds
                            
                            val displayTime = if ((remainingSeconds >= 0) && !uiState.isCardioFinished) {
                                TimeUtils.formatTime(remainingSeconds)
                            } else {
                                (if (remainingSeconds < 0 && !uiState.isCardioFinished) "EXTRA: " else "") + TimeUtils.formatTime(if (uiState.isCardioFinished) elapsedSeconds else -remainingSeconds)
                            }

                            Text(
                                text = displayTime,
                                style = MaterialTheme.typography.displayMedium,
                                color = if (uiState.isCardioFinished) Color.Gray else if (remainingSeconds >= 0) MaterialTheme.colorScheme.primary else Color.Yellow,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )

                            if (uiState.isStarted && !uiState.isResting && !uiState.isWarmupActive && !uiState.isCardioFinished) {
                                val isStarted = uiState.isCardioActive || uiState.cardioTimeSeconds > 0
                                
                                if (isStarted) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        GymButton(
                                            text = if (uiState.isCardioActive) "Pause" else "Resume",
                                            onClick = { if (uiState.isCardioActive) viewModel.stopCardio(context) else viewModel.startCardio(context) },
                                            containerColor = if (uiState.isCardioActive) Color(0xFFFFC107) else MaterialTheme.colorScheme.primary,
                                            contentColor = if (uiState.isCardioActive) Color.Black else Color.White,
                                            modifier = Modifier.weight(1f)
                                        )
                                        GymButton(
                                            text = "Finish",
                                            onClick = { viewModel.stopCardio(context, markAsFinished = true) },
                                            containerColor = Color.Red,
                                            contentColor = Color.White,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                } else {
                                    GymButton(
                                        text = "Start Cardio",
                                        onClick = { viewModel.startCardio(context) },
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (uiState.isStarted && !uiState.isResting) {
                GymButton(
                    text = "FINISH WORKOUT", 
                    onClick = { showFinishConfirm = true }, 
                    containerColor = Color.Transparent, 
                    contentColor = Color.Red
                )
            }
        }

        AnimatedVisibility(
            visible = uiState.isResting,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            RestTimerPanel(
                timeLeft = uiState.restTimeLeft,
                onAdjust = { viewModel.adjustRestTime(it, context) },
                onSkip = { viewModel.skipRest(context) }
            )
        }
    }

    if (uiState.showSummary) {
        SummaryDialog(
            summary = uiState.summary!!,
            effortRating = uiState.effortRating,
            onRatingSelected = { viewModel.setEffortRating(it) },
            onConfirm = { viewModel.confirmSaveSession(context) },
            onDiscard = { viewModel.discardSession(context) }
        )
    }

    if (showFinishConfirm) {
        AlertDialog(
            onDismissRequest = { showFinishConfirm = false },
            title = { Text("Finish Workout?") },
            text = { Text("Are you sure you want to end this session and see the summary?") },
            confirmButton = {
                TextButton(onClick = { 
                    showFinishConfirm = false
                    viewModel.showSummary() 
                }) { Text("FINISH", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showFinishConfirm = false }) { Text("CANCEL") }
            }
        )
    }

    setIndexToUnmark?.let { (exIndex, sIndex) ->
        AlertDialog(
            onDismissRequest = { setIndexToUnmark = null },
            title = { Text("Unmark Set?") },
            text = { Text("Do you want to mark this set as incomplete?") },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.toggleSet(exIndex, sIndex, context)
                    setIndexToUnmark = null
                }) { Text("YES, UNMARK", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { setIndexToUnmark = null }) { Text("CANCEL") }
            }
        )
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit Session?") },
            text = { Text("Do you want to exit and return to the main page? Your current progress will be lost if you don't save.") },
            confirmButton = {
                TextButton(onClick = { onSessionEnd() }) { Text("YES, EXIT", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("STAY") }
            }
        )
    }

    editingNoteIndex?.let { exIndex ->
        val exerciseState = uiState.exercises[exIndex]
        var tempNotes by remember(exIndex) { mutableStateOf(exerciseState.selectedVariant?.notes ?: "") }
        
        Dialog(
            onDismissRequest = { editingNoteIndex = null },
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
                        IconButton(onClick = { editingNoteIndex = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    
                    val variantName = exerciseState.selectedVariant?.name ?: exerciseState.exercise.name
                    Text(
                        text = variantName,
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
                            viewModel.updateVariantNotes(exIndex, tempNotes)
                            editingNoteIndex = null
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseSessionCard(
    index: Int,
    exState: com.tibarra.gymhelper.ui.viewmodel.ExerciseSessionState,
    isEnabled: Boolean,
    onSelectVariant: (ExerciseVariantEntity) -> Unit,
    onEditNotes: () -> Unit,
    onToggleSet: (Int) -> Unit,
    onUpdateSet: (Int, Double, Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val isCompleted = exState.sets.isNotEmpty() && exState.sets.all { it.isCompleted }
    val canChangeVariant = isEnabled && exState.sets.none { it.isCompleted }

    GymCard(modifier = Modifier.alpha(if (isCompleted) 0.5f else 1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$index. ${exState.exercise.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (isCompleted) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }

        ExposedDropdownMenuBox(
            expanded = expanded && canChangeVariant,
            onExpandedChange = { if (canChangeVariant) expanded = !expanded },
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            OutlinedTextField(
                value = exState.selectedVariant?.name ?: "Select Variant",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { if (canChangeVariant) ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true).fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                enabled = canChangeVariant
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                exState.variants.forEach { variant ->
                    DropdownMenuItem(
                        text = { Text(variant.name) },
                        onClick = {
                            onSelectVariant(variant)
                            expanded = false
                        }
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = isEnabled) { onEditNotes() }
                .padding(vertical = 8.dp, horizontal = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.EditNote,
                contentDescription = null,
                tint = if (exState.selectedVariant?.notes.isNullOrBlank()) Color.Gray else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            val notes = exState.selectedVariant?.notes
            Text(
                text = if (notes.isNullOrBlank()) "Add notes/tips for this variant..." else notes,
                style = MaterialTheme.typography.bodySmall,
                color = if (notes.isNullOrBlank()) Color.Gray else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = Ellipsis
            )
        }

        exState.sets.forEachIndexed { sIndex, set ->
            val isPreviousDone = sIndex == 0 || exState.sets[sIndex - 1].isCompleted
            val isNextSet = !set.isCompleted && isPreviousDone
            SetRow(
                setState = set,
                isEnabled = isEnabled && isPreviousDone,
                isVibrant = isNextSet && isEnabled,
                onToggle = { onToggleSet(sIndex) },
                onUpdate = { w, r -> onUpdateSet(sIndex, w, r) }
            )
        }
    }
}

@Composable
fun SetRow(
    setState: SetActiveState,
    isEnabled: Boolean,
    isVibrant: Boolean,
    onToggle: () -> Unit,
    onUpdate: (Double, Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp).alpha(if (setState.isCompleted) 0.4f else 1f)
    ) {
        Text(if (setState.isDropSet) "DR" else "S${setState.setNumber}", fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp), color = if (setState.isDropSet) Color.Yellow else MaterialTheme.colorScheme.onSurface)
        
        var tempWeight by remember(setState.weight) { mutableStateOf(if (setState.weight == 0.0) "" else setState.weight.toString()) }
        OutlinedTextField(
            value = tempWeight,
            onValueChange = { 
                tempWeight = it
                if (it.isNotEmpty()) {
                    it.toDoubleOrNull()?.let { w -> onUpdate(w, setState.reps) }
                }
            },
            label = { Text("kg") },
            modifier = Modifier.width(85.dp),
            enabled = isEnabled && !setState.isCompleted,
            singleLine = true
        )
        
        var tempReps by remember(setState.reps) { mutableStateOf(setState.reps.toString()) }
        OutlinedTextField(
            value = tempReps,
            onValueChange = { 
                tempReps = it
                if (it.isNotEmpty()) {
                    it.toIntOrNull()?.let { r -> onUpdate(setState.weight, r) }
                }
            },
            label = { Text("reps") },
            modifier = Modifier.width(75.dp),
            enabled = isEnabled && !setState.isCompleted,
            singleLine = true
        )

        val buttonColor = if (setState.isCompleted) MaterialTheme.colorScheme.primary else if (isVibrant) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else if (isEnabled) Color.DarkGray else Color.Transparent
        val contentColor = if (setState.isCompleted || isVibrant) Color.White else Color.Gray

        Box(
            modifier = Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(8.dp))
                .background(buttonColor)
                .then(if (!setState.isCompleted && !isVibrant && isEnabled) Modifier.border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(8.dp)) else Modifier)
                .clickable(enabled = isEnabled) { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                if (setState.isCompleted) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = contentColor)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("DONE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = contentColor)
                } else {
                    Text("CHECK", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = contentColor)
                }
            }
        }
    }
}

@Composable
fun SummaryDialog(
    summary: com.tibarra.gymhelper.ui.viewmodel.SessionSummary,
    effortRating: Int,
    onRatingSelected: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDiscard: () -> Unit
) {
    var showDiscardConfirm by remember { mutableStateOf(false) }

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text("Discard Session?") },
            text = { Text("All progress from this session will be permanently lost.") },
            confirmButton = {
                TextButton(onClick = onDiscard) { Text("DISCARD", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) { Text("KEEP") }
            }
        )
    }

    AlertDialog(
        onDismissRequest = {},
        title = { Text("SESSION SUMMARY", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryRow("Date", summary.date)
                SummaryRow("Start Time", summary.startTime)
                SummaryRow("End Time", summary.endTime)
                SummaryRow("Total Gym Time", summary.totalGymDuration)
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                
                if (summary.warmupDuration != "00:00") {
                    SummaryRow("Warm-up Time", summary.warmupDuration)
                }
                if (summary.strengthDuration != "00:00") {
                    SummaryRow("Strength Time", summary.strengthDuration)
                }
                if (summary.restDuration != "00:00") {
                    SummaryRow("Rest Time", summary.restDuration)
                }
                if (summary.cardioDuration != "00:00") {
                    SummaryRow("Cardio Time", summary.cardioDuration)
                }
                
                SummaryRow("Total Reps", summary.totalReps.toString())
                SummaryRow("Total Volume", "${String.format(Locale.getDefault(), "%.1f", summary.totalVolume)} kg")
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Perceived Effort (1-5):", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                EffortRatingBar(selectedRating = effortRating, onRatingSelected = onRatingSelected)
            }
        },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GymButton(text = "SAVE & CLOSE", onClick = onConfirm)
                GymButton(text = "DISCARD", onClick = { showDiscardConfirm = true }, containerColor = Color.Red)
            }
        }
    )
}

@Composable
fun SummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray)
        Text(value, fontWeight = FontWeight.Bold)
    }
}
@Composable
fun RestTimerPanel(
    timeLeft: Int,
    onAdjust: (Int) -> Unit,
    onSkip: () -> Unit
) {
    GymCard(
        modifier = Modifier.padding(vertical = 8.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("RESTING", style = MaterialTheme.typography.labelMedium, color = Color.Red, fontWeight = FontWeight.Bold)
                Text(
                    text = TimeUtils.formatTime(timeLeft),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButton(
                    onClick = { onAdjust(-10) },
                    modifier = Modifier.size(56.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Minus", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(32.dp))
                }
                IconButton(
                    onClick = { onAdjust(10) },
                    modifier = Modifier.size(56.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Plus", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(32.dp))
                }
                IconButton(
                    onClick = onSkip,
                    modifier = Modifier.size(56.dp).background(Color.Red, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Skip", tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}
