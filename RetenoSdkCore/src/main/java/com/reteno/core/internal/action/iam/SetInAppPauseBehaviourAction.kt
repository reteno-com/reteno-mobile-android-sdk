package com.reteno.core.internal.action.iam

import com.reteno.core.features.iam.InAppPauseBehaviour
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.iam.InAppMessagesState

internal class SetInAppPauseBehaviourAction(
    private val state: InAppMessagesState,
    private val runner: ActionRunner
) {

    suspend operator fun invoke(behaviour: InAppPauseBehaviour): Result<Unit> = runner("SetInAppPauseBehaviour") {
        args("behaviour" to behaviour)
        step("apply pause behaviour to in-app state") {
            state.setPauseBehaviour(behaviour)
        }
    }
}
