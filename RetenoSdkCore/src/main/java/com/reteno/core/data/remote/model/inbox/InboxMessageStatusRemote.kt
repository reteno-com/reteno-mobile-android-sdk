package com.reteno.core.data.remote.model.inbox

import com.google.gson.annotations.SerializedName

enum class InboxMessageStatusRemote {
    @SerializedName("OPENED")
    OPENED,

    @SerializedName("UNOPENED")
    UNOPENED
}