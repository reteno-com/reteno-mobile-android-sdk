package com.reteno.core.data.repository

internal interface InAppMessagesRepository {
    suspend fun getBaseHtml(): String
    suspend fun getWidget(widgetId: String): String
}