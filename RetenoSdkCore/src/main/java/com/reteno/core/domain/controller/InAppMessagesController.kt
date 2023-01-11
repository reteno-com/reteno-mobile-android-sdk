package com.reteno.core.domain.controller

import com.reteno.core.domain.ResultDomain
import kotlinx.coroutines.flow.StateFlow

internal interface InAppMessagesController {

    suspend fun fetchInAppMessagesFullHtml(widgetId: String)

    fun reset()

    val fullHtmlStateFlow: StateFlow<ResultDomain<String>>
}