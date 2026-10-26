package com.reteno.core.internal.action.trace

import kotlinx.coroutines.CancellationException

internal suspend fun <T> ActionTrace.stepOrNull(
    name: String,
    detail: (T) -> String = { "" },
    block: suspend () -> T
): T? =
    try {
        step(name, detail, block)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        null
    }
