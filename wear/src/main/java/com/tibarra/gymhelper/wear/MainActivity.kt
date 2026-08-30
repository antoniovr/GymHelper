package com.tibarra.gymhelper.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.wear.compose.material3.MaterialTheme
import com.tibarra.gymhelper.wear.ui.WearApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val uiState by SyncStore.nodeState.collectAsState()
            MaterialTheme {
                WearApp(uiState, onFinish = { finish() })
            }
        }
    }
}
