package com.reteno.core.internal.action

import com.reteno.core.util.Logger
import java.util.UUID

suspend inline fun runAction(tag: String, name: String, action: suspend () -> Unit) {
    val operationId = UUID.randomUUID()
    runCatching {
        Logger.d(tag, "invoke():", "${operationId}: $name START")
        action()
    }.onSuccess {
        Logger.d(tag, "invoke():", "${operationId}: $name SUCCESS")
    }.onFailure {
        Logger.e(tag, "invoke():", it, "${operationId}: $name FAILURE")
    }
}