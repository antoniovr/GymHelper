package com.tibarra.gymhelper.wear

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.wear.compose.material3.MaterialTheme
import android.content.LocusId
import com.tibarra.gymhelper.wear.service.GymOngoingService
import com.tibarra.gymhelper.wear.ui.WearApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val uiState by SyncStore.nodeState.collectAsState()
            
            LaunchedEffect(uiState.isStarted, uiState.isFinished) {
                if (uiState.isStarted && !uiState.isFinished) {
                    startForegroundService(Intent(this@MainActivity, GymOngoingService::class.java))
                }
            }

            MaterialTheme {
                WearApp(uiState, onFinish = { finish() })
            }
        }
    }
}
