package com.reteno.core.data.local.model.user

data class UserDb(
    val deviceId: String,
    val externalUserId: String,
    val userAttributes: UserAttributesDb? = null,
    val subscriptionKeys: List<String>? = null,
    val groupNamesInclude: List<String>? = null,
    val groupNamesExclude: List<String>? = null
)