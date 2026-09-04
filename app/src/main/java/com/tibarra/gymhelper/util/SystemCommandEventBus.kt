package com.tibarra.gymhelper.util

import kotlinx.coroutines.flow.MutableSharedFlow

object SystemCommandEventBus {
    val commands = MutableSharedFlow<String>(extraBufferCapacity = 10)
    const val CMD_SKIP_REST = "SKIP_REST"
    const val CMD_FINISH_WORKOUT = "FINISH_WORKOUT"
    const val CMD_STOP_WARMUP = "STOP_WARMUP"
    const val CMD_STOP_CARDIO = "STOP_CARDIO"
}
