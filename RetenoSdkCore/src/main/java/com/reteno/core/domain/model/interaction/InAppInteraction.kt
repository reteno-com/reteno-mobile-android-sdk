package com.reteno.core.domain.model.interaction

import java.time.ZonedDateTime
import java.util.UUID

data class InAppInteraction(
    val interactionId: String,
    val time: ZonedDateTime,
    val messageInstanceId: Long,
    val status: InAppInteractionStatus,
    val statusDescription: String? = null
) {
    companion object {
        fun createOpened(interactionId: String, messageInstanceId: Long): InAppInteraction {
            return InAppInteraction(
                interactionId = interactionId,
                time = ZonedDateTime.now(),
                messageInstanceId = messageInstanceId,
                status = InAppInteractionStatus.OPENED
            )
        }

        fun createFailed(interactionId: String, messageInstanceId: Long, errorDescription: String?): InAppInteraction {
            return InAppInteraction(
                interactionId = interactionId,
                time = ZonedDateTime.now(),
                messageInstanceId = messageInstanceId,
                status = InAppInteractionStatus.FAILED,
                statusDescription = errorDescription
            )
        }
    }
}