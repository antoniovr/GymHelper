package com.tibarra.gymhelper.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tibarra.gymhelper.data.GymDatabase
import com.tibarra.gymhelper.data.model.SessionHistoryEntity
import com.tibarra.gymhelper.data.model.SetLogEntity
import com.tibarra.gymhelper.data.model.WorkoutEntity
import com.tibarra.gymhelper.ui.components.GymCard
import com.tibarra.gymhelper.util.TimeUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp,
) {
    val context = LocalContext.current
    val db = remember { GymDatabase.getDatabase(context) }
    
    val sessions by db.gymDao().getSessionHistory().collectAsState(initial = emptyList())
    val workouts by db.gymDao().getAllWorkouts().collectAsState(initial = emptyList())

    var isCalendarView by remember { mutableStateOf(value = false) }
    var sessionToDelete by remember { mutableStateOf<SessionHistoryEntity?>(null) }
    var sessionToEdit by remember { mutableStateOf<SessionHistoryEntity?>(null) }
    var showManualAdd by remember { mutableStateOf(value = false) }
    
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.systemBars
            .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            .add(WindowInsets(bottom = bottomPadding)),
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("HISTORY", fontWeight = FontWeight.Bold)
                        Text("${sessions.size} total sessions", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    IconButton(onClick = { isCalendarView = !isCalendarView }) {
                        Icon(if (isCalendarView) Icons.AutoMirrored.Filled.List else Icons.Default.CalendarMonth, contentDescription = "Toggle View")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showManualAdd = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 80.dp) // Space for bottom nav
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Manual Session")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (isCalendarView) {
                CalendarView(
                    sessions = sessions, 
                    dao = db.gymDao(), 
                    onDeleteSession = { sessionToDelete = it },
                ) { sessionToEdit = it }
            } else {
                if (sessions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No sessions recorded yet.", color = Color.Gray)
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 160.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(sessions) { session ->
                        HistoryItem(
                            session = session,
                            dao = db.gymDao(),
                            onDelete = { sessionToDelete = it },
                            onEdit = { sessionToEdit = it }
                        )
                    }
                }
            }
        }
    }

    sessionToDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Delete Session?") },
            text = { Text("Delete '${session.workoutName}' on ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(session.endTime))}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            db.gymDao().deleteSession(session)
                        }
                        sessionToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("DELETE") }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) { Text("CANCEL") }
            }
        )
    }

    sessionToEdit?.let { session ->
        EditSessionDialog(
            session = session,
            onDismiss = { sessionToEdit = null },
            onSave = { updatedSession ->
                scope.launch {
                    db.gymDao().updateSession(updatedSession)
                }
                sessionToEdit = null
            }
        )
    }

    if (showManualAdd) {
        ManualSessionDialog(
            workouts = workouts,
            onDismiss = { showManualAdd = false },
            onSave = { workout, start, end ->
                scope.launch {
                    val dao = db.gymDao()
                    val exercises = dao.getExercisesForWorkoutSync(workout.id)
                    
                    var totalReps = 0
                    var totalVolume = 0.0
                    val logsToInsert = mutableListOf<SetLogEntity>()
                    
                    exercises.forEach { ex ->
                        val variants = dao.getVariantsForExerciseSync(ex.id)
                        val v = variants.firstOrNull() ?: return@forEach
                        
                        for (i in 1..v.defaultSetsCount) {
                            totalReps += v.defaultRepsCount
                            totalVolume += v.defaultRepsCount * v.currentWeight
                            logsToInsert.add(
                                SetLogEntity(
                                    sessionHistoryId = 0,
                                    exerciseId = ex.id,
                                    exerciseName = "${ex.name} (${v.name})",
                                    setNumber = i,
                                    reps = v.defaultRepsCount,
                                    weight = v.currentWeight,
                                    isDropSet = false,
                                    durationSeconds = 60,
                                )
                            )
                        }
                        if (v.hasDropSet) {
                            totalReps += v.defaultRepsCount
                            totalVolume += v.defaultRepsCount * (v.currentWeight / 2)
                            logsToInsert.add(
                                SetLogEntity(
                                    sessionHistoryId = 0,
                                    exerciseId = ex.id,
                                    exerciseName = "${ex.name} (${v.name})",
                                    setNumber = v.defaultSetsCount + 1,
                                    reps = v.defaultRepsCount,
                                    weight = v.currentWeight / 2,
                                    isDropSet = true,
                                    durationSeconds = 30,
                                )
                            )
                        }
                    }
                    
                    val sessionId = dao.insertSession(SessionHistoryEntity(
                        workoutId = workout.id,
                        workoutName = workout.name,
                        startTime = start,
                        endTime = end,
                        cardioDurationSeconds = workout.cardioDurationMinutes * 60,
                        warmupDurationSeconds = workout.warmupDurationMinutes * 60,
                        totalReps = totalReps,
                        totalVolume = totalVolume,
                        effortRating = 3,
                        totalRestSeconds = exercises.sumOf { it.restBetweenSetsSeconds * 3 } // Approx
                    ))
                    
                    logsToInsert.forEach { log ->
                        dao.insertSetLog(log.copy(sessionHistoryId = sessionId))
                    }
                }
                showManualAdd = false
            }
        )
    }
}

@Composable
fun EditSessionDialog(
    session: SessionHistoryEntity,
    onDismiss: () -> Unit,
    onSave: (SessionHistoryEntity) -> Unit
) {
    val context = LocalContext.current
    var startTime by remember { mutableLongStateOf(session.startTime) }
    var endTime by remember { mutableLongStateOf(session.endTime) }
    var hasCardio by remember { mutableStateOf(session.cardioDurationSeconds > 0) }
    
    val dateFormatter = SimpleDateFormat("EEEE, dd/MM/yyyy", Locale.getDefault())
    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Session Times") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Date Picker (affects both start and end)
                Column {
                    Text("Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    OutlinedButton(
                        onClick = {
                            val calendar = Calendar.getInstance().apply { timeInMillis = startTime }
                            android.app.DatePickerDialog(
                                context, 
                                { _, y, m, d ->
                                    startTime = Calendar.getInstance().apply {
                                        timeInMillis = startTime
                                        set(y, m, d)
                                    }.timeInMillis
                                    endTime = Calendar.getInstance().apply {
                                        timeInMillis = endTime
                                        set(y, m, d)
                                    }.timeInMillis
                                }, 
                                calendar[Calendar.YEAR], 
                                calendar[Calendar.MONTH], 
                                calendar[Calendar.DAY_OF_MONTH],
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(dateFormatter.format(Date(startTime)))
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Start Time Picker
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Entry (Gym)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        OutlinedButton(
                            onClick = {
                                val calendar = Calendar.getInstance().apply { timeInMillis = startTime }
                                android.app.TimePickerDialog(context, { _, h, min ->
                                    startTime = Calendar.getInstance().apply {
                                        timeInMillis = startTime
                                        set(Calendar.HOUR_OF_DAY, h)
                                        set(Calendar.MINUTE, min)
                                    }.timeInMillis
                                }, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.MINUTE], true).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(timeFormatter.format(Date(startTime)))
                        }
                    }

                    // End Time Picker
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Exit (Gym)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        OutlinedButton(
                            onClick = {
                                val calendar = Calendar.getInstance().apply { timeInMillis = endTime }
                                android.app.TimePickerDialog(context, { _, h, min ->
                                    endTime = Calendar.getInstance().apply {
                                        timeInMillis = endTime
                                        set(Calendar.HOUR_OF_DAY, h)
                                        set(Calendar.MINUTE, min)
                                    }.timeInMillis
                                }, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.MINUTE], true).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(timeFormatter.format(Date(endTime)))
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Cardio included?")
                    Switch(checked = hasCardio, onCheckedChange = { hasCardio = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val newCardioDuration = if (hasCardio) {
                    if (session.cardioDurationSeconds > 0) session.cardioDurationSeconds else 1200 // 20 min default
                } else 0
                
                onSave(session.copy(
                    startTime = startTime,
                    endTime = endTime,
                    cardioDurationSeconds = newCardioDuration
                ))
            }) { Text("SAVE") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}

@Composable
fun HistoryItem(
    session: SessionHistoryEntity,
    dao: com.tibarra.gymhelper.data.dao.GymDao,
    onDelete: (SessionHistoryEntity) -> Unit,
    onEdit: (SessionHistoryEntity) -> Unit,
) {
    var expanded by remember { mutableStateOf(value = false) }
    val logs by dao.getLogsForSession(session.id).collectAsState(initial = emptyList())
    
    val dateStr = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault()).format(Date(session.endTime))
    val startTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(session.startTime))
    val endTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(session.endTime))
    
    val totalGymSecs = ((session.endTime - session.startTime) / 1000).toInt()
    val hours = totalGymSecs / 3600
    val mins = (totalGymSecs % 3600) / 60
    val gymDurationStr = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
    
    GymCard(modifier = Modifier.clickable { expanded = !expanded }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(dateStr, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(session.workoutName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$startTimeStr - $endTimeStr ($gymDurationStr in gym)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    if (session.effortRating > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Score: ${session.effortRating}/5", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
                Text("${session.totalReps} reps | ${String.format(Locale.getDefault(), "%.1f", session.totalVolume)} kg", style = MaterialTheme.typography.bodySmall)
            }
            Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = { onEdit(session) }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = { onDelete(session) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(20.dp))
            }
        }
        
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(8.dp))
                
                val groupedLogs = logs.groupBy { it.exerciseName }
                groupedLogs.forEach { (name, setLogs) ->
                    Text(name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    setLogs.forEach { log ->
                        Text("S${log.setNumber}: ${log.weight}kg x ${log.reps}", modifier = Modifier.padding(start = 8.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))
                
                val strengthDurationSecs = if (session.strengthEndTime > session.strengthStartTime) 
                    ((session.strengthEndTime - session.strengthStartTime) / 1000).toInt() 
                else 0
                
                if (session.warmupDurationSeconds > 0) {
                    HistorySummaryRow("Warm-up Time", TimeUtils.formatTime(session.warmupDurationSeconds))
                }
                if (strengthDurationSecs > 0) {
                    HistorySummaryRow("Strength Time", TimeUtils.formatTime(strengthDurationSecs))
                }
                if (session.totalRestSeconds > 0) {
                    HistorySummaryRow("Rest Time", TimeUtils.formatTime(session.totalRestSeconds))
                }
                if (session.cardioDurationSeconds > 0) {
                    HistorySummaryRow("Cardio Time", TimeUtils.formatTime(session.cardioDurationSeconds))
                }
            }
        }
    }
}

@Composable
fun HistorySummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun CalendarView(
    sessions: List<SessionHistoryEntity>, 
    dao: com.tibarra.gymhelper.data.dao.GymDao,
    onDeleteSession: (SessionHistoryEntity) -> Unit,
    onEditSession: (SessionHistoryEntity) -> Unit,
) {
    var currentMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedDay by remember { mutableStateOf<Calendar?>(null) }
    
    val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { 
                    currentMonth = (currentMonth.clone() as Calendar).apply { 
                        add(Calendar.MONTH, -1) 
                    } 
                }
            ) { Icon(Icons.Default.ChevronLeft, null) }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(monthYearFormat.format(currentMonth.time).uppercase(), fontWeight = FontWeight.Bold)
                TextButton(
                    onClick = { currentMonth = Calendar.getInstance() },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Text("TODAY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            
            IconButton(
                onClick = { 
                    currentMonth = (currentMonth.clone() as Calendar).apply { 
                        add(Calendar.MONTH, 1) 
                    } 
                }
            ) { Icon(Icons.Default.ChevronRight, null) }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(modifier = Modifier.fillMaxWidth()) {
            val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            days.forEach { Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = Color.Gray) }
        }
        
        val daysInMonth = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfMonth = (currentMonth.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }[Calendar.DAY_OF_WEEK]
        val offset = if (firstDayOfMonth == Calendar.SUNDAY) 6 else firstDayOfMonth - 2
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp) // Limit height to save space
        ) {
            items(offset) { Box(Modifier.size(40.dp)) }
            items(daysInMonth) { day ->
                val date = (currentMonth.clone() as Calendar).apply { 
                    set(Calendar.DAY_OF_MONTH, day + 1) 
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val daySessions = sessions.filter { 
                    val sDate = Calendar.getInstance().apply { 
                        timeInMillis = it.endTime 
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    sDate.timeInMillis == date.timeInMillis
                }
                
                val isSelected = selectedDay?.let {
                    (it[Calendar.DAY_OF_MONTH] == (day + 1)) &&
                    (it[Calendar.MONTH] == currentMonth[Calendar.MONTH]) &&
                    (it[Calendar.YEAR] == currentMonth[Calendar.YEAR])
                } ?: false

                CalendarDay(day + 1, daySessions, isSelected) {
                    selectedDay = date.clone() as Calendar
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendItem("Strength", MaterialTheme.colorScheme.primary)
            LegendItem("Cardio", Color(0xFFFFB300))
            LegendItem("Both", Color(0xFFFF5252))
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
        
        selectedDay?.let { date ->
            val daySessions = sessions.filter { 
                val sDate = Calendar.getInstance().apply { 
                    timeInMillis = it.endTime 
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                sDate.timeInMillis == date.timeInMillis
            }

            if (daySessions.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 160.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(daySessions) { session ->
                        HistoryItem(
                            session = session, 
                            dao = dao, 
                            onDelete = onDeleteSession,
                            onEdit = onEditSession
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No sessions on this day.", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun CalendarDay(day: Int, sessions: List<SessionHistoryEntity>, isSelected: Boolean, onClick: () -> Unit) {
    val hasStrength = sessions.any { it.totalReps > 0 }
    val hasCardio = sessions.any { it.cardioDurationSeconds > 0 }
    
    val strengthColor = MaterialTheme.colorScheme.primary
    val cardioColor = Color(0xFFFFB300) // Amber
    val bothColor = Color(0xFFFF5252) // Reddish tone
    val alpha = 0.3f

    val backgroundModifier = when {
        hasStrength && hasCardio -> Modifier.background(bothColor.copy(alpha = alpha))
        hasStrength -> Modifier.background(strengthColor.copy(alpha = alpha))
        hasCardio -> Modifier.background(cardioColor.copy(alpha = alpha))
        else -> Modifier
    }

    Column(
        modifier = Modifier.size(40.dp).padding(2.dp).clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .then(if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                .clip(CircleShape)
                .then(backgroundModifier),
            contentAlignment = Alignment.Center
        ) {
            Text(
                day.toString(), 
                fontSize = 12.sp, 
                color = MaterialTheme.colorScheme.onSurface, 
                fontWeight = if (hasStrength || hasCardio || isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
        
        Row(
            modifier = Modifier.height(6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasStrength && hasCardio) {
                Box(modifier = Modifier.padding(horizontal = 1.dp).size(4.dp).clip(CircleShape).background(bothColor))
            } else {
                if (hasStrength) {
                    Box(modifier = Modifier.padding(horizontal = 1.dp).size(4.dp).clip(CircleShape).background(strengthColor))
                }
                if (hasCardio) {
                    Box(modifier = Modifier.padding(horizontal = 1.dp).size(4.dp).clip(CircleShape).background(cardioColor))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualSessionDialog(
    workouts: List<WorkoutEntity>,
    onDismiss: () -> Unit,
    onSave: (WorkoutEntity, Long, Long) -> Unit
) {
    val context = LocalContext.current
    var selectedWorkout by remember { mutableStateOf<WorkoutEntity?>(null) }
    var timestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var startTime by remember { mutableLongStateOf(System.currentTimeMillis() - 3600000) } // 1h ago
    var endTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    
    var expanded by remember { mutableStateOf(false) }
    
    val dateFormatter = SimpleDateFormat("EEEE, dd/MM/yyyy", Locale.getDefault())
    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Manual Session") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Workout Selection
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedWorkout?.name ?: "Select Workout",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        workouts.forEach { workout ->
                            DropdownMenuItem(
                                text = { Text(workout.name) },
                                onClick = {
                                    selectedWorkout = workout
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Date Picker
                Column {
                    Text("Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    OutlinedButton(
                        onClick = {
                            val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
                            android.app.DatePickerDialog(
                                context, 
                                { _, y, m, d ->
                                    val res = Calendar.getInstance().apply {
                                        timeInMillis = timestamp
                                        set(y, m, d)
                                    }
                                    timestamp = res.timeInMillis
                                    // Sync start/end dates
                                    startTime = Calendar.getInstance().apply { timeInMillis = startTime; set(y, m, d) }.timeInMillis
                                    endTime = Calendar.getInstance().apply { timeInMillis = endTime; set(y, m, d) }.timeInMillis
                                }, 
                                calendar[Calendar.YEAR], 
                                calendar[Calendar.MONTH], 
                                calendar[Calendar.DAY_OF_MONTH],
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(dateFormatter.format(Date(timestamp)))
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Start Time Picker
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Start Time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        OutlinedButton(
                            onClick = {
                                val calendar = Calendar.getInstance().apply { timeInMillis = startTime }
                                android.app.TimePickerDialog(context, { _, h, min ->
                                    startTime = Calendar.getInstance().apply {
                                        timeInMillis = startTime
                                        set(Calendar.HOUR_OF_DAY, h)
                                        set(Calendar.MINUTE, min)
                                    }.timeInMillis
                                }, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.MINUTE], true).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(timeFormatter.format(Date(startTime)))
                        }
                    }

                    // End Time Picker
                    Column(modifier = Modifier.weight(1f)) {
                        Text("End Time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        OutlinedButton(
                            onClick = {
                                val calendar = Calendar.getInstance().apply { timeInMillis = endTime }
                                android.app.TimePickerDialog(context, { _, h, min ->
                                    endTime = Calendar.getInstance().apply {
                                        timeInMillis = endTime
                                        set(Calendar.HOUR_OF_DAY, h)
                                        set(Calendar.MINUTE, min)
                                    }.timeInMillis
                                }, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.MINUTE], true).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(timeFormatter.format(Date(endTime)))
                        }
                    }
                }
                
                Text(
                    "This will populate the session with all exercises and default sets for the selected workout.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = selectedWorkout != null,
                onClick = { selectedWorkout?.let { onSave(it, startTime, endTime) } }
            ) { Text("ADD SESSION") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
