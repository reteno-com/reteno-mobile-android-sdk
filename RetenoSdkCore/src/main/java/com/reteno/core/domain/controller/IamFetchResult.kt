package com.reteno.core.domain.controller

import com.reteno.core.data.remote.model.iam.message.InAppMessageContent

data class IamFetchResult(
    val id: String,
    val fullHtml: String,
    val layoutType: InAppMessageContent.InAppLayoutType,
    val layoutParams: InAppMessageContent.InAppLayoutParams
)