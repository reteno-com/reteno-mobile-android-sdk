package com.reteno.core.domain.controller

import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent
import com.reteno.core.domain.ResultDomain
import com.reteno.core.features.iam.IamJsEvent
import com.reteno.core.features.iam.InAppPauseBehaviour
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

internal interface IamController {

    fun fetchIamFullHtml(interactionId: String)

    fun fetchIamFullHtml(messageContent: InAppMessageContent?)

    fun widgetInitFailed(tenantId:String, jsEvent: IamJsEvent)

    fun reset()

    suspend fun getInAppMessages()

    fun pauseInAppMessages(isPaused: Boolean)

    fun updateInAppMessage(inAppMessage: InAppMessage)

    fun setPauseBehaviour(behaviour: InAppPauseBehaviour)

    fun refreshSegmentation()

    fun preloadHtml()

    val fullHtmlStateFlow: StateFlow<ResultDomain<IamFetchResult>>
    val inAppMessagesFlow: Flow<InAppMessage>
}