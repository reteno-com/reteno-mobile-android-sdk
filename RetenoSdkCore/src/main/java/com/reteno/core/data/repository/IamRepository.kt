package com.reteno.core.data.repository

import com.reteno.core.data.remote.model.iam.displayrules.async.AsyncRulesCheckResult
import com.reteno.core.data.remote.model.iam.message.InAppMessageResponse
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent
import com.reteno.core.data.remote.model.iam.widget.WidgetModel
import com.reteno.core.features.iam.IamJsEvent

internal interface IamRepository {
    suspend fun getBaseHtml(): String
    suspend fun getWidgetRemote(interactionId: String): WidgetModel
    fun widgetInitFailed(widgetId: String, jsEvent: IamJsEvent)
    suspend fun getInAppMessages(): List<InAppMessageResponse>
    suspend fun getInAppMessagesContent(messageInstanceIds: List<Long>): List<InAppMessageContent>
    suspend fun checkUserInSegments(segmentIds: List<Long>): List<AsyncRulesCheckResult>
}