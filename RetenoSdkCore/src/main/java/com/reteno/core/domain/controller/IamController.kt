package com.reteno.core.domain.controller

import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.data.remote.model.iam.message.InAppMessageResponse
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent
import com.reteno.core.domain.ResultDomain
import com.reteno.core.domain.model.event.Event
import com.reteno.core.features.iam.IamJsEvent
import com.reteno.core.view.iam.IamView
import kotlinx.coroutines.flow.StateFlow

internal interface IamController {

    fun fetchIamFullHtml(interactionId: String)

    fun fetchIamFullHtml(messageContent: InAppMessageContent?)

    fun widgetInitFailed(jsEvent: IamJsEvent)

    fun reset()

    fun setIamView(iamView: IamView)

    fun getInAppMessages(showMessage: (InAppMessage) -> Unit)

    fun notifyEventOccurred(event: Event)

    val fullHtmlStateFlow: StateFlow<ResultDomain<String>>
}