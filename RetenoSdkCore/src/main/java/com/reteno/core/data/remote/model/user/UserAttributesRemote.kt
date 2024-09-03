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
    fun createDiffModel(olderModel: UserAttributesRemote?): UserAttributesRemote? {
        if (olderModel == null) return copy()

        if (olderModel == this) return null

        var somethingChanged = false
        val result = copy()

        if (phone == olderModel.phone) {
            result.phone = null
        } else {
            somethingChanged = true
        }

        if (email == olderModel.email) {
            result.email = null
        } else {
            somethingChanged = true
        }

        if (firstName == olderModel.firstName) {
            result.firstName = null
        } else {
            somethingChanged = true
        }

        if (lastName == olderModel.lastName) {
            result.lastName = null
        } else {
            somethingChanged = true
        }

        if (languageCode == olderModel.languageCode) {
            result.languageCode = null
        } else {
            somethingChanged = true
        }

        if (timeZone == olderModel.timeZone) {
            result.timeZone = null
        } else {
            somethingChanged = true
        }

        if (address == olderModel.address) {
            result.address = null
        } else {
            somethingChanged = true
        }

        if (fields == olderModel.fields) {
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

    fun createAccModel(olderModel: UserAttributesRemote?): UserAttributesRemote {
        if (olderModel == null || olderModel == this) return copy()

        val result = copy()

        result.phone = result.phone ?: olderModel.phone
        result.email = result.email ?: olderModel.email
        result.firstName = result.firstName ?: olderModel.firstName
        result.lastName = result.lastName ?: olderModel.lastName
        result.languageCode = result.languageCode ?: olderModel.languageCode
        result.timeZone = result.timeZone ?: olderModel.timeZone
        result.address = result.address ?: olderModel.address
        result.fields = olderModel.fields.orEmpty().toSet().plus(result.fields.orEmpty().toSet()).toList()

        return result
    }
}