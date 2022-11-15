package com.reteno.core.data.remote.model.inbox

import com.google.gson.annotations.SerializedName

data class InboxMessageRemote(
    @SerializedName("content")
    val content: String,
    @SerializedName("createDate")
    val createdDate: String,
    @SerializedName("id")
    val id: String,
    @SerializedName("image")
    val imageUrl: String?,
    @SerializedName("link")
    val linkUrl: String?,
    @SerializedName("newMessage")
    val isNewMessage: Boolean,
    @SerializedName("title")
    val title: String
)