package com.reteno.core.internal.action.iam

import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.iam.InAppMessagesState
import com.reteno.core.internal.iam.PostponedInAppMessage

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
        val postponed = step<PostponedInAppMessage?>(
            "apply pause to in-app state",
            detail = { if (it == null) "nothing postponed to show" else "show postponed ${it.message.messageId}" }
        ) {
            state.setPaused(paused)
        }
        if (postponed != null) {
            val resume = postponed.resume
            if (resume != null) {
                resume()
            } else {
                step("pass postponed in-app message to display") {
                    display.show(postponed.message)
                }
            }
        }
    }
}
