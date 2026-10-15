package com.reteno.core.internal.platform

internal data class LogEventOrigin(
    val packageName: String,
    val osVersion: String,
    val appVersion: String?,
    val deviceModel: String
)
