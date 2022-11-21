package com.reteno.core.data.remote.model.user

import com.google.gson.annotations.SerializedName

data class AddressRemote(
    @SerializedName("region")
    val region: String? = null,
    @SerializedName("town")
    val town: String? = null,
    @SerializedName("address")
    val address: String? = null,
    @SerializedName("postcode")
    val postcode: String? = null
)