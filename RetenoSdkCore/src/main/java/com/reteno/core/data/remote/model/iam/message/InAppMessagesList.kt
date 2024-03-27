package com.reteno.core.data.remote.model.iam.message

data class InAppMessagesList(
    val messages: List<InAppMessage> = emptyList(),
    val etag: String? = null,
    val isFromRemote: Boolean = false
)