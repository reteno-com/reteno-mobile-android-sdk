package com.reteno.core.data.remote.model.recommendation.post

import com.google.gson.annotations.SerializedName

internal data class RecomEventsRequestRemote(
    @SerializedName("events")
    val events: List<RecomEventsRemote>
)