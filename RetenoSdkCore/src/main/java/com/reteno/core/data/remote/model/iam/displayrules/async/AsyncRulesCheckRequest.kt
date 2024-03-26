package com.reteno.core.data.remote.model.iam.displayrules.async

import com.google.gson.annotations.SerializedName

data class AsyncRulesCheckRequest(
    @SerializedName("checks")
    val checks: List<AsyncRulesCheckRequestModel>
) {
    companion object {
        fun createSegmentRequest(segmentIds: List<Long>): AsyncRulesCheckRequest {
            return AsyncRulesCheckRequest(segmentIds.map { segmentId ->
                AsyncRulesCheckRequestModel(params = AsyncRulesCheckRequestParams(segmentId))
            })
        }
    }
}