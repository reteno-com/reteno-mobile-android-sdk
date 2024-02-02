package com.reteno.core.data.repository

import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent
import com.reteno.core.data.remote.model.iam.widget.WidgetModel
import com.reteno.core.features.iam.IamJsEvent

internal interface IamRepository {
    suspend fun getBaseHtml(): String
    suspend fun getWidgetRemote(interactionId: String): WidgetModel
    fun widgetInitFailed(widgetId: String, jsEvent: IamJsEvent)
    suspend fun getInAppMessages(): List<InAppMessage>
    suspend fun getInAppMessagesContent(messageInstanceIds: List<Long>): List<InAppMessageContent>
}