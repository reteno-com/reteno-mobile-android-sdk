package com.reteno.core.fixtures

import com.reteno.core.domain.callback.appinbox.RetenoResultCallback

internal class RecordingResultCallback<T>(private val main: MarkingDispatcher? = null) : RetenoResultCallback<T> {
    val results = mutableListOf<T>()
    val failures = mutableListOf<Triple<Int?, String?, Throwable?>>()
    val calledOnMain = mutableListOf<Boolean>()

    override fun onSuccess(result: T) {
        results += result
        calledOnMain += main?.isRunningHere == true
    }

    override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
        failures += Triple(statusCode, response, throwable)
        calledOnMain += main?.isRunningHere == true
    }
}
