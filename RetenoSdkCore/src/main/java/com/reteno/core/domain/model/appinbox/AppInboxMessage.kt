package com.reteno.core.domain.model.appinbox

import com.reteno.core.features.appinbox.AppInboxStatus

data class AppInboxMessage(
    val id: String,
    val title: String,
    val createdDate: String,
    val isNewMessage: Boolean,
    val content: String?,
    val imageUrl: String?,
    val linkUrl: String?,
    val category: String?,
    val status: AppInboxStatus?,
    val customData: Map<String, String>?
)