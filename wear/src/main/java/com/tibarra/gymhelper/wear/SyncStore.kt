package com.tibarra.gymhelper.wear

import com.tibarra.gymhelper.shared.model.SessionUiState
import kotlinx.coroutines.flow.MutableStateFlow

object SyncStore {
    val nodeState = MutableStateFlow(SessionUiState())
}
