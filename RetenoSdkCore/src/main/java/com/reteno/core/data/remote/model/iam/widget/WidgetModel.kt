package com.reteno.core.data.remote.model.iam.widget

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent

data class WidgetModel(
    @SerializedName("layoutType")
    val layoutType: InAppMessageContent.InAppLayoutType,

    @SerializedName("layoutParams")
    val layoutParams: InAppMessageContent.InAppLayoutParams?,

    @SerializedName("model")
    val model: JsonElement?,

    @SerializedName("personalisation")
    val personalization: JsonElement? = null,
)
