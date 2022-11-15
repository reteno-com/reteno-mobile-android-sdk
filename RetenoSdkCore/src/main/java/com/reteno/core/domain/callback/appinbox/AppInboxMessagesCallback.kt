package com.reteno.core.domain.callback.appinbox

import com.reteno.core.domain.model.appinbox.AppInboxMessage

interface AppInboxMessagesCallback {

    fun onSuccess(messages: List<AppInboxMessage>, totalPages: Int)

    fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?)
}