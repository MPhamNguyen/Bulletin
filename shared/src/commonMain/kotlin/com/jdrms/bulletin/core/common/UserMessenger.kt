package com.jdrms.bulletin.core.common

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

interface UserMessenger {
    val messages: SharedFlow<String>

    fun show(message: String)
}

class FlowUserMessenger : UserMessenger {
    private val mutableMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    override val messages: SharedFlow<String> = mutableMessages.asSharedFlow()

    override fun show(message: String) {
        mutableMessages.tryEmit(message)
    }
}
