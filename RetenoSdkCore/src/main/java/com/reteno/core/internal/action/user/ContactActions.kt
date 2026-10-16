package com.reteno.core.internal.action.user

import com.reteno.core.domain.model.user.User
import com.reteno.core.domain.model.user.UserAttributesAnonymous

internal interface ContactActions {
    fun submitUserAttributes(externalUserId: String, user: User?)

    fun submitMultiAccountUserAttributes(externalUserId: String, user: User?)

    fun submitAnonymousUserAttributes(attributes: UserAttributesAnonymous)

    fun submitFcmToken(token: String)

    fun submitNotificationsEnabled(enabled: Boolean)
}
