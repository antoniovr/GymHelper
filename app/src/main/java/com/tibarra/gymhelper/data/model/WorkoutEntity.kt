package com.tibarra.gymhelper.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val cardioDurationMinutes: Int = 0,
    val cardioType: String = "",
    val warmupDurationMinutes: Int = 0,
    val restBetweenExercisesSeconds: Int = 60,
    val restAfterExerciseSeconds: Int = 90,
    val sequenceOrder: Int = 0
)
