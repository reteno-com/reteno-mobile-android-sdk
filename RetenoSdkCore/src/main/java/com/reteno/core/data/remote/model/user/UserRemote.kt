package com.reteno.core.data.remote.model.user

import com.google.gson.annotations.SerializedName

internal data class UserRemote(
    @SerializedName("deviceId")
    var deviceId: String,
    @SerializedName("externalUserId")
    var externalUserId: String?,
    @SerializedName("userAttributes")
    var userAttributes: UserAttributesRemote? = null,
    @SerializedName("subscriptionKeys")
    var subscriptionKeys: List<String>? = null,
    @SerializedName("groupNamesInclude")
    var groupNamesInclude: List<String>? = null,
    @SerializedName("groupNamesExclude")
    var groupNamesExclude: List<String>? = null
) {
    fun createDiffModel(another: UserRemote?): UserRemote? {
        if (another == null) return this

        if (another == this) return null

        var somethingChanged = false
        val result = another.copy()

        val userAttributesDiff = userAttributes?.createDiffModel(another.userAttributes)
        result.userAttributes = userAttributesDiff
        if (userAttributesDiff != null) {
            somethingChanged = true
        }

        if (areListsSame(subscriptionKeys, result.subscriptionKeys)) {
            result.subscriptionKeys = null
        } else {
            somethingChanged = true
        }

        if (areListsSame(groupNamesInclude, result.groupNamesInclude)) {
            result.groupNamesInclude = null
        } else {
            somethingChanged = true
        }

        if (areListsSame(groupNamesExclude, result.groupNamesExclude)) {
            result.groupNamesExclude = null
        } else {
            somethingChanged = true
        }

        return if (somethingChanged) {
            result
        } else {
            null
        }
    }

    private fun areListsSame(firstList: List<Any>?, secondList: List<Any>?): Boolean {
        if (firstList == null && secondList == null) return true

        if (firstList != null && secondList != null &&
            firstList.size == secondList.size && firstList.containsAll(secondList)) {
            return true
        }

        return false
    }
}