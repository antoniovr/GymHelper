package com.tibarra.gymhelper.data.dao

import androidx.room.*
import com.tibarra.gymhelper.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GymDao {
    // Workouts
    @Query("SELECT * FROM workouts ORDER BY sequenceOrder ASC")
    fun getAllWorkouts(): Flow<List<WorkoutEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Delete
    suspend fun deleteWorkout(workout: WorkoutEntity)

    @Update
    suspend fun updateWorkout(workout: WorkoutEntity)

    @Query("SELECT * FROM workouts ORDER BY sequenceOrder ASC")
    suspend fun getAllWorkoutsSync(): List<WorkoutEntity>

    @Query("DELETE FROM workouts")
    suspend fun clearAllWorkouts()

    // Exercises
    @Query("SELECT * FROM exercises WHERE workoutId = :workoutId ORDER BY sequenceOrder ASC")
    fun getExercisesForWorkout(workoutId: Long): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE workoutId = :workoutId ORDER BY sequenceOrder ASC")
    suspend fun getExercisesForWorkoutSync(workoutId: Long): List<ExerciseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity): Long

    @Update
    suspend fun updateExercise(exercise: ExerciseEntity)

    @Query("UPDATE exercises SET restBetweenSetsSeconds = :seconds WHERE id = :id")
    suspend fun updateRestBetweenSets(id: Long, seconds: Int)

    @Query("UPDATE exercises SET restAfterExerciseSeconds = :seconds WHERE id = :id")
    suspend fun updateRestAfterExercise(id: Long, seconds: Int)

    @Delete
    suspend fun deleteExercise(exercise: ExerciseEntity)

    // Variants
    @Query("SELECT * FROM exercise_variants WHERE exerciseId = :exerciseId")
    fun getVariantsForExercise(exerciseId: Long): Flow<List<ExerciseVariantEntity>>

    @Query("SELECT * FROM exercise_variants WHERE exerciseId = :exerciseId")
    suspend fun getVariantsForExerciseSync(exerciseId: Long): List<ExerciseVariantEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVariant(variant: ExerciseVariantEntity): Long

    @Update
    suspend fun updateVariant(variant: ExerciseVariantEntity)

    @Delete
    suspend fun deleteVariant(variant: ExerciseVariantEntity)

    // Session History
    @Query("SELECT * FROM session_history ORDER BY endTime DESC")
    fun getSessionHistory(): Flow<List<SessionHistoryEntity>>

    @Query("SELECT * FROM session_history")
    suspend fun getSessionHistorySync(): List<SessionHistoryEntity>

    @Query("DELETE FROM session_history")
    suspend fun clearHistory()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionHistoryEntity): Long

    @Update
    suspend fun updateSession(session: SessionHistoryEntity)

    @Delete
    suspend fun deleteSession(session: SessionHistoryEntity)

    // Set Logs
    @Query("SELECT * FROM set_logs WHERE sessionHistoryId = :sessionId")
    fun getLogsForSession(sessionId: Long): Flow<List<SetLogEntity>>

    @Query("SELECT * FROM set_logs WHERE sessionHistoryId = :sessionId")
    suspend fun getLogsForSessionSync(sessionId: Long): List<SetLogEntity>

    @Insert
    suspend fun insertSetLog(log: SetLogEntity)
}
