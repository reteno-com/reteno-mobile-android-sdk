package com.reteno.sample.util

import android.os.Bundle
import android.util.Log

object SampleLog {

    fun d(tag: String, methodName: String, vararg arguments: Any?) {
        Log.d(tag, arguments.joinToString(separator = "", prefix = methodName))
    }

    fun i(tag: String, methodName: String, vararg arguments: Any?) {
        Log.i(tag, arguments.joinToString(separator = "", prefix = methodName))
    }

    fun e(tag: String, methodName: String, error: Throwable) {
        Log.e(tag, methodName, error)
    }
}

fun Bundle?.toStringVerbose(): String {
    if (this == null) return "Bundle = [null]"
    return keySet().joinToString(separator = "; ", prefix = "Bundle: [", postfix = "]") { key -> "$key = ${get(key)}" }
}
