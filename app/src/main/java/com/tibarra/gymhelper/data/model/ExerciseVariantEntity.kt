package com.tibarra.gymhelper.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercise_variants",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("exerciseId")]
)
data class ExerciseVariantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: Long,
    val name: String, // e.g., "Smith Machine", "Dumbbells"
    val notes: String = "",
    val initialWeight: Double = 0.0,
    val initialWeightDate: Long = System.currentTimeMillis(),
    val currentWeight: Double = 0.0,
    val defaultSetsCount: Int = 3,
    val defaultRepsCount: Int = 10,
    val hasDropSet: Boolean = false
)
