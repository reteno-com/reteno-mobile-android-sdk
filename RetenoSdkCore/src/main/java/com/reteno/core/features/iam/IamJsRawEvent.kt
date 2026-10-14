package com.reteno.core.features.iam

import com.google.gson.annotations.SerializedName

internal class IamJsRawEvent(
    @SerializedName("type") val type: String?,
    @SerializedName("payload") val payload: IamJsPayload?
)
