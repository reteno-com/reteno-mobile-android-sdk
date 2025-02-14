package com.reteno.core.domain.controller

import com.reteno.core.data.remote.model.iam.message.InAppMessageContent

class IamFetchResult(
    val fullHtml: String,
    val layoutType: InAppMessageContent.InAppLayoutType,
    val layoutParams: InAppMessageContent.InAppLayoutParams
)