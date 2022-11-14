package com.reteno.core.domain.model.user

data class UserCustomField(
    /**
     * Additional field.
     */
    val key: String,

    /**
     * Value of additional field.
     */
    val value: String?
)