package com.reteno.core.domain.controller

import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent
import com.reteno.core.domain.ResultDomain
import com.reteno.core.features.iam.IamJsEvent
import com.reteno.core.internal.action.iam.InAppDisplay
import kotlinx.coroutines.flow.Flow

internal interface IamController : InAppDisplay {

    fun fetchIamFullHtml(interactionId: String)

    fun fetchIamFullHtml(messageContent: InAppMessageContent?)

    fun widgetInitFailed(tenantId:String, jsEvent: IamJsEvent)

    fun reset()

    val fullHtmlStateFlow: Flow<ResultDomain<IamFetchResult>>
    val inAppMessagesFlow: Flow<InAppMessage>
}