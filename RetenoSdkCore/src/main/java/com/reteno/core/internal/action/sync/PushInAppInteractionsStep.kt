package com.reteno.core.internal.action.sync

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInAppInteraction
import com.reteno.core.data.local.model.interaction.InAppInteractionDb
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.HttpMethod
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.mapper.toRemote
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.iam.serverAssignedIid
import com.reteno.core.internal.network.ApiResult
import com.reteno.core.internal.network.SuspendApiClient
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.withContext

internal class PushInAppInteractionsStep(
    private val inAppInteractionsDb: RetenoDatabaseManagerInAppInteraction,
    private val api: SuspendApiClient,
    private val dispatchers: RetenoDispatchers,
    private val runner: ActionRunner
) : SyncStep {

    override val name: String = "PushInAppInteractions"

    override suspend fun invoke(): Result<StepOutcome> = runner(name) {
        push()
    }

    private suspend fun ActionTrace.push(): StepOutcome {
        var handled = 0
        while (true) {
            val interaction = step("load in-app interaction from local database") {
                withContext(dispatchers.io) {
                    inAppInteractionsDb.getPendingInteractions(api.deliveryContext()).firstOrNull()
                }
            } ?: return finishedAfter(handled)
            if (interaction.serverAssignedIid && interaction.attempts >= MAX_DELIVERY_ATTEMPTS) {
                val retained = step("retain shown state after delivery retries are exhausted") {
                    withContext(dispatchers.io) { inAppInteractionsDb.deleteInteraction(interaction) }
                }
                if (!retained) return StepOutcome.CONTINUE
                handled++
                continue
            }
            if (interaction.serverAssignedIid) {
                step("save delivery delivery attempt to local database") {
                    withContext(dispatchers.io) { inAppInteractionsDb.recordAttempt(interaction) }
                }
            }
            val result = step("send in-app interaction to server") {
                val owner = interaction.deliveryContext
                if (owner == null) {
                    api.request(
                        HttpMethod.POST,
                        ApiContract.InAppMessages.RegisterInteraction,
                        interaction.toRemote().toJson()
                    )
                } else {
                    api.deliveryRequest(owner, ApiContract.InAppMessages.RegisterInteraction, interaction.toRemote().toJson())
                }
            }
            if (result is ApiResult.Failure && result.isRepeatable) {
                if (interaction.serverAssignedIid) {
                    defer(interaction, result)
                }
                return StepOutcome.ABORT
            }
            val deleted = step("delete sent in-app interaction from local database") {
                withContext(dispatchers.io) { inAppInteractionsDb.deleteInteraction(interaction) }
            }
            if (!deleted) return StepOutcome.CONTINUE
            handled++
        }
    }

    private suspend fun ActionTrace.defer(interaction: InAppInteractionDb, result: ApiResult.Failure) {
        val value = result.headers.entries.firstOrNull { it.key.equals("Retry-After", ignoreCase = true) }
            ?.value?.firstOrNull() ?: return
        val seconds = value.toLongOrNull()
        val requested = if (seconds != null) {
            System.currentTimeMillis() + seconds.coerceIn(0, MAX_RETRY_AFTER_SECONDS) * 1_000
        } else {
            runCatching { ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() }
                .getOrNull() ?: return
        }
        val backoff = System.currentTimeMillis() + (30_000L shl interaction.attempts.coerceAtMost(10))
        step("save server-requested delivery retry deadline to local database") {
            withContext(dispatchers.io) {
                inAppInteractionsDb.deferUntil(interaction, maxOf(requested, backoff))
            }
        }
    }

    private companion object {
        const val MAX_DELIVERY_ATTEMPTS = 3
        const val MAX_RETRY_AFTER_SECONDS = 90L * 24 * 60 * 60
    }
}
