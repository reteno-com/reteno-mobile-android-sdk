package com.reteno.core.internal.action.trace

internal interface TracedOutcome {
    val traceLabel: String
        get() = toString()
    val isSuccessful: Boolean
}
