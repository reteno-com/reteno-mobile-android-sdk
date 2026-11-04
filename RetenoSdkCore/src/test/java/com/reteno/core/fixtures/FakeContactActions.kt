package com.reteno.core.fixtures

import com.reteno.core.domain.model.user.User
import com.reteno.core.domain.model.user.UserAttributesAnonymous
import com.reteno.core.internal.action.user.ContactActions

internal class FakeContactActions(private val log: FakeCallLog = FakeCallLog()) : ContactActions {
    val users = mutableListOf<User?>()
    val multiAccountUsers = mutableListOf<User?>()
    val anonymousAttributes = mutableListOf<UserAttributesAnonymous>()

    override fun submitUserAttributes(externalUserId: String, user: User?) {
        log.record("submitUserAttributes $externalUserId")
        users += user
    }

    override fun submitMultiAccountUserAttributes(externalUserId: String, user: User?) {
        log.record("submitMultiAccountUserAttributes $externalUserId")
        multiAccountUsers += user
    }

    override fun submitAnonymousUserAttributes(attributes: UserAttributesAnonymous) {
        log.record("submitAnonymousUserAttributes")
        anonymousAttributes += attributes
    }

    override fun submitFcmToken(token: String) {
        log.record("submitFcmToken")
    }

    override fun submitNotificationsEnabled(enabled: Boolean) {
        log.record("submitNotificationsEnabled $enabled")
    }
}
