package com.reteno.core.internal.action.iam

import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.iam.InAppMessagesState

internal interface PauseInAppMessages {
    suspend operator fun invoke(paused: Boolean): Result<Unit>
}

internal class PauseInAppMessagesAction(
    private val state: InAppMessagesState,
    private val display: InAppDisplay,
    private val runner: ActionRunner
) : PauseInAppMessages {

    override suspend operator fun invoke(paused: Boolean): Result<Unit> = runner("PauseInAppMessages") {
        args("paused" to paused)
        val postponed = step<InAppMessage?>(
            "apply pause to in-app state",
            detail = { if (it == null) "nothing postponed to show" else "show postponed ${it.messageId}" }
        ) {
            state.setPaused(paused)
        }
        if (postponed != null) {
            step("pass postponed in-app message to display") {
                display.show(postponed)
            }
        }
    }
}
