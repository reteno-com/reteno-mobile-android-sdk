package com.reteno.core.domain.model.appinbox

data class AppInboxMessages(
    val messages: List<AppInboxMessage>,
    val totalPages: Int
)