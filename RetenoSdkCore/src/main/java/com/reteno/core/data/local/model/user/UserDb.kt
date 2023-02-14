package com.reteno.core.data.local.model.user

data class UserDb(
    val rowId: String? = null,
    val deviceId: String,
    val externalUserId: String? = null,
    val userAttributes: UserAttributesDb? = null,
    val subscriptionKeys: List<String>? = null,
    val groupNamesInclude: List<String>? = null,
    val groupNamesExclude: List<String>? = null
)