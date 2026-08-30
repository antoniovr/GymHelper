package com.tibarra.gymhelper.shared.model

data class SessionUiState(
    val workoutName: String = "",
    val exercises: List<ExerciseState> = emptyList(),
    val isResting: Boolean = false,
    val restTimeLeft: Int = 0,
    val totalRestSeconds: Int = 0,
    val totalSessionTimeSeconds: Int = 0,
    val warmupEndTimestamp: Long = 0,
    val isWarmupActive: Boolean = false,
    val cardioEndTimestamp: Long = 0,
    val isCardioActive: Boolean = false,
    val isCardioFinished: Boolean = false,
    val isFinished: Boolean = false,
    val isStarted: Boolean = false,
    val accentColorIndex: Int = 0,
    val themeMode: Int = 0
)

data class ExerciseState(
    val index: Int,
    val name: String,
    val variantName: String,
    val variantNotes: String,
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
