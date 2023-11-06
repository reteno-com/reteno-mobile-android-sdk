package com.reteno.core.data.remote.model.logevent

import com.google.gson.annotations.SerializedName


internal data class RetenoLogEventListRemote(
   @SerializedName("events") val logEvents: List<RetenoLogEventRemote>
)