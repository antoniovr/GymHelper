package com.tibarra.gymhelper.util

import kotlinx.coroutines.flow.MutableSharedFlow

object WearCommandEventBus {
    val commands = MutableSharedFlow<String>(extraBufferCapacity = 10)
}
