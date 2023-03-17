package com.reteno.core.domain.controller

import com.reteno.core.domain.ResultDomain
import com.reteno.core.features.iam.IamJsEvent
import kotlinx.coroutines.flow.StateFlow

internal interface IamController {

    fun fetchIamFullHtml(interactionId: String)

    fun widgetInitFailed(jsEvent: IamJsEvent)

    fun reset()

    val fullHtmlStateFlow: StateFlow<ResultDomain<String>>
}