package com.tibarra.gymhelper.shared

import com.google.gson.Gson
import com.tibarra.gymhelper.shared.model.SessionUiState

object SyncUtils {
    private val gson = Gson()

    const val PATH_SESSION_STATE = "/session_state"
    const val PATH_COMMAND = "/command"
    
    // Commands
    const val CMD_TOGGLE_SET = "toggle_set" // payload: "exerciseIndex,setIndex"
    const val CMD_SKIP_REST = "skip_rest"
    const val CMD_START_WARMUP = "start_warmup"
    const val CMD_STOP_WARMUP = "stop_warmup"
    const val CMD_START_CARDIO = "start_cardio"
    const val CMD_STOP_CARDIO = "stop_cardio"
    const val CMD_FINISH_SESSION = "finish_session"

    fun toJson(state: SessionUiState): String = gson.toJson(state)
    fun fromJson(json: String): SessionUiState = gson.fromJson(json, SessionUiState::class.java)
}
