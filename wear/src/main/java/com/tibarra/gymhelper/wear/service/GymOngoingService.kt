package com.tibarra.gymhelper.wear.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.LocusId
import androidx.core.content.LocusIdCompat
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import com.tibarra.gymhelper.shared.model.SessionUiState
import com.tibarra.gymhelper.wear.MainActivity
import com.tibarra.gymhelper.wear.R
import com.tibarra.gymhelper.wear.SyncStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class GymOngoingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var stateJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Essential: startForeground immediately to prevent crash
        val initialNotification = createNotification(SyncStore.nodeState.value)
        startForeground(NOTIFICATION_ID, initialNotification)

        stateJob?.cancel()
        stateJob = SyncStore.nodeState.onEach { state ->
            if (state.isStarted && !state.isFinished) {
                updateNotification(state)
            } else {
                stopSelf()
            }
        }.launchIn(serviceScope)

        return START_STICKY
    }

    private fun updateNotification(state: SessionUiState) {
        val notification = createNotification(state)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Required for Foreground Service
        startForeground(NOTIFICATION_ID, notification)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotification(state: SessionUiState): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val activeExercise = state.exercises.find { !it.isCompleted }
        val title = when {
            state.isResting -> "Rest (${state.restTimeLeft}s)"
            state.isWarmupActive -> "Warmup"
            state.isCardioActive -> "Cardio"
            else -> activeExercise?.name ?: "Workout"
        }

        val baseTime = if (state.sessionStartTimeMillis > 0) {
            val elapsedSinceStart = System.currentTimeMillis() - state.sessionStartTimeMillis
            SystemClock.elapsedRealtime() - elapsedSinceStart
        } else {
            SystemClock.elapsedRealtime()
        }

        val status = Status.Builder()
            .addTemplate("Gym #time#")
            .addPart("time", Status.StopwatchPart(baseTime))
            .build()

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_watch_dumbbell)
            .setContentTitle("Gym Helper")
            .setContentText(title)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setLocalOnly(true)
            .setUsesChronometer(true)
            .setWhen(System.currentTimeMillis() - (state.totalSessionTimeSeconds * 1000L))
            .setShowWhen(true)

        val ongoingActivity = OngoingActivity.Builder(
            this, 
            NOTIFICATION_ID, 
            notificationBuilder
        )
            .setAnimatedIcon(R.mipmap.ic_launcher_foreground)
            .setStaticIcon(R.mipmap.ic_launcher_foreground)
            .setTouchIntent(pendingIntent)
            .setStatus(status)
            .build()

        ongoingActivity.apply(this)

        return notificationBuilder.build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Active Workout",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Shows active gym session on the watch face"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "active_workout_channel"
    }
}
