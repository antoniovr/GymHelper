package com.tibarra.gymhelper.util

import kotlinx.coroutines.flow.MutableSharedFlow

object SystemCommandEventBus {
    val commands = MutableSharedFlow<String>(extraBufferCapacity = 10)
    const val CMD_SKIP_REST = "SKIP_REST"
}
