package com.tibarra.gymhelper.util

import com.tibarra.gymhelper.data.model.*
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*

object CsvManager {

    private const val WORKOUT_HEADER = "WorkoutName,CardioType,CardioDuration,WarmupDuration,ExName,ExRestBetween,ExRestAfter,VariantName,VariantSets,VariantReps,VariantWeight,VariantHasDrop,VariantNotes,VariantInitialWeight,VariantInitialWeightDate"
    private const val HISTORY_HEADER = "WorkoutName,StartTime,EndTime,CardioDuration,WarmupDuration,TotalReps,TotalVolume,EffortRating,TotalRestSeconds,StrengthStartTime,StrengthEndTime,ExName,SetNumber,Reps,Weight,IsDropSet,SetDuration"
    private const val SETTINGS_HEADER = "CountdownAudioEnabled,ThemeMode,AccentColorIndex"

    private val dateFormat = SimpleDateFormat("dd-MM-yy", Locale.US)
    private val dateTimeFormat = SimpleDateFormat("dd-MM-yy HH:mm", Locale.US)

    // Helper to escape CSV fields: wraps in quotes and escapes internal quotes
    private fun String.csvSafe(): String {
        val clean = this.replace("\n", " [NL] ").replace("\r", "")
        return if (clean.contains(",") || clean.contains("\"")) {
            "\"" + clean.replace("\"", "\"\"") + "\""
        } else {
            clean
        }
    }

    // Helper to unescape CSV fields
    private fun String.csvUnsafe(): String {
        val unquoted = if (startsWith("\"") && endsWith("\"")) {
            substring(1, length - 1).replace("\"\"", "\"")
        } else {
            this
        }
        return unquoted.replace(" [NL] ", "\n")
    }

    // Improved row splitter that respects quotes
    private fun splitCsvRow(row: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < row.length) {
            val char = row[i]
            if (char == '\"') {
                if (inQuotes && i + 1 < row.length && row[i + 1] == '\"') {
                    current.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (char == ',' && !inQuotes) {
                result.add(current.toString())
                current = StringBuilder()
            } else {
                current.append(char)
            }
            i++
        }
        result.add(current.toString())
        return result
    }

    sealed class ValidationResult<out T> {
        data class Success<out T>(val data: T) : ValidationResult<T>()
        data class Error(val message: String) : ValidationResult<Nothing>()
    }

    fun exportWorkouts(data: List<WorkoutBundle>): String {
        val sb = StringBuilder()
        sb.append(WORKOUT_HEADER).append("\n")
        data.forEach { bundle ->
            bundle.exercises.forEach { exBundle ->
                exBundle.variants.forEach { variant ->
                    sb.append("${bundle.workout.name.csvSafe()},")
                    sb.append("${bundle.workout.cardioType.csvSafe()},")
                    sb.append("${bundle.workout.cardioDurationMinutes},")
                    sb.append("${bundle.workout.warmupDurationMinutes},")
                    sb.append("${exBundle.exercise.name.csvSafe()},")
                    sb.append("${exBundle.exercise.restBetweenSetsSeconds},")
                    sb.append("${exBundle.exercise.restAfterExerciseSeconds},")
                    sb.append("${variant.name.csvSafe()},")
                    sb.append("${variant.defaultSetsCount},")
                    sb.append("${variant.defaultRepsCount},")
                    sb.append("${variant.currentWeight},")
                    sb.append("${variant.hasDropSet},")
                    sb.append("${variant.notes.csvSafe()},")
                    sb.append("${variant.initialWeight},")
                    sb.append("${dateFormat.format(Date(variant.initialWeightDate))}\n")
                }
                if (exBundle.variants.isEmpty()) {
                    sb.append("${bundle.workout.name.csvSafe()},")
                    sb.append("${bundle.workout.cardioType.csvSafe()},")
                    sb.append("${bundle.workout.cardioDurationMinutes},")
                    sb.append("${bundle.workout.warmupDurationMinutes},")
                    sb.append("${exBundle.exercise.name.csvSafe()},")
                    sb.append("${exBundle.exercise.restBetweenSetsSeconds},")
                    sb.append("${exBundle.exercise.restAfterExerciseSeconds},")
                    sb.append(",,,,,,\n")
                }
            }
            if (bundle.exercises.isEmpty()) {
                sb.append("${bundle.workout.name.csvSafe()},")
                sb.append("${bundle.workout.cardioType.csvSafe()},")
                sb.append("${bundle.workout.cardioDurationMinutes},,,,,,,,,,,,\n")
            }
        }
        return sb.toString()
    }

    fun importWorkouts(csv: String): ValidationResult<List<WorkoutBundle>> {
        val lines = csv.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return ValidationResult.Error("The file is empty.")
        
        val header = lines[0].trim()
        if (!header.startsWith("WorkoutName") || !header.contains("VariantName")) {
            return ValidationResult.Error("Invalid file format. The header does not match a Workout Backup.")
        }

        val bundles = mutableMapOf<String, WorkoutBundle>()
        val exerciseMap = mutableMapOf<String, ExerciseBundle>()

        try {
            lines.drop(1).forEach { line ->
                val parts = splitCsvRow(line)
                if (parts.size < 3) return@forEach
                
                val wName = parts[0].csvUnsafe()
                val cType = parts[1].csvUnsafe()
                val cDur = parts[2].toIntOrNull() ?: 0
                val wDur = if (parts.size > 3) parts[3].toIntOrNull() ?: 0 else 0
                
                val bundle = bundles.getOrPut(wName) {
                    WorkoutBundle(WorkoutEntity(name = wName, cardioType = cType, cardioDurationMinutes = cDur, warmupDurationMinutes = wDur), mutableListOf())
                }
                
                val exerciseStartIdx = if (parts.size > 14) 4 else 3 
                
                if ((parts.size > exerciseStartIdx) && parts[exerciseStartIdx].isNotBlank()) {
                    val exName = parts[exerciseStartIdx].csvUnsafe()
                    val exRestB = parts.getOrNull(exerciseStartIdx + 1)?.toIntOrNull() ?: 60
                    val exRestA = parts.getOrNull(exerciseStartIdx + 2)?.toIntOrNull() ?: 120
                    
                    val exKey = "$wName|$exName"
                    val exBundle = exerciseMap.getOrPut(exKey) {
                        val b = ExerciseBundle(ExerciseEntity(workoutId = 0, name = exName, sequenceOrder = bundle.exercises.size, restBetweenSetsSeconds = exRestB, restAfterExerciseSeconds = exRestA), mutableListOf())
                        (bundle.exercises as MutableList).add(b)
                        b
                    }
                    
                    val variantStartIdx = exerciseStartIdx + 3
                    if ((parts.size > variantStartIdx) && parts[variantStartIdx].isNotBlank()) {
                        val vName = parts[variantStartIdx].csvUnsafe()
                        val vSets = parts.getOrNull(variantStartIdx + 1)?.toIntOrNull() ?: 3
                        val vReps = parts.getOrNull(variantStartIdx + 2)?.toIntOrNull() ?: 10
                        val vWeight = parts.getOrNull(variantStartIdx + 3)?.toDoubleOrNull() ?: 0.0
                        val vHasDrop = parts.getOrNull(variantStartIdx + 4)?.toBoolean() ?: false
                        val vNotes = if (parts.size > variantStartIdx + 5) parts[variantStartIdx + 5].csvUnsafe() else ""
                        val vInitW = if (parts.size > variantStartIdx + 6) parts[variantStartIdx + 6].toDoubleOrNull() ?: 0.0 else 0.0
                        val vInitDStr = parts.getOrNull(variantStartIdx + 7)
                        val vInitD = if (vInitDStr != null) {
                            try {
                                dateFormat.parse(vInitDStr)?.time ?: System.currentTimeMillis()
                            } catch (_: Exception) {
                                vInitDStr.toLongOrNull() ?: System.currentTimeMillis()
                            }
                        } else System.currentTimeMillis()
                        
                        (exBundle.variants as MutableList).add(
                            ExerciseVariantEntity(
                                exerciseId = 0,
                                name = vName,
                                notes = vNotes,
                                defaultSetsCount = vSets,
                                defaultRepsCount = vReps,
                                currentWeight = vWeight,
                                hasDropSet = vHasDrop,
                                initialWeight = vInitW,
                                initialWeightDate = vInitD,
                            )
                        )
                    }
                }
            }
            return ValidationResult.Success(bundles.values.toList())
        } catch (e: Exception) {
            return ValidationResult.Error("Error parsing file: ${e.localizedMessage}")
        }
    }

    fun exportHistory(data: List<HistoryBundle>): String {
        val sb = StringBuilder()
        sb.append(HISTORY_HEADER).append("\n")
        data.forEach { bundle ->
            val s = bundle.session
            val sessionPrefix = StringBuilder()
            sessionPrefix.append("${s.workoutName.csvSafe()},")
            sessionPrefix.append("${dateTimeFormat.format(Date(s.startTime))},")
            sessionPrefix.append("${dateTimeFormat.format(Date(s.endTime))},")
            sessionPrefix.append("${s.cardioDurationSeconds},")
            sessionPrefix.append("${s.warmupDurationSeconds},")
            sessionPrefix.append("${s.totalReps},")
            sessionPrefix.append("${s.totalVolume},")
            sessionPrefix.append("${s.effortRating},")
            sessionPrefix.append("${s.totalRestSeconds},")
            sessionPrefix.append("${if (s.strengthStartTime > 0) dateTimeFormat.format(Date(s.strengthStartTime)) else ""},")
            sessionPrefix.append("${if (s.strengthEndTime > 0) dateTimeFormat.format(Date(s.strengthEndTime)) else ""},")

            bundle.logs.forEach { log ->
                sb.append(sessionPrefix.toString())
                sb.append("${log.exerciseName.csvSafe()},")
                sb.append("${log.setNumber},")
                sb.append("${log.reps},")
                sb.append("${log.weight},")
                sb.append("${log.isDropSet},")
                sb.append("${log.durationSeconds}\n")
            }
            if (bundle.logs.isEmpty()) {
                sb.append(sessionPrefix.toString())
                sb.append(",,,,,,\n")
            }
        }
        return sb.toString()
    }

    fun importHistory(csv: String): ValidationResult<List<HistoryBundle>> {
        val lines = csv.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return ValidationResult.Error("The file is empty.")

        val header = lines[0].trim()
        if (!header.startsWith("WorkoutName") || !header.contains("TotalVolume")) {
            return ValidationResult.Error("Invalid file format. The header does not match a History Backup.")
        }

        val bundles = mutableMapOf<String, HistoryBundle>()

        try {
            lines.drop(1).forEach { line ->
                val parts = splitCsvRow(line)
                if (parts.size < 7) return@forEach
                
                val wName = parts[0].csvUnsafe()
                val startStr = parts[1]
                val endStr = parts[2]
                
                val start = try { dateTimeFormat.parse(startStr)?.time ?: 0L } catch (_: Exception) { startStr.toLongOrNull() ?: 0L }
                val end = try { dateTimeFormat.parse(endStr)?.time ?: 0L } catch (_: Exception) { endStr.toLongOrNull() ?: 0L }
                
                val cDur = parts[3].toIntOrNull() ?: 0
                val wDur = parts[4].toIntOrNull() ?: 0
                val reps = parts[5].toIntOrNull() ?: 0
                val vol = parts[6].toDoubleOrNull() ?: 0.0
                val effort = parts[7].toIntOrNull() ?: 0
                
                var totalRest = 0
                var strengthStart = 0L
                var strengthEnd = 0L
                var logIndex = 8
                
                if (parts.size >= 17) {
                    totalRest = parts[8].toIntOrNull() ?: 0
                    strengthStart = try { dateTimeFormat.parse(parts[9])?.time ?: 0L } catch (_: Exception) { parts[9].toLongOrNull() ?: 0L }
                strengthEnd = try { dateTimeFormat.parse(parts[10])?.time ?: 0L } catch (_: Exception) { parts[10].toLongOrNull() ?: 0L }
                    logIndex = 11
                } else if (parts.size >= 13) { // Simplified legacy check
                    logIndex = 7
                }
                
                val key = "$wName|$start|$end"
                val bundle = bundles.getOrPut(key) {
                    HistoryBundle(
                        SessionHistoryEntity(
                            workoutId = 0, 
                            workoutName = wName, 
                            startTime = start, 
                            endTime = end, 
                            cardioDurationSeconds = cDur, 
                            warmupDurationSeconds = wDur,
                            totalReps = reps, 
                            totalVolume = vol, 
                            effortRating = effort,
                            totalRestSeconds = totalRest,
                            strengthStartTime = strengthStart,
                            strengthEndTime = strengthEnd
                        ), 
                        mutableListOf()
                    )
                }
                
                if (parts.size > logIndex && parts[logIndex].isNotBlank()) {
                    val exName = parts[logIndex].csvUnsafe()
                    val sNum = parts.getOrNull(logIndex + 1)?.toIntOrNull() ?: 1
                    val sReps = parts.getOrNull(logIndex + 2)?.toIntOrNull() ?: 10
                    val sWeight = parts.getOrNull(logIndex + 3)?.toDoubleOrNull() ?: 0.0
                    val sDrop = parts.getOrNull(logIndex + 4)?.toBoolean() ?: false
                    val sDur = parts.getOrNull(logIndex + 5)?.toIntOrNull() ?: 0
                    
                    (bundle.logs as MutableList).add(SetLogEntity(
                        sessionHistoryId = 0,
                        exerciseId = 0,
                        exerciseName = exName,
                        setNumber = sNum,
                        reps = sReps,
                        weight = sWeight,
                        isDropSet = sDrop,
                        durationSeconds = sDur
                    ))
                }
            }
            return ValidationResult.Success(bundles.values.toList())
        } catch (e: Exception) {
            return ValidationResult.Error("Error parsing history: ${e.localizedMessage}")
        }
    }

    fun exportSettings(prefs: PreferencesManager): String {
        val sb = StringBuilder()
        sb.append(SETTINGS_HEADER).append("\n")
        sb.append("${prefs.isCountdownAudioEnabled},")
        sb.append("${prefs.themeMode},")
        sb.append("${prefs.accentColorIndex}\n")
        return sb.toString()
    }

    fun importSettings(csv: String): ValidationResult<SettingsData> {
        val lines = csv.lines().filter { it.isNotBlank() }
        if (lines.size < 2) return ValidationResult.Error("The file is empty or missing data.")
        
        val header = lines[0].trim()
        if (!header.startsWith("CountdownAudioEnabled")) {
            return ValidationResult.Error("Invalid file format. The header does not match Settings Backup.")
        }

        val parts = lines[1].split(",")
        if (parts.size < 3) return ValidationResult.Error("Missing settings data in the file.")
        
        return try {
            ValidationResult.Success(SettingsData(
                countdownAudioEnabled = parts[0].toBoolean(),
                themeMode = parts[1].toIntOrNull() ?: 0,
                accentColorIndex = parts[2].toIntOrNull() ?: 0
            ))
        } catch (e: Exception) {
            ValidationResult.Error("Error parsing settings: ${e.localizedMessage}")
        }
    }

    data class WorkoutBundle(val workout: WorkoutEntity, val exercises: List<ExerciseBundle>)
    data class ExerciseBundle(val exercise: ExerciseEntity, val variants: List<ExerciseVariantEntity>)
    data class HistoryBundle(val session: SessionHistoryEntity, val logs: List<SetLogEntity>)
    data class SettingsData(val countdownAudioEnabled: Boolean, val themeMode: Int, val accentColorIndex: Int)
}
