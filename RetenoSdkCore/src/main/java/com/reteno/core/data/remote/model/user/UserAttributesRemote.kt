package com.reteno.core.data.remote.model.user

import com.google.gson.annotations.SerializedName

internal data class UserAttributesRemote(
    @SerializedName("phone")
    var phone: String? = null,
    @SerializedName("email")
    var email: String? = null,
    @SerializedName("firstName")
    var firstName: String? = null,
    @SerializedName("lastName")
    var lastName: String? = null,
    @SerializedName("languageCode")
    var languageCode: String? = null,
    @SerializedName("timeZone")
    var timeZone: String? = null,
    @SerializedName("address")
    var address: AddressRemote? = null,
    @SerializedName("fields")
    var fields: List<UserCustomFieldRemote>? = null,
) {
    fun createDiffModel(another: UserAttributesRemote?): UserAttributesRemote? {
        if (another == null) return this

        if (another == this) return null

        var somethingChanged = false
        val result = another.copy()

        if (phone == result.phone) {
            result.phone = null
        } else {
            somethingChanged = true
        }

        if (email == result.email) {
            result.email = null
        } else {
            somethingChanged = true
        }

        if (firstName == result.firstName) {
            result.firstName = null
        } else {
            somethingChanged = true
        }

        if (lastName == result.lastName) {
            result.lastName = null
        } else {
            somethingChanged = true
        }

        if (languageCode == result.languageCode) {
            result.languageCode = null
        } else {
            somethingChanged = true
        }

        if (timeZone == result.timeZone) {
            result.timeZone = null
        } else {
            somethingChanged = true
        }

        if (address == result.address) {
            result.address = null
        } else {
            somethingChanged = true
        }

        if (fields == result.fields) {
            result.fields = null
        } else {
            somethingChanged = true
        }

        return if (somethingChanged) {
            result
        } else {
            null
        }
    }
}