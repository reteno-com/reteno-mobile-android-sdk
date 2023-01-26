package com.reteno.core.data.repository

import com.reteno.core.features.iam.IamJsEvent

internal interface IamRepository {
    suspend fun getBaseHtml(): String
    suspend fun getWidget(widgetId: String): String
    fun widgetInitFailed(widgetId: String, jsEvent: IamJsEvent)
}