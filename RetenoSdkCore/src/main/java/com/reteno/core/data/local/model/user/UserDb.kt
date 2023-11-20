package com.reteno.core.data.local.model.user

import com.reteno.core.data.local.model.BooleanDb

data class UserDb(
    val rowId: String? = null,
    val createdAt: Long = 0L,
    val deviceId: String,
    val externalUserId: String? = null,
    val userAttributes: UserAttributesDb? = null,
    val subscriptionKeys: List<String>? = null,
    val groupNamesInclude: List<String>? = null,
    val groupNamesExclude: List<String>? = null,
    val isSynchronizedWithBackend: BooleanDb? = null
)