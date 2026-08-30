package com.tibarra.gymhelper.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session_history")
data class SessionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val workoutName: String, // Denormalized for history
    val startTime: Long,
    val endTime: Long,
    val cardioStartTime: Long = 0,
    val cardioEndTime: Long = 0,
    val cardioDurationSeconds: Int = 0,
    val warmupDurationSeconds: Int = 0,
    val totalRestSeconds: Int = 0,
    val strengthStartTime: Long = 0,
    val strengthEndTime: Long = 0,
    val totalReps: Int = 0,
    val totalVolume: Double = 0.0,
    val effortRating: Int = 0 // 1 to 5
)
