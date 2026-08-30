package com.tibarra.gymhelper.service

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.tibarra.gymhelper.shared.SyncUtils
import com.tibarra.gymhelper.util.WearCommandEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PhoneWearListenerService : WearableListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == SyncUtils.PATH_COMMAND) {
            val command = String(messageEvent.data)
            Log.d("PhoneWearListener", "Command received: $command")
            serviceScope.launch {
                WearCommandEventBus.commands.emit(command)
            }
        }
    }
}
