package com.tibarra.gymhelper.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "set_logs",
    foreignKeys = [
        ForeignKey(
            entity = SessionHistoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionHistoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionHistoryId")]
)
data class SetLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionHistoryId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val setNumber: Int,
    val reps: Int,
    val weight: Double,
    val isCompleted: Boolean = true,
    val isDropSet: Boolean = false,
    val durationSeconds: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
