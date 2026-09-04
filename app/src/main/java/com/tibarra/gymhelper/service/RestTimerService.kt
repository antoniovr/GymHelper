package com.tibarra.gymhelper.service

import android.app.*
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.tibarra.gymhelper.MainActivity
import com.tibarra.gymhelper.R
import com.tibarra.gymhelper.util.PreferencesManager
import com.tibarra.gymhelper.util.SystemCommandEventBus
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.milliseconds

class RestTimerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null
    private var toneGenerator: ToneGenerator? = null
    private var audioManager: AudioManager? = null
    private var currentFocusRequest: Any? = null
    private var prefsManager: PreferencesManager? = null

    private var initialSeconds = 60
    private var remainingSeconds = 60
    private var mode = TimerMode.SESSION
    private var isBellPlayed = false

    private enum class TimerMode { SESSION, REST, CARDIO, WARMUP }

    companion object {
        const val CHANNEL_ID = "gym_timer_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START_SESSION = "START_SESSION"
        const val ACTION_STOP_SESSION = "STOP_SESSION"
        const val ACTION_START_REST = "START_REST"
        const val ACTION_UPDATE_REST = "UPDATE_REST"
        const val ACTION_STOP_REST = "STOP_REST"
        const val ACTION_START_CARDIO = "START_CARDIO"
        const val ACTION_STOP_CARDIO = "STOP_CARDIO"
        const val ACTION_START_WARMUP = "START_WARMUP"
        const val ACTION_STOP_WARMUP = "STOP_WARMUP"
        const val ACTION_FINISH_SESSION = "FINISH_SESSION"
        const val ACTION_SKIP_REST_NOTIF = "SKIP_REST_NOTIF"
        const val EXTRA_SECONDS = "EXTRA_SECONDS"
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        prefsManager = PreferencesManager(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SESSION -> {
                timerJob?.cancel()
                mode = TimerMode.SESSION
                initialSeconds = 0
                remainingSeconds = 0
                updateNotification()
            }
            ACTION_STOP_SESSION -> {
                timerJob?.cancel()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_START_REST -> {
                timerJob?.cancel()
                isBellPlayed = false
                mode = TimerMode.REST
                initialSeconds = intent.getIntExtra(EXTRA_SECONDS, 60)
                remainingSeconds = initialSeconds
                startRestTimer()
            }
            ACTION_UPDATE_REST -> {
                if (mode == TimerMode.REST) {
                    val newSeconds = intent.getIntExtra(EXTRA_SECONDS, remainingSeconds)
                    if (newSeconds > initialSeconds) initialSeconds = newSeconds
                    remainingSeconds = newSeconds
                    updateNotification()
                }
            }
            ACTION_STOP_REST -> {
                timerJob?.cancel()
                mode = TimerMode.SESSION
                initialSeconds = 0
                remainingSeconds = 0
                updateNotification()
            }
            ACTION_START_CARDIO -> {
                timerJob?.cancel()
                isBellPlayed = false
                mode = TimerMode.CARDIO
                initialSeconds = intent.getIntExtra(EXTRA_SECONDS, 0)
                remainingSeconds = if (initialSeconds > 0) intent.getIntExtra("EXTRA_REMAINING", initialSeconds) else 0
                startCardioTimer()
            }
            ACTION_STOP_CARDIO -> {
                timerJob?.cancel()
                mode = TimerMode.SESSION
                initialSeconds = 0
                remainingSeconds = 0
                updateNotification()
            }
            ACTION_START_WARMUP -> {
                timerJob?.cancel()
                isBellPlayed = false
                mode = TimerMode.WARMUP
                initialSeconds = intent.getIntExtra(EXTRA_SECONDS, 0)
                remainingSeconds = if (initialSeconds > 0) intent.getIntExtra("EXTRA_REMAINING", initialSeconds) else 0
                startWarmupTimer()
            }
            ACTION_STOP_WARMUP -> {
                timerJob?.cancel()
                mode = TimerMode.SESSION
                initialSeconds = 0
                remainingSeconds = 0
                updateNotification()
            }
            "RESPAWN_NOTIFICATION" -> {
                updateNotification()
            }
            ACTION_FINISH_SESSION -> {
                if (mode == TimerMode.SESSION) {
                    serviceScope.launch {
                        SystemCommandEventBus.commands.emit(SystemCommandEventBus.CMD_FINISH_WORKOUT)
                    }
                    
                    val restartIntent = Intent(this, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        putExtra("ACTION", "FINISH_WORKOUT")
                    }
                    startActivity(restartIntent)
                } else {
                    val oldMode = mode
                    timerJob?.cancel()
                    mode = TimerMode.SESSION
                    initialSeconds = 0
                    remainingSeconds = 0
                    updateNotification()
                    
                    serviceScope.launch {
                        when (oldMode) {
                            TimerMode.REST -> SystemCommandEventBus.commands.emit(SystemCommandEventBus.CMD_SKIP_REST)
                            TimerMode.WARMUP -> SystemCommandEventBus.commands.emit(SystemCommandEventBus.CMD_STOP_WARMUP)
                            TimerMode.CARDIO -> SystemCommandEventBus.commands.emit(SystemCommandEventBus.CMD_STOP_CARDIO)
                            else -> {}
                        }
                    }
                }
            }
            ACTION_SKIP_REST_NOTIF -> {
                if (mode == TimerMode.REST) {
                    timerJob?.cancel()
                    mode = TimerMode.SESSION
                    updateNotification()
                    
                    serviceScope.launch {
                        com.tibarra.gymhelper.util.SystemCommandEventBus.commands.emit(com.tibarra.gymhelper.util.SystemCommandEventBus.CMD_SKIP_REST)
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startRestTimer() {
        timerJob?.cancel()
        updateNotification()

        timerJob = serviceScope.launch {
            var nextTick = System.currentTimeMillis()
            while (remainingSeconds >= 0) {
                updateNotification()
                checkAndPlayAudioFeedback(remainingSeconds)

                if (remainingSeconds == 0) break
                
                nextTick += 1000
                val delayTime = nextTick - System.currentTimeMillis()
                if (delayTime > 0) delay(delayTime.milliseconds)
                
                remainingSeconds--
            }
            delay(1000)
            abandonPersistentDucking()
            mode = TimerMode.SESSION
            updateNotification()
        }
    }

    private fun startCardioTimer() {
        timerJob?.cancel()
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        timerJob = serviceScope.launch {
            var nextTick = System.currentTimeMillis()
            while (true) {
                updateNotification()
                
                if (initialSeconds > 0) {
                    checkAndPlayAudioFeedback(remainingSeconds)
                }
                
                nextTick += 1000
                val delayTime = nextTick - System.currentTimeMillis()
                if (delayTime > 0) delay(delayTime.milliseconds)
                
                if (initialSeconds > 0) {
                    remainingSeconds--
                } else {
                    remainingSeconds++
                }
            }
        }
    }

    private fun startWarmupTimer() {
        timerJob?.cancel()
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        timerJob = serviceScope.launch {
            var nextTick = System.currentTimeMillis()
            while (true) {
                updateNotification()
                
                if (initialSeconds > 0) {
                    checkAndPlayAudioFeedback(remainingSeconds)
                }
                
                nextTick += 1000
                val delayTime = nextTick - System.currentTimeMillis()
                if (delayTime > 0) delay(delayTime.milliseconds)
                
                if (initialSeconds > 0) {
                    remainingSeconds--
                } else {
                    remainingSeconds++
                }
            }
        }
    }

    private fun checkAndPlayAudioFeedback(remaining: Int) {
        if (initialSeconds == 0) return // Skip audio for 0s timers

        val soundsEnabled = prefsManager?.isCountdownAudioEnabled == true
        if (!soundsEnabled || !isHeadsetConnected()) {
            if ((remaining == 0) && !isBellPlayed) {
                isBellPlayed = true
                abandonPersistentDucking()
            }
            return
        }

        if (remaining == 10) {
            requestPersistentDucking()
        }

        if (remaining in 1..10) {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        } else if (remaining == 0 && !isBellPlayed) {
            isBellPlayed = true
            playBoxingBell()
            abandonPersistentDucking()
        }
    }

    private fun isHeadsetConnected(): Boolean {
        val devices = audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS) ?: return false
        return devices.any {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && it.type == AudioDeviceInfo.TYPE_USB_HEADSET)
        }
    }

    private fun playBoxingBell() {
        serviceScope.launch {
            // High-pitched DTMF tones to simulate a sharp bell ring
            toneGenerator?.startTone(ToneGenerator.TONE_DTMF_D, 300)
            delay(400)
            toneGenerator?.startTone(ToneGenerator.TONE_DTMF_D, 500)
        }
    }

    private fun requestPersistentDucking() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .build()
            if (am.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                currentFocusRequest = focusRequest
            }
        } else {
            @Suppress("DEPRECATION")
            if (am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                currentFocusRequest = "legacy"
            }
        }
    }

    private fun abandonPersistentDucking() {
        val am = audioManager ?: return
        val request = currentFocusRequest ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && request is AudioFocusRequest) {
            am.abandonAudioFocusRequest(request)
        } else if (request == "legacy") {
            @Suppress("DEPRECATION")
            am.abandonAudioFocus(null)
        }
        currentFocusRequest = null
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val modeLabel = when (mode) {
            TimerMode.SESSION -> "Gym Session Active"
            TimerMode.REST -> "Resting"
            TimerMode.CARDIO -> "Cardio"
            TimerMode.WARMUP -> "Warm-up"
        }
        
        val timeStr = if (mode == TimerMode.SESSION) {
            ""
        } else if (mode == TimerMode.REST) {
            formatTime(remainingSeconds)
        } else {
            if (remainingSeconds > 0) formatTime(remainingSeconds)
            else "EXTRA: ${formatTime(-remainingSeconds)}"
        }

        val title = if (mode == TimerMode.SESSION) modeLabel else "$modeLabel: $timeStr"
        val content = if (mode == TimerMode.SESSION) {
            "Training in progress..."
        } else if (mode == TimerMode.REST) {
            "Time to recover"
        } else if (initialSeconds > 0) {
            "Target goal in progress"
        } else {
            "Elapsed: $timeStr"
        }

        val finishIntent = Intent(this, RestTimerService::class.java).apply { action = ACTION_FINISH_SESSION }
        val finishPendingIntent = PendingIntent.getService(this, 1, finishIntent, PendingIntent.FLAG_IMMUTABLE)
        
        val skipIntent = Intent(this, RestTimerService::class.java).apply { action = ACTION_SKIP_REST_NOTIF }
        val skipPendingIntent = PendingIntent.getService(this, 2, skipIntent, PendingIntent.FLAG_IMMUTABLE)

        val respawnIntent = Intent(this, RestTimerService::class.java).apply { action = "RESPAWN_NOTIFICATION" }
        val respawnPendingIntent = PendingIntent.getService(this, 3, respawnIntent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setDeleteIntent(respawnPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(false)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "FINISH", finishPendingIntent)

        if (mode == TimerMode.REST) {
            builder.addAction(android.R.drawable.ic_media_next, "SKIP", skipPendingIntent)
        }

        if (mode != TimerMode.SESSION && initialSeconds > 0) {
            if (remainingSeconds >= 0) {
                builder.setProgress(initialSeconds, initialSeconds - remainingSeconds, false)
            } else {
                builder.setProgress(0, 0, false)
            }
        }

        return builder.build()
    }

    private fun updateNotification() {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun formatTime(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return "%02d:%02d".format(mins, secs)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Training in progress",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Notifications for rest, cardio and warm-up"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        // This is called when the app is swiped away from Recents
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Workout Still Running")
            .setContentText("The app was closed, but your session is still active. Tap to return.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        
        // We don't stop the service, let it be killed by the system if needed, 
        // but we've updated the notification to warn the user.
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        timerJob?.cancel()
        serviceScope.cancel()
        abandonPersistentDucking()
        toneGenerator?.release()
        toneGenerator = null
        super.onDestroy()
    }
}
