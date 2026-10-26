package com.reteno.core.internal.platform

internal data class AppVersion(val name: String, val code: Long) {
    override fun toString(): String = "$name ($code)"
}
