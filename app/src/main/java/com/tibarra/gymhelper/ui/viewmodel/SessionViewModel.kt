package com.tibarra.gymhelper.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tibarra.gymhelper.data.dao.GymDao
import com.tibarra.gymhelper.data.model.*
import com.tibarra.gymhelper.service.RestTimerService
import com.tibarra.gymhelper.shared.SyncUtils
import com.tibarra.gymhelper.util.CsvManager
import com.tibarra.gymhelper.util.TimeUtils
import com.tibarra.gymhelper.util.WearCommandEventBus
import com.tibarra.gymhelper.util.WearSyncManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration.Companion.seconds

data class SessionUiState(
    val workout: WorkoutEntity? = null,
    val exercises: List<ExerciseSessionState> = emptyList(),
    val isResting: Boolean = false,
    val restTimeLeft: Int = 0,
    val totalRestSeconds: Int = 0,
    val strengthStartTime: Long = 0,
    val strengthEndTime: Long = 0,
    val totalSessionTimeSeconds: Int = 0,
    val warmupEndTimestamp: Long = 0,
    val warmupTimeSeconds: Int = 0,
    val warmupTargetSeconds: Int = 0,
    val isWarmupActive: Boolean = false,
    val cardioEndTimestamp: Long = 0,
    val cardioTimeSeconds: Int = 0,
    val cardioTargetSeconds: Int = 0,
    val isCardioActive: Boolean = false,
    val isCardioFinished: Boolean = false,
    val isFinished: Boolean = false,
    val cardioStartTime: Long = 0,
    val cardioEndTime: Long = 0,
    val showSummary: Boolean = false,
    val summary: SessionSummary? = null,
    val isStarted: Boolean = false,
    val effortRating: Int = 0,
)

data class SessionSummary(
    val date: String,
    val startTime: String,
    val endTime: String,
    val totalGymDuration: String,
    val warmupDuration: String,
    val strengthDuration: String,
    val cardioDuration: String,
    val restDuration: String,
    val totalReps: Int,
    val totalVolume: Double,
)

data class ExerciseSessionState(
    val exercise: ExerciseEntity,
    val variants: List<ExerciseVariantEntity>,
    val selectedVariant: ExerciseVariantEntity?,
    val sets: List<SetActiveState> = emptyList(),
    val isStarted: Boolean = false
)

data class SetActiveState(
    val setNumber: Int,
    val isDropSet: Boolean = false,
    val isCompleted: Boolean = false,
    val weight: Double,
    val reps: Int = 10,
    val durationSeconds: Int = 0
)

class SessionViewModel(private val dao: GymDao) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private var sessionTimerJob: Job? = null
    private var restTimerJob: Job? = null
    private var cardioTimerJob: Job? = null
    private var warmupTimerJob: Job? = null
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)

    private var startTimeMillis = 0L
    private var lastActionTimestamp = 0L
    private var wearSyncManager: WearSyncManager? = null
    private var prefsManager: com.tibarra.gymhelper.util.PreferencesManager? = null

    fun initWearSync(context: Context) {
        if (prefsManager == null) prefsManager = com.tibarra.gymhelper.util.PreferencesManager(context)
        if (wearSyncManager != null) return
        wearSyncManager = WearSyncManager(context)
        
        viewModelScope.launch {
            _uiState.onEach { state ->
                wearSyncManager?.syncState(state.toSharedState(prefsManager))
            }.collect()
        }

        viewModelScope.launch {
            WearCommandEventBus.commands.collect { command ->
                handleWearCommand(command, context)
            }
        }

        viewModelScope.launch {
            com.tibarra.gymhelper.util.SystemCommandEventBus.commands.collect { command ->
                if (command == com.tibarra.gymhelper.util.SystemCommandEventBus.CMD_SKIP_REST) {
                    skipRest(context)
                }
            }
        }
    }

    private fun handleWearCommand(command: String, context: Context) {
        val parts = command.split("/")
        val cmd = parts[0]
        val payload = parts.getOrNull(1)

        when (cmd) {
            SyncUtils.CMD_TOGGLE_SET -> {
                payload?.split(",")?.let {
                    val exIndex = it.getOrNull(0)?.toIntOrNull()
                    val setIndex = it.getOrNull(1)?.toIntOrNull()
                    if ((exIndex != null) && (setIndex != null)) {
                        toggleSet(exIndex, setIndex, context)
                    }
                }
            }
            SyncUtils.CMD_SKIP_REST -> skipRest(context)
            SyncUtils.CMD_START_WARMUP -> startWarmup(context)
            SyncUtils.CMD_STOP_WARMUP -> stopWarmup(context)
            SyncUtils.CMD_START_CARDIO -> startCardio(context)
            SyncUtils.CMD_STOP_CARDIO -> {
                val markAsFinished = payload == "finish"
                stopCardio(context, markAsFinished)
            }
        }
    }

    private fun cancelAllTimers() {
        restTimerJob?.cancel()
        cardioTimerJob?.cancel()
        warmupTimerJob?.cancel()
        _uiState.update { it.copy(isResting = false, isCardioActive = false, isWarmupActive = false) }
    }

    fun startSession(workoutId: Long) {
        if (_uiState.value.isStarted && _uiState.value.workout?.id == workoutId) return
        
        viewModelScope.launch {
            val workout = dao.getAllWorkouts().first().find { it.id == workoutId } ?: return@launch
            val exercises = dao.getExercisesForWorkout(workoutId).first()
            
            val exercisesWithState = exercises.map { exercise ->
                val variants = dao.getVariantsForExercise(exercise.id).first()
                val selected = variants.firstOrNull()
                ExerciseSessionState(
                    exercise = exercise,
                    variants = variants,
                    selectedVariant = selected,
                    sets = generateSets(selected)
                )
            }

            _uiState.value = SessionUiState(
                workout = workout,
                exercises = exercisesWithState,
                warmupTargetSeconds = workout.warmupDurationMinutes * 60,
                cardioTargetSeconds = workout.cardioDurationMinutes * 60
            )
        }
    }

    fun startWorkout(context: Context? = null) {
        startTimeMillis = System.currentTimeMillis()
        lastActionTimestamp = startTimeMillis
        _uiState.update { it.copy(isStarted = true) }
        startSessionTimer()

        // Persist active workout ID
        val workoutId = _uiState.value.workout?.id ?: -1L
        context?.let {
            if (prefsManager == null) prefsManager = com.tibarra.gymhelper.util.PreferencesManager(it)
            prefsManager?.activeWorkoutId = workoutId

            val intent = Intent(it, RestTimerService::class.java).apply {
                action = RestTimerService.ACTION_START_SESSION
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                it.startForegroundService(intent)
            } else {
                it.startService(intent)
            }
        }

        val state = _uiState.value
        if ((state.workout?.warmupDurationMinutes ?: 0) > 0) {
            startWarmup(context)
        } else if (state.exercises.isEmpty() && ((state.workout?.cardioDurationMinutes ?: 0) > 0)) {
            startCardio(context)
        }
    }

    private fun generateSets(variant: ExerciseVariantEntity?): List<SetActiveState> {
        if (variant == null) return emptyList()
        val sets = mutableListOf<SetActiveState>()
        for (i in 1..variant.defaultSetsCount) {
            sets.add(SetActiveState(setNumber = i, weight = variant.currentWeight, reps = variant.defaultRepsCount))
        }
        if (variant.hasDropSet) {
            sets.add(SetActiveState(setNumber = variant.defaultSetsCount + 1, weight = variant.currentWeight / 2, reps = variant.defaultRepsCount, isDropSet = true))
        }
        return sets
    }

    private fun startSessionTimer() {
        sessionTimerJob?.cancel()
        sessionTimerJob = viewModelScope.launch {
            while (true) {
                delay(1.seconds)
                _uiState.update { it.copy(totalSessionTimeSeconds = it.totalSessionTimeSeconds + 1) }
            }
        }
    }

    fun selectVariant(exIndex: Int, variant: ExerciseVariantEntity) {
        val exercise = _uiState.value.exercises[exIndex]
        if (exercise.sets.any { it.isCompleted }) return

        _uiState.update { state ->
            val newExercises = state.exercises.toMutableList()
            newExercises[exIndex] = newExercises[exIndex].copy(
                selectedVariant = variant,
                sets = generateSets(variant)
            )
            state.copy(exercises = newExercises)
        }
    }

    fun toggleSet(exIndex: Int, setIndex: Int, context: Context? = null) {
        val now = System.currentTimeMillis()
        val duration = ((now - lastActionTimestamp) / 1000).toInt()
        
        val currentState = _uiState.value
        val exerciseState = currentState.exercises[exIndex]
        val set = exerciseState.sets[setIndex]
        val isNowCompleted = !set.isCompleted

        val newSets = exerciseState.sets.toMutableList()
        newSets[setIndex] = set.copy(
            isCompleted = isNowCompleted,
            durationSeconds = if (isNowCompleted) duration else 0
        )

        val newExercises = currentState.exercises.toMutableList()
        newExercises[exIndex] = exerciseState.copy(sets = newSets, isStarted = true)

        _uiState.update { state -> 
            state.copy(
                exercises = newExercises,
                strengthStartTime = if (state.strengthStartTime == 0L) now else state.strengthStartTime,
                strengthEndTime = if (isNowCompleted) now else state.strengthEndTime
            )
        }

        if (isNowCompleted) {
            val isLastSetOfExercise = (setIndex == (exerciseState.sets.size - 1))
            val nextSetIsDropset = !isLastSetOfExercise && exerciseState.sets[setIndex + 1].isDropSet
            
            val restTime = if (nextSetIsDropset) {
                10 // Forced 10s rest before dropset
            } else if (isLastSetOfExercise) {
                exerciseState.exercise.restAfterExerciseSeconds
            } else {
                exerciseState.exercise.restBetweenSetsSeconds
            }
            startRestTimer(restTime, context)
        } else {
            stopRestTimer(context)
        }
        lastActionTimestamp = now
    }
    
    fun updateSetValues(exIndex: Int, setIndex: Int, weight: Double, reps: Int) {
        _uiState.update { state ->
            val newExercises = state.exercises.toMutableList()
            val exercise = newExercises[exIndex]
            val newSets = exercise.sets.toMutableList()
            newSets[setIndex] = newSets[setIndex].copy(weight = weight, reps = reps)
            newExercises[exIndex] = exercise.copy(sets = newSets)
            state.copy(exercises = newExercises)
        }
    }

    fun updateVariantNotes(exIndex: Int, newNotes: String) {
        val currentState = _uiState.value
        val exerciseState = currentState.exercises.getOrNull(exIndex)
        val variant = exerciseState?.selectedVariant ?: return

        viewModelScope.launch {
            // 1. Update Database
            val updatedVariant = variant.copy(notes = newNotes)
            dao.updateVariant(updatedVariant)

            // 2. Update UI State
            _uiState.update { state ->
                val newExercises = state.exercises.toMutableList()
                newExercises[exIndex] = exerciseState.copy(
                    selectedVariant = updatedVariant
                )
                state.copy(exercises = newExercises)
            }
        }
    }

    private fun startRestTimer(seconds: Int, context: Context?) {
        cancelAllTimers()
        if (seconds <= 0) {
            _uiState.update { it.copy(isResting = false, restTimeLeft = 0) }
            lastActionTimestamp = System.currentTimeMillis()
            return
        }
        
        _uiState.update { it.copy(isResting = true, restTimeLeft = seconds) }
        
        // Start foreground service
        context?.let {
            val intent = Intent(it, RestTimerService::class.java).apply {
                action = RestTimerService.ACTION_START_REST
                putExtra(RestTimerService.EXTRA_SECONDS, seconds)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                it.startForegroundService(intent)
            } else {
                it.startService(intent)
            }
        }

        restTimerJob = viewModelScope.launch {
            while (_uiState.value.restTimeLeft > 0) {
                delay(1.seconds)
                val newTime = (_uiState.value.restTimeLeft - 1).coerceAtLeast(0)
                _uiState.update { it.copy(restTimeLeft = newTime, totalRestSeconds = it.totalRestSeconds + 1) }
            }
            _uiState.update { it.copy(isResting = false) }
            lastActionTimestamp = System.currentTimeMillis()
        }
    }

    private fun stopRestTimer(context: Context?) {
        cancelAllTimers()
        _uiState.update { it.copy(restTimeLeft = 0) }
        context?.let {
            val intent = Intent(it, RestTimerService::class.java).apply {
                action = RestTimerService.ACTION_STOP_REST
            }
            it.startService(intent)
        }
    }

    fun adjustRestTime(seconds: Int, context: Context? = null) {
        _uiState.update { 
            val newTime = (it.restTimeLeft + seconds).coerceAtLeast(0)
            
            // Notify service if context is available
            context?.let { ctx ->
                val intent = Intent(ctx, RestTimerService::class.java).apply {
                    action = RestTimerService.ACTION_UPDATE_REST
                    putExtra(RestTimerService.EXTRA_SECONDS, newTime)
                }
                ctx.startService(intent)
            }
            
            it.copy(restTimeLeft = newTime)
        }
    }

    fun skipRest(context: Context?) {
        stopRestTimer(context)
        lastActionTimestamp = System.currentTimeMillis()
    }

    fun startWarmup(context: Context? = null) {
        cancelAllTimers()
        val state = _uiState.value
        val totalSeconds = (state.workout?.warmupDurationMinutes ?: 0) * 60
        val endTimestamp = System.currentTimeMillis() + (totalSeconds * 1000L)
        
        val remaining = if (state.warmupTimeSeconds > 0) totalSeconds - state.warmupTimeSeconds else totalSeconds
        _uiState.update { it.copy(isWarmupActive = true, warmupEndTimestamp = endTimestamp) }
        
        context?.let {
            val intent = Intent(it, RestTimerService::class.java).apply {
                action = RestTimerService.ACTION_START_WARMUP
                putExtra(RestTimerService.EXTRA_SECONDS, totalSeconds)
                putExtra("EXTRA_REMAINING", remaining)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                it.startForegroundService(intent)
            } else {
                it.startService(intent)
            }
        }

        warmupTimerJob = viewModelScope.launch {
            while (true) {
                delay(1.seconds)
                _uiState.update { it.copy(warmupTimeSeconds = it.warmupTimeSeconds + 1) }
            }
        }
    }

    fun stopWarmup(context: Context? = null) {
        cancelAllTimers()
        
        context?.let {
            val intent = Intent(it, RestTimerService::class.java).apply {
                action = RestTimerService.ACTION_STOP_WARMUP
            }
            it.startService(intent)
        }
    }

    fun startCardio(context: Context? = null) {
        cancelAllTimers()
        val now = System.currentTimeMillis()
        val state = _uiState.value
        val totalSeconds = (state.workout?.cardioDurationMinutes ?: 0) * 60
        val endTimestamp = now + (totalSeconds * 1000L)
        
        val remaining = if (state.cardioTimeSeconds > 0) totalSeconds - state.cardioTimeSeconds else totalSeconds
        _uiState.update { it.copy(isCardioActive = true, cardioStartTime = if (it.cardioStartTime == 0L) now else it.cardioStartTime, cardioEndTimestamp = endTimestamp) }
        
        context?.let {
            val intent = Intent(it, RestTimerService::class.java).apply {
                action = RestTimerService.ACTION_START_CARDIO
                putExtra(RestTimerService.EXTRA_SECONDS, totalSeconds)
                putExtra("EXTRA_REMAINING", remaining)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                it.startForegroundService(intent)
            } else {
                it.startService(intent)
            }
        }

        cardioTimerJob = viewModelScope.launch {
            while (true) {
                delay(1.seconds)
                _uiState.update { it.copy(cardioTimeSeconds = it.cardioTimeSeconds + 1) }
            }
        }
    }
    
    fun stopCardio(context: Context? = null, markAsFinished: Boolean = false) {
        cancelAllTimers()
        val now = System.currentTimeMillis()
        _uiState.update { it.copy(cardioEndTime = now, isCardioFinished = it.isCardioFinished || markAsFinished) }
        
        context?.let {
            val intent = Intent(it, RestTimerService::class.java).apply {
                action = RestTimerService.ACTION_STOP_CARDIO
            }
            it.startService(intent)
        }
    }

    fun showSummary() {
        val state = _uiState.value
        val now = System.currentTimeMillis()
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        
        val strengthDurationSecs = if (state.strengthEndTime > state.strengthStartTime) 
            ((state.strengthEndTime - state.strengthStartTime) / 1000).toInt() 
        else 0
        
        var totalReps = 0
        var totalVolume = 0.0
        state.exercises.forEach { ex ->
            ex.sets.forEach { s ->
                if (s.isCompleted) {
                    totalReps += s.reps
                    totalVolume += s.reps * s.weight
                }
            }
        }

        val summary = SessionSummary(
            date = dateFormat.format(Date(now)),
            startTime = timeFormat.format(Date(startTimeMillis)),
            endTime = timeFormat.format(Date(now)),
            totalGymDuration = TimeUtils.formatHoursMinutes(state.totalSessionTimeSeconds),
            warmupDuration = TimeUtils.formatTime(state.warmupTimeSeconds),
            strengthDuration = TimeUtils.formatTime(strengthDurationSecs),
            cardioDuration = TimeUtils.formatTime(state.cardioTimeSeconds),
            restDuration = TimeUtils.formatTime(state.totalRestSeconds),
            totalReps = totalReps,
            totalVolume = totalVolume
        )
        
        _uiState.update { it.copy(showSummary = true, summary = summary) }
    }

    fun setEffortRating(rating: Int) {
        _uiState.update { it.copy(effortRating = rating) }
    }

    fun discardSession(context: Context? = null) {
        context?.let { 
            endSessionCleanup(it) 
            if (prefsManager == null) prefsManager = com.tibarra.gymhelper.util.PreferencesManager(it)
            prefsManager?.activeWorkoutId = -1L
        } ?: cancelAllTimers()
        _uiState.update { it.copy(isFinished = true) }
    }

    fun endSessionCleanup(context: Context) {
        cancelAllTimers()
        sessionTimerJob?.cancel()
        
        // Notify Wear OS that session is over
        _uiState.update { it.copy(isStarted = false, isFinished = true, workout = null, exercises = emptyList()) }

        // Stop the foreground service completely
        val intent = Intent(context, RestTimerService::class.java).apply {
            action = RestTimerService.ACTION_STOP_SESSION
        }
        context.startService(intent)
    }

    fun confirmSaveSession(context: Context? = null) {
        val state = _uiState.value
        val workout = state.workout ?: return
        
        context?.let { endSessionCleanup(it) } ?: cancelAllTimers()

        viewModelScope.launch {
            val endTime = System.currentTimeMillis()
            
            // Clear active workout ID
            context?.let {
                if (prefsManager == null) prefsManager = com.tibarra.gymhelper.util.PreferencesManager(it)
                prefsManager?.activeWorkoutId = -1L
            }

            val sessionId = dao.insertSession(
                SessionHistoryEntity(
                    workoutId = workout.id,
                    workoutName = workout.name,
                    startTime = startTimeMillis,
                    endTime = endTime,
                    cardioStartTime = state.cardioStartTime,
                    cardioEndTime = state.cardioEndTime,
                    cardioDurationSeconds = state.cardioTimeSeconds,
                    warmupDurationSeconds = state.warmupTimeSeconds,
                    totalRestSeconds = state.totalRestSeconds,
                    strengthStartTime = state.strengthStartTime,
                    strengthEndTime = state.strengthEndTime,
                    totalReps = state.summary?.totalReps ?: 0,
                    totalVolume = state.summary?.totalVolume ?: 0.0,
                    effortRating = state.effortRating
                )
            )

            state.exercises.forEach { exerciseState ->
                exerciseState.sets.forEach { set ->
                    if (set.isCompleted) {
                        dao.insertSetLog(
                            SetLogEntity(
                                sessionHistoryId = sessionId,
                                exerciseId = exerciseState.exercise.id,
                                exerciseName = "${exerciseState.exercise.name} (${exerciseState.selectedVariant?.name})",
                                setNumber = set.setNumber,
                                reps = set.reps,
                                weight = set.weight,
                                isDropSet = set.isDropSet,
                                durationSeconds = set.durationSeconds
                            )
                        )
                        
                        if (!set.isDropSet) {
                            exerciseState.selectedVariant?.let { variant ->
                                if (set.weight != variant.currentWeight) {
                                    dao.updateVariant(variant.copy(currentWeight = set.weight))
                                }
                            }
                        }
                    }
                }
            }
            
            _uiState.update { it.copy(isFinished = true) }
        }
    }

    suspend fun getHistoryBackupData(): List<CsvManager.HistoryBundle> {
        val sessions = dao.getSessionHistorySync()
        return sessions.map { session ->
            val logs = dao.getLogsForSessionSync(session.id)
            CsvManager.HistoryBundle(session, logs)
        }
    }

    fun importHistoryFromCsv(csv: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            when (val result = CsvManager.importHistory(csv)) {
                is CsvManager.ValidationResult.Success -> {
                    val bundles = result.data
                    if (bundles.isNotEmpty()) {
                        dao.clearHistory()
                        bundles.forEach { bundle ->
                            val sessionId = dao.insertSession(bundle.session.copy(id = 0))
                            bundle.logs.forEach { log ->
                                dao.insertSetLog(log.copy(id = 0, sessionHistoryId = sessionId))
                            }
                        }
                        onResult(null)
                    } else {
                        onResult("The file contains no history data.")
                    }
                }
                is CsvManager.ValidationResult.Error -> {
                    onResult(result.message)
                }
            }
        }
    }

    override fun onCleared() {
        toneGenerator.release()
    }
}

private fun SessionUiState.toSharedState(prefs: com.tibarra.gymhelper.util.PreferencesManager?): com.tibarra.gymhelper.shared.model.SessionUiState {
    val activeExerciseIndex = exercises.indexOfFirst { ex ->
        ex.sets.any { it.isCompleted } && !ex.sets.all { it.isCompleted }
    }

    return com.tibarra.gymhelper.shared.model.SessionUiState(
        workoutName = workout?.name ?: "",
        exercises = exercises.mapIndexed { index, ex ->
            com.tibarra.gymhelper.shared.model.ExerciseState(
                index = index + 1,
                name = ex.exercise.name,
                variantName = ex.selectedVariant?.name ?: "",
                variantNotes = ex.selectedVariant?.notes ?: "",
                sets = ex.sets.map { s ->
                    com.tibarra.gymhelper.shared.model.SetState(
                        setNumber = s.setNumber,
                        weight = s.weight,
                        reps = s.reps,
                        isCompleted = s.isCompleted,
                        isDropSet = s.isDropSet
                    )
                },
                isCompleted = ex.sets.isNotEmpty() && ex.sets.all { it.isCompleted },
                isInteractionAllowed = activeExerciseIndex == -1 || activeExerciseIndex == index
            )
        },
        isResting = isResting,
        restTimeLeft = restTimeLeft,
        totalRestSeconds = totalRestSeconds,
        totalSessionTimeSeconds = totalSessionTimeSeconds,
        warmupEndTimestamp = warmupEndTimestamp,
        isWarmupActive = isWarmupActive,
        cardioEndTimestamp = cardioEndTimestamp,
        isCardioActive = isCardioActive,
        isCardioFinished = isCardioFinished,
        isFinished = isFinished,
        isStarted = isStarted,
        accentColorIndex = prefs?.accentColorIndex ?: 0,
        themeMode = prefs?.themeMode ?: 0
    )
}
