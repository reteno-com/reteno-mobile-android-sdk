package com.reteno.core.domain.model.user

data class User(
    /**
     * Defines [UserAttributes]
     */
    val userAttributes: UserAttributes? = null,

    /**
     * List of subscription categories keys
     */
    val subscriptionKeys: List<String>? = null,

    /**
     * List of group ID to add a contact to
     */
    val groupNamesInclude: List<String>? = null,

    /**
     * List of group ID to remove a contact from
     */
    val groupNamesExclude: List<String>? = null
)