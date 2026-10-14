package com.reteno.core.data.repository

import com.reteno.core.data.remote.model.iam.widget.WidgetModel
import com.reteno.core.features.iam.IamJsEvent

internal interface IamRepository {
    suspend fun getLocalBaseHtml(): String
    suspend fun getWidgetRemote(interactionId: String): WidgetModel
    fun widgetInitFailed(widgetId: String, jsEvent: IamJsEvent)
}