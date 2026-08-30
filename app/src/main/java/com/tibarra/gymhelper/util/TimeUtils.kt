package com.tibarra.gymhelper.util

import java.util.Locale

object TimeUtils {
    fun formatTime(seconds: Int): String {
        if (seconds <= 0) return "00:00"
        val mins = seconds / 60
        val secs = seconds % 60
        return "%02d:%02d".format(Locale.getDefault(), mins, secs)
    }

    fun formatHoursMinutes(seconds: Int): String {
        if (seconds <= 0) return "0m"
        val hours = seconds / 3600
        val mins = (seconds % 3600) / 60
        return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
    }

    fun formatCardioDuration(minutes: Int): String {
        if (minutes <= 0) return "0 m"
        val hours = minutes / 60
        val mins = minutes % 60
        return if (hours > 0) "${hours} h %02d m".format(Locale.getDefault(), mins) else "$mins m"
    }
}
