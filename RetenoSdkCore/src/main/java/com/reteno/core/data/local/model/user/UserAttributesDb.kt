package com.reteno.core.data.local.model.user

data class UserAttributesDb(
    val phone: String? = null,
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val languageCode: String? = null,
    val timeZone: String? = null,
    val address: AddressDb? = null,
    val fields: List<UserCustomFieldDb>? = null,
)