package com.reteno.core.features.iam

import com.google.gson.annotations.SerializedName

internal data class IamJsEvent(
    @SerializedName("type") val type: IamJsEventType,
    @SerializedName("payload") val payload: IamJsPayload?,
)