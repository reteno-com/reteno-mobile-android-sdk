package com.reteno.core.domain.model.appinbox

data class AppInboxMessage(
    val content: String,
    val createdDate: String,
    val id: String,
    val imageUrl: String?,
    val linkUrl: String?,
    val isNewMessage: Boolean,
    val title: String
)