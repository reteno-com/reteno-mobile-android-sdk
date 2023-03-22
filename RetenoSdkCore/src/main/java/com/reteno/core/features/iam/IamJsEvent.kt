package com.reteno.core.features.iam

internal data class IamJsEvent(
    val type: IamJsEventType,
    val payload: IamJsPayload?
)