package com.tibarra.gymhelper.util

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("gym_helper_prefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_COUNTDOWN_AUDIO = "countdown_audio_enabled"
        const val KEY_THEME_MODE = "theme_mode" // 0: System, 1: Light, 2: Dark
        const val KEY_ACCENT_COLOR = "accent_color_index" // 0: Blue, 1: Green, 2: Purple
        const val KEY_ACTIVE_WORKOUT_ID = "active_workout_id"
    }

    var activeWorkoutId: Long
        get() = prefs.getLong(KEY_ACTIVE_WORKOUT_ID, -1L)
        set(value) = prefs.edit().putLong(KEY_ACTIVE_WORKOUT_ID, value).apply()

    var isCountdownAudioEnabled: Boolean
        get() = prefs.getBoolean(KEY_COUNTDOWN_AUDIO, true)
        set(value) = prefs.edit().putBoolean(KEY_COUNTDOWN_AUDIO, value).apply()

    var themeMode: Int
        get() = prefs.getInt(KEY_THEME_MODE, 0)
        set(value) = prefs.edit().putInt(KEY_THEME_MODE, value).apply()

    var accentColorIndex: Int
        get() = prefs.getInt(KEY_ACCENT_COLOR, 0)
        set(value) = prefs.edit().putInt(KEY_ACCENT_COLOR, value).apply()
}
