package com.reteno.core.domain.model.appinbox

data class AppInboxMessage(
    val id: String,
    val title: String,
    val createdDate: String,
    val isNewMessage: Boolean,
    val content: String?,
    val imageUrl: String?,
    val linkUrl: String?,
    val category: String?,
    val customData: Map<String, String>?
)