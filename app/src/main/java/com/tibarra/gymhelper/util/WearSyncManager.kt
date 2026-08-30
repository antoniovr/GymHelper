package com.tibarra.gymhelper.util

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.tibarra.gymhelper.shared.SyncUtils
import com.tibarra.gymhelper.shared.model.SessionUiState
import kotlinx.coroutines.tasks.await

class WearSyncManager(private val context: Context) {
    private val dataClient by lazy { Wearable.getDataClient(context) }

    suspend fun syncState(state: SessionUiState) {
        try {
            val json = SyncUtils.toJson(state)
            val request = PutDataMapRequest.create(SyncUtils.PATH_SESSION_STATE).apply {
                dataMap.putString("state", json)
                dataMap.putLong("timestamp", System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()
            
            dataClient.putDataItem(request).await()
            Log.d("WearSyncManager", "State synced to Wear OS")
        } catch (e: Exception) {
            Log.e("WearSyncManager", "Error syncing state", e)
        }
    }
}
