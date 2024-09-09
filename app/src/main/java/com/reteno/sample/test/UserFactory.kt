package com.reteno.sample.test

import com.reteno.core.domain.model.user.UserAttributesAnonymous
import com.reteno.core.domain.model.user.UserCustomField

object UserFactory {
    @JvmStatic
    fun createAnonymousAttrs(
        name: String,
        lastName: String,
        fields: List<UserCustomField>
    ): UserAttributesAnonymous {
        return UserAttributesAnonymous(
            name,
            lastName,
            fields = fields
        )
    }
}