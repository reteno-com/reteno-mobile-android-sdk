package com.reteno.core.fixtures

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInAppInteraction
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.iam.DeliveryInAppState
import io.mockk.mockk
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import java.time.Instant

internal fun deliveryState(
    dispatchers: RetenoDispatchers,
    rest: FakeRestClient = FakeRestClient(),
    interactions: RetenoDatabaseManagerInAppInteraction = mockk(relaxed = true),
    clock: () -> Long = System::currentTimeMillis
): DeliveryInAppState = DeliveryInAppState(rest.api(dispatchers), interactions, dispatchers, clock)

internal fun stubDeliveryMessage(
    iid: String = "11111111-1111-4111-8111-111111111111",
    context: String = "test-context",
    now: Long = System.currentTimeMillis()
): InAppMessage = stubInAppMessage(content = null).copy(
    messageInstanceId = 8001,
    priority = 20,
    iid = iid,
    deliveryContext = context,
    expiresAt = Instant.ofEpochMilli(now).plusSeconds(3600).toString()
)
