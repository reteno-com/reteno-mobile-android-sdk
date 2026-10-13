package com.reteno.core.internal.action.user

import com.reteno.core.domain.model.user.User
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.lifecycle.SdkLifecycle
import com.reteno.core.internal.log.Redact

internal class SetMultiAccountUserAttributesAction(
    private val writer: ContactWriter,
    private val sdk: () -> SdkLifecycle,
    private val requestSegmentationRefresh: () -> Unit,
    private val runner: ActionRunner
) {

    suspend operator fun invoke(externalUserId: String, user: User?): Result<Unit> = runner("SetMultiAccountUserAttributes") {
        args("externalUserId" to { Redact.token(externalUserId) }, "user" to { user.traceFields() })
        writer.write(this, suffix = externalUserId, externalUserId = externalUserId, user = user)
        if (sdk().isStarted) step("schedule segmentation refresh") { requestSegmentationRefresh() }
    }
}
