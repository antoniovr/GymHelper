package com.tibarra.gymhelper.wear.util

import android.content.Context
import com.google.android.gms.wearable.Wearable
import com.tibarra.gymhelper.shared.SyncUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object CommandSender {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun send(context: Context, command: String, payload: String? = null) {
        scope.launch {
            try {
                val nodes = Wearable.getNodeClient(context).connectedNodes.await()
                val messageClient = Wearable.getMessageClient(context)
                val data = if (payload != null) "$command/$payload" else command
                nodes.forEach { node ->
                    messageClient.sendMessage(node.id, SyncUtils.PATH_COMMAND, data.toByteArray()).await()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
