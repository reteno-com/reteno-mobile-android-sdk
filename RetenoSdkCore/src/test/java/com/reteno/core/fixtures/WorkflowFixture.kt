package com.reteno.core.fixtures

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInAppInteraction
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.iam.WorkflowInAppState
import io.mockk.mockk
import com.reteno.core.data.remote.model.iam.message.InAppMessage
import java.time.Instant

internal fun workflowState(
    dispatchers: RetenoDispatchers,
    rest: FakeRestClient = FakeRestClient(),
    interactions: RetenoDatabaseManagerInAppInteraction = mockk(relaxed = true),
    clock: () -> Long = System::currentTimeMillis
): WorkflowInAppState = WorkflowInAppState(rest.api(dispatchers), interactions, dispatchers, clock)

internal fun stubWorkflowMessage(
    iid: String = "11111111-1111-4111-8111-111111111111",
    context: String = "test-context",
    now: Long = System.currentTimeMillis()
): InAppMessage = stubInAppMessage(content = null).copy(
    messageInstanceId = 8001,
    inWorkflow = true,
    iid = iid,
    workflowContext = context,
    expiresAt = Instant.ofEpochMilli(now).plusSeconds(3600).toString()
)
