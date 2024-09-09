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
    fun createDiffModel(olderModel: UserRemote?): UserRemote? {
        if (olderModel == null) return copy()

        if (olderModel == this) return null

        var somethingChanged = false
        val result = copy()

        val userAttributesDiff = userAttributes?.createDiffModel(olderModel.userAttributes)
        result.userAttributes = userAttributesDiff
        if (userAttributesDiff != null) {
            somethingChanged = true
        }

        if (areListsSame(subscriptionKeys, olderModel.subscriptionKeys)) {
            result.subscriptionKeys = null
        } else {
            somethingChanged = true
        }

        if (areListsSame(groupNamesInclude, olderModel.groupNamesInclude)) {
            result.groupNamesInclude = null
        } else {
            somethingChanged = true
        }

        if (areListsSame(groupNamesExclude, olderModel.groupNamesExclude)) {
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

    fun createAccModel(olderModel: UserRemote?): UserRemote {
        if (olderModel == null || olderModel == this) return copy()

        val result = copy()

        result.userAttributes = userAttributes?.createAccModel(olderModel.userAttributes)
        result.subscriptionKeys = result.subscriptionKeys?: olderModel.subscriptionKeys
        result.groupNamesInclude = result.groupNamesInclude?: olderModel.groupNamesInclude
        result.groupNamesExclude = result.groupNamesExclude?: olderModel.groupNamesExclude

        return result
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