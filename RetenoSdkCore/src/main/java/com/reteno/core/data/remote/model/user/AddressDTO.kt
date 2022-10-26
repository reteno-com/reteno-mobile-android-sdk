package com.reteno.core.data.remote.model.user

import com.google.gson.annotations.SerializedName

data class AddressDTO(
    @SerializedName("region")
    val region: String?,
    @SerializedName("town")
    val town: String?,
    @SerializedName("address")
    val address: String?,
    @SerializedName("postcode")
    val postcode: String?
)