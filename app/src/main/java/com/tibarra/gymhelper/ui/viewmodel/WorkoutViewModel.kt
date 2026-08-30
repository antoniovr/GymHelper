package com.tibarra.gymhelper.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tibarra.gymhelper.data.dao.GymDao
import com.tibarra.gymhelper.data.model.*
import com.tibarra.gymhelper.util.CsvManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WorkoutViewModel(private val dao: GymDao) : ViewModel() {

    // Reactive workouts flow directly from the DB
    val workouts: StateFlow<List<WorkoutEntity>> = dao.getAllWorkouts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    private val _currentWorkout = MutableStateFlow<WorkoutEntity?>(null)
    val currentWorkout: StateFlow<WorkoutEntity?> = _currentWorkout.asStateFlow()

    private val _exercises = MutableStateFlow<List<ExerciseWithVariants>>(emptyList())
    val exercises: StateFlow<List<ExerciseWithVariants>> = _exercises.asStateFlow()

    // Reactive exercise counts map
    val exercisesCountMap: StateFlow<Map<Long, Int>> = workouts
        .map { list ->
            val counts = mutableMapOf<Long, Int>()
            list.forEach { workout ->
                counts[workout.id] = dao.getExercisesForWorkoutSync(workout.id).size
            }
            counts
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap(),
        )

    data class ExerciseWithVariants(
        val exercise: ExerciseEntity,
        val variants: List<ExerciseVariantEntity>,
    )

    data class ExerciseSource(
        val workoutName: String,
        val exercise: ExerciseEntity,
        val variants: List<ExerciseVariantEntity>
    )

    fun loadWorkoutDetails(workoutId: Long) {
        viewModelScope.launch {
            workouts.map { list -> list.find { it.id == workoutId } }.collect { workout ->
                _currentWorkout.value = workout
                workout?.let { loadExercises(it.id) }
            }
        }
    }

    private fun loadExercises(workoutId: Long) {
        viewModelScope.launch {
            dao.getExercisesForWorkout(workoutId).collect { exerciseList ->
                val detailedList = exerciseList.map { exercise ->
                    val variants = dao.getVariantsForExercise(exercise.id).first()
                    ExerciseWithVariants(exercise, variants)
                }
                _exercises.value = detailedList
            }
        }
    }

    fun addWorkout(name: String, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val currentWorkouts = dao.getAllWorkoutsSync()
            val newId = dao.insertWorkout(WorkoutEntity(name = name, sequenceOrder = currentWorkouts.size))
            onCreated(newId)
        }
    }

    fun updateWorkoutCardio(workout: WorkoutEntity, duration: Int, type: String) {
        viewModelScope.launch {
            dao.updateWorkout(workout.copy(cardioDurationMinutes = duration, cardioType = type))
        }
    }

    fun updateWorkoutWarmup(workout: WorkoutEntity, duration: Int) {
        viewModelScope.launch {
            dao.updateWorkout(workout.copy(warmupDurationMinutes = duration))
        }
    }

    fun updateWorkoutName(workout: WorkoutEntity, name: String) {
        viewModelScope.launch {
            dao.updateWorkout(workout.copy(name = name))
        }
    }

    fun updateExerciseName(exercise: ExerciseEntity, name: String) {
        viewModelScope.launch {
            dao.updateExercise(exercise.copy(name = name))
            _currentWorkout.value?.id?.let { loadExercises(it) }
        }
    }

    fun addExercise(workoutId: Long, name: String) {
        viewModelScope.launch {
            val currentExercises = dao.getExercisesForWorkoutSync(workoutId)
            dao.insertExercise(
                ExerciseEntity(
                    workoutId = workoutId,
                    name = name,
                    sequenceOrder = currentExercises.size
                )
            )
        }
    }

    fun addVariant(exerciseId: Long, name: String, weight: Double, sets: Int, reps: Int, hasDrop: Boolean, date: Long, notes: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            dao.insertVariant(
                ExerciseVariantEntity(
                    exerciseId = exerciseId,
                    name = name,
                    notes = notes,
                    initialWeight = weight,
                    initialWeightDate = date,
                    currentWeight = weight,
                    defaultSetsCount = sets,
                    defaultRepsCount = reps,
                    hasDropSet = hasDrop
                )
            )
            _currentWorkout.value?.id?.let { loadExercises(it) }
        }
    }
    
    fun updateVariant(variant: ExerciseVariantEntity) {
        viewModelScope.launch {
            dao.updateVariant(variant)
            _currentWorkout.value?.id?.let { loadExercises(it) }
        }
    }

    fun deleteVariant(variant: ExerciseVariantEntity) {
        viewModelScope.launch {
            dao.deleteVariant(variant)
            _currentWorkout.value?.id?.let { loadExercises(it) }
        }
    }

    fun deleteExercise(exercise: ExerciseEntity) {
        viewModelScope.launch {
            dao.deleteExercise(exercise)
            // Renormalize sequence order for remaining exercises in this workout
            val remaining = dao.getExercisesForWorkoutSync(exercise.workoutId)
            remaining.forEachIndexed { index, ex ->
                if (ex.sequenceOrder != index) {
                    dao.updateExercise(ex.copy(sequenceOrder = index))
                }
            }
        }
    }
    
    fun deleteWorkout(workout: WorkoutEntity) {
        viewModelScope.launch {
            dao.deleteWorkout(workout)
            normalizeWorkoutSequence()
        }
    }

    private suspend fun normalizeWorkoutSequence() {
        val all = dao.getAllWorkoutsSync()
        all.forEachIndexed { index, w ->
            if (w.sequenceOrder != index) {
                dao.updateWorkout(w.copy(sequenceOrder = index))
            }
        }
    }

    fun moveWorkoutUp(workout: WorkoutEntity) {
        viewModelScope.launch {
            normalizeWorkoutSequence() // Ensure clean indices before swap
            val list = dao.getAllWorkoutsSync()
            val index = list.indexOfFirst { it.id == workout.id }
            if (index > 0) {
                val current = list[index]
                val other = list[index - 1]
                dao.updateWorkout(current.copy(sequenceOrder = index - 1))
                dao.updateWorkout(other.copy(sequenceOrder = index))
            }
        }
    }

    fun moveWorkoutDown(workout: WorkoutEntity) {
        viewModelScope.launch {
            normalizeWorkoutSequence() // Ensure clean indices before swap
            val list = dao.getAllWorkoutsSync()
            val index = list.indexOfFirst { it.id == workout.id }
            if ((index != -1) && (index < (list.size - 1))) {
                val current = list[index]
                val other = list[index + 1]
                dao.updateWorkout(current.copy(sequenceOrder = index + 1))
                dao.updateWorkout(other.copy(sequenceOrder = index))
            }
        }
    }

    private suspend fun normalizeExerciseSequence(workoutId: Long) {
        val all = dao.getExercisesForWorkoutSync(workoutId)
        all.forEachIndexed { index, ex ->
            if (ex.sequenceOrder != index) {
                dao.updateExercise(ex.copy(sequenceOrder = index))
            }
        }
    }

    fun moveExerciseUp(exercise: ExerciseEntity) {
        viewModelScope.launch {
            normalizeExerciseSequence(exercise.workoutId)
            val list = dao.getExercisesForWorkoutSync(exercise.workoutId)
            val index = list.indexOfFirst { it.id == exercise.id }
            if (index > 0) {
                val current = list[index]
                val other = list[index - 1]
                dao.updateExercise(current.copy(sequenceOrder = index - 1))
                dao.updateExercise(other.copy(sequenceOrder = index))
                loadExercises(exercise.workoutId)
            }
        }
    }

    fun moveExerciseDown(exercise: ExerciseEntity) {
        viewModelScope.launch {
            normalizeExerciseSequence(exercise.workoutId)
            val list = dao.getExercisesForWorkoutSync(exercise.workoutId)
            val index = list.indexOfFirst { it.id == exercise.id }
            if ((index != -1) && (index < (list.size - 1))) {
                val current = list[index]
                val other = list[index + 1]
                dao.updateExercise(current.copy(sequenceOrder = index + 1))
                dao.updateExercise(other.copy(sequenceOrder = index))
                loadExercises(exercise.workoutId)
            }
        }
    }
    
    fun updateRestBetweenSets(exerciseId: Long, seconds: Int) {
        viewModelScope.launch {
            dao.updateRestBetweenSets(exerciseId, seconds)
        }
    }

    fun updateRestAfterExercise(exerciseId: Long, seconds: Int) {
        viewModelScope.launch {
            dao.updateRestAfterExercise(exerciseId, seconds)
        }
    }

    suspend fun getWorkoutsBackupData(): List<CsvManager.WorkoutBundle> {
        val workouts = dao.getAllWorkoutsSync()
        return workouts.map { workout ->
            val exercises = dao.getExercisesForWorkoutSync(workout.id)
            val exBundles = exercises.map { exercise ->
                val variants = dao.getVariantsForExerciseSync(exercise.id)
                CsvManager.ExerciseBundle(exercise, variants)
            }
            CsvManager.WorkoutBundle(workout, exBundles)
        }
    }

    fun importWorkoutsFromCsv(csv: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            when (val result = CsvManager.importWorkouts(csv)) {
                is CsvManager.ValidationResult.Success -> {
                    val bundles = result.data
                    if (bundles.isNotEmpty()) {
                        dao.clearAllWorkouts()
                        bundles.forEachIndexed { index, bundle ->
                            val workoutId = dao.insertWorkout(bundle.workout.copy(id = 0, sequenceOrder = index))
                            bundle.exercises.forEach { exBundle ->
                                val exerciseId = dao.insertExercise(exBundle.exercise.copy(id = 0, workoutId = workoutId))
                                exBundle.variants.forEach { variant ->
                                    dao.insertVariant(variant.copy(id = 0, exerciseId = exerciseId))
                                }
                            }
                        }
                        onResult(null)
                    } else {
                        onResult("The file contains no workout data.")
                    }
                }
                is CsvManager.ValidationResult.Error -> {
                    onResult(result.message)
                }
            }
        }
    }

    fun cleanupEmptyExercises(workoutId: Long) {
        viewModelScope.launch {
            val exercises = dao.getExercisesForWorkout(workoutId).first()
            exercises.forEach { exercise ->
                val variants = dao.getVariantsForExercise(exercise.id).first()
                if (variants.isEmpty()) {
                    deleteExercise(exercise)
                }
            }
        }
    }

    suspend fun getExercisesAvailableToImport(excludeWorkoutId: Long): List<ExerciseSource> {
        val allWorkouts = dao.getAllWorkoutsSync()
        val result = mutableListOf<ExerciseSource>()
        allWorkouts.filter { it.id != excludeWorkoutId }.forEach { workout ->
            val exercises = dao.getExercisesForWorkoutSync(workout.id)
            exercises.forEach { ex ->
                val variants = dao.getVariantsForExerciseSync(ex.id)
                result.add(ExerciseSource(workout.name, ex, variants))
            }
        }
        return result
    }

    fun importExercises(targetWorkoutId: Long, sources: List<ExerciseSource>) {
        viewModelScope.launch {
            val currentExercises = dao.getExercisesForWorkoutSync(targetWorkoutId)
            var currentSize = currentExercises.size
            
            sources.forEach { source ->
                val newExerciseId = dao.insertExercise(
                    source.exercise.copy(
                        id = 0,
                        workoutId = targetWorkoutId,
                        sequenceOrder = currentSize++
                    )
                )
                source.variants.forEach { variant ->
                    dao.insertVariant(variant.copy(id = 0, exerciseId = newExerciseId))
                }
            }
            loadExercises(targetWorkoutId)
        }
    }
}
