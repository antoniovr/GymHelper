package com.tibarra.gymhelper.shared.model

data class SessionUiState(
    val workoutName: String = "",
    val exercises: List<ExerciseState> = emptyList(),
    val isResting: Boolean = false,
    val restTimeLeft: Int = 0,
    val totalRestSeconds: Int = 0,
    val totalSessionTimeSeconds: Int = 0,
    val warmupTimeSeconds: Int = 0,
    val warmupTargetSeconds: Int = 0,
    val isWarmupActive: Boolean = false,
    val cardioTimeSeconds: Int = 0,
    val cardioTargetSeconds: Int = 0,
    val isCardioActive: Boolean = false,
    val isCardioFinished: Boolean = false,
    val isFinished: Boolean = false,
    val isStarted: Boolean = false,
    val sessionStartTimeMillis: Long = 0L,
    val accentColorIndex: Int = 0,
    val themeMode: Int = 0
)

data class ExerciseState(
    val index: Int,
    val name: String,
    val variantName: String,
    val variantNotes: String,
    val initialWeight: Double = 0.0,
    val initialWeightDate: Long = 0L,
    val sets: List<SetState> = emptyList(),
    val isCompleted: Boolean = false,
    val isInteractionAllowed: Boolean = true
)

data class SetState(
    val setNumber: Int,
    val weight: Double,
    val reps: Int,
    val isCompleted: Boolean,
    val isDropSet: Boolean
)
