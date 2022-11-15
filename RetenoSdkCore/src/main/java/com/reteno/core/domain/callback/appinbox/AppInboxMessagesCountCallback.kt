package com.reteno.core.domain.callback.appinbox

interface AppInboxMessagesCountCallback {

    fun onSuccess(count: Int)

    fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?)
}