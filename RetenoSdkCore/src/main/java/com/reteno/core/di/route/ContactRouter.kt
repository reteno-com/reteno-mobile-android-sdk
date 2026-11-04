package com.reteno.core.di.route

import com.reteno.core.di.graph.DeviceGraph
import com.reteno.core.di.graph.UserGraph
import com.reteno.core.domain.model.user.User
import com.reteno.core.domain.model.user.UserAttributesAnonymous
import com.reteno.core.internal.action.user.ContactActions

internal class ContactRouter(
    private val user: UserGraph,
    private val device: DeviceGraph
) : ContactActions {

    override fun submitUserAttributes(externalUserId: String, user: User?) {
        device.contactQueue.enqueue { this.user.setUserAttributes(externalUserId, user) }
    }

    override fun submitMultiAccountUserAttributes(externalUserId: String, user: User?) {
        device.contactQueue.enqueue { this.user.setMultiAccountUserAttributes(externalUserId, user) }
    }

    override fun submitAnonymousUserAttributes(attributes: UserAttributesAnonymous) {
        device.contactQueue.enqueue { user.setAnonymousUserAttributes(attributes) }
    }

    override fun submitFcmToken(token: String) {
        device.contactQueue.enqueue { device.updateFcmToken(token) }
    }

    override fun submitNotificationsEnabled(enabled: Boolean) {
        device.contactQueue.enqueue { device.updateNotificationsEnabled(enabled) }
    }
}
