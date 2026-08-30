package com.tibarra.gymhelper.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tibarra.gymhelper.data.dao.GymDao
import com.tibarra.gymhelper.data.model.*

@Database(
    entities = [
        WorkoutEntity::class,
        ExerciseEntity::class,
        ExerciseVariantEntity::class,
        SessionHistoryEntity::class,
        SetLogEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class GymDatabase : RoomDatabase() {
    abstract fun gymDao(): GymDao

    companion object {
        @Volatile
        private var INSTANCE: GymDatabase? = null

        fun getDatabase(context: Context): GymDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GymDatabase::class.java,
                    "gym_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
