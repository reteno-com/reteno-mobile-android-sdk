package com.reteno.core.data.remote.model.inbox

import com.google.gson.annotations.SerializedName

internal data class InboxMessagesRemote(
    @SerializedName("list")
    val messages: List<InboxMessageRemote>,
    @SerializedName("totalPages")
    val totalPages: Int?
)