package com.reteno.core.domain.controller

import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent
import com.reteno.core.domain.ResultDomain
import com.reteno.core.features.iam.IamJsEvent
import kotlinx.coroutines.flow.StateFlow

internal interface IamController {

    fun fetchIamFullHtml(interactionId: String)

    fun fetchIamFullHtml(messageContent: InAppMessageContent?)

    fun widgetInitFailed(jsEvent: IamJsEvent)

    fun reset()

    fun getInAppMessages(onMessagesLoaded: (List<InAppMessage>, List<InAppMessageContent>) -> Unit)

    val fullHtmlStateFlow: StateFlow<ResultDomain<String>>
}