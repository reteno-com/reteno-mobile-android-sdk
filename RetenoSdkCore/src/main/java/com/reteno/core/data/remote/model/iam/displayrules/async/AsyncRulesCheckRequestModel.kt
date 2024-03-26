package com.reteno.core.data.remote.model.iam.displayrules.async

import com.google.gson.annotations.SerializedName

data class AsyncRulesCheckRequestModel(
    @SerializedName("type")
    val type: String = IS_IN_SEGMENT,
    val params: AsyncRulesCheckRequestParams
) {
    companion object {
        private const val IS_IN_SEGMENT = "IS_IN_SEGMENT"
    }
}