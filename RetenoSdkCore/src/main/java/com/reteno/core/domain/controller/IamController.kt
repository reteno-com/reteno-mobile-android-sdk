package com.reteno.core.domain.controller

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

    fun getInAppMessages()

    fun notifyEventOccurred(event: Event)

    fun pauseInAppMessages(isPaused: Boolean)

    val fullHtmlStateFlow: StateFlow<ResultDomain<String>>
}