package com.reteno.core.data.repository

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInAppInteraction
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInteraction
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.local.model.interaction.InAppInteractionDb
import com.reteno.core.data.local.model.interaction.InteractionDb
import com.reteno.core.data.remote.OperationQueue
import com.reteno.core.data.remote.PushOperationQueue
import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.mapper.toRemote
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.domain.model.interaction.InAppInteraction
import com.reteno.core.domain.model.interaction.Interaction
import com.reteno.core.domain.model.logevent.LogLevel
import com.reteno.core.domain.model.logevent.RetenoLogEvent
import com.reteno.core.util.Logger
import com.reteno.core.util.Util.formatToRemote
import com.reteno.core.util.isNonRepeatableError
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicBoolean

internal class InteractionRepositoryImpl(
    private val apiClient: ApiClient,
    private val interactionDatabaseManager: RetenoDatabaseManagerInteraction,
    private val inAppInteractionDatabaseManager: RetenoDatabaseManagerInAppInteraction
) : InteractionRepository {
    private val pushingInApp = AtomicBoolean(false)

    override suspend fun saveInteraction(interactionId: String, interaction: Interaction) {
        /*@formatter:off*/ Logger.i(TAG, "saveInteraction(): ", "interactionId = [" , interactionId , "], interaction = [" , interaction , "]")
        /*@formatter:on*/
        interactionDatabaseManager.insertInteraction(interaction.toDb(interactionId))
    }

    override fun pushInteractions() {
        val interactionDb: InteractionDb = interactionDatabaseManager.getInteractions(1).firstOrNull() ?: kotlin.run {
            PushOperationQueue.nextOperation()
            return
        }
        /*@formatter:off*/ Logger.i(TAG, "pushInteractions(): ", "interactionDb = [" , interactionDb , "]")
        /*@formatter:on*/
        apiClient.put(
            ApiContract.RetenoApi.InteractionStatus(interactionDb.interactionId),
            interactionDb.toRemote().toJson(),
            object : ResponseCallback {

                override fun onSuccess(response: String) {
                    /*@formatter:off*/ Logger.i(TAG, "onSuccess(): ", "response = [" , response , "]")
                    /*@formatter:on*/
                    val cacheUpdated = interactionDatabaseManager.deleteInteraction(interactionDb)
                    if (cacheUpdated) {
                        pushInteractions()
                    } else {
                        PushOperationQueue.nextOperation()
                    }
                }

                override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
                    /*@formatter:off*/ Logger.i(TAG, "onFailure(): ", "statusCode = [" , statusCode , "], response = [" , response , "], throwable = [" , throwable , "]")
                    /*@formatter:on*/
                    if (isNonRepeatableError(statusCode)) {
                        val cacheUpdated =
                            interactionDatabaseManager.deleteInteraction(interactionDb)
                        if (cacheUpdated) {
                            pushInteractions()
                        }
                    } else {
                        PushOperationQueue.removeAllOperations()
                    }
                }

            }
        )
    }

    override fun clearOldInteractions(outdatedTime: ZonedDateTime) {
        /*@formatter:off*/ Logger.i(TAG, "clearOldInteractions(): ", "outdatedTime = [" , outdatedTime , "]")
        /*@formatter:on*/
        OperationQueue.addOperation {
            val removedInteractions: List<InteractionDb> =
                interactionDatabaseManager.deleteInteractionByTime(outdatedTime.formatToRemote())
            /*@formatter:off*/ Logger.i(TAG, "clearOldInteractions(): ", "removedInteractionsCount = [" , removedInteractions.count() , "]")
            /*@formatter:on*/
            if (removedInteractions.isNotEmpty()) {
                removedInteractions
                    .groupBy { it.status }
                    .map { it.key to "${it.value.size}" }
                    .forEach {
                        val status = it.first
                        val count = it.second

                        val msg = "$REMOVE_INTERACTIONS($status) - $count"
                        val event = RetenoLogEvent(
                            logLevel = LogLevel.INFO,
                            errorMessage = msg
                        )
                        Logger.captureEvent(event)
                    }
            }
        }
    }

    override fun saveAndPushInAppInteraction(inAppInteraction: InAppInteraction) {
        if (inAppInteraction.workflowContext != null) {
            Logger.d(TAG, "saveInAppInteraction(): workflow status=", inAppInteraction.status)
            // A visible observation is both the durable shown marker and its queued event.
            inAppInteractionDatabaseManager.insertInteraction(inAppInteraction.toDb())
            OperationQueue.addParallelOperation { pushInAppInteractions() }
        } else {
            Logger.i(TAG, "saveInAppInteraction(): ", "inAppInteraction = [", inAppInteraction, "]")
            OperationQueue.addParallelOperation {
                inAppInteractionDatabaseManager.insertInteraction(inAppInteraction.toDb())
                pushInAppInteractions()
            }
        }
    }

    override fun pushInAppInteractions() {
        if (!pushingInApp.compareAndSet(false, true)) return
        val owner = apiClient.workflowContext()
        val interaction = inAppInteractionDatabaseManager.getPendingInteractions(owner).firstOrNull()
        if (interaction == null) {
            pushingInApp.set(false)
            PushOperationQueue.nextOperation()
            return
        }
        if (interaction.workflowContext != null) {
            if (interaction.attempts >= MAX_WORKFLOW_ATTEMPTS) {
                Logger.captureMessage("Workflow interaction retries exhausted. status=${interaction.status}", LogLevel.ERROR)
                inAppInteractionDatabaseManager.deleteInteraction(interaction)
                pushingInApp.set(false)
                pushInAppInteractions()
                return
            }
            try {
                inAppInteractionDatabaseManager.recordAttempt(interaction)
            } catch (error: Exception) {
                pushingInApp.set(false)
                Logger.e(TAG, "Failed to persist workflow retry; delivery deferred", error)
                return
            }
        }
        val callback = object : ResponseCallback {
            override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?, headers: Map<String, List<String>>) {
                if (interaction.workflowContext != null) {
                    val retryAfter = headers.entries.firstOrNull { it.key.equals("Retry-After", true) }?.value?.firstOrNull()
                    val delay = retryAfter?.toLongOrNull()?.coerceAtLeast(0)?.coerceAtMost(90L * 24 * 60 * 60)?.times(1000)
                    val deadline = if (delay != null) System.currentTimeMillis() + delay else runCatching {
                        ZonedDateTime.parse(retryAfter, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
                    }.getOrNull()
                    if (deadline != null) inAppInteractionDatabaseManager.deferUntil(interaction,
                        maxOf(deadline, System.currentTimeMillis() + (30_000L shl interaction.attempts.coerceAtMost(10))))
                }
                onFailure(statusCode, response, throwable)
            }

            override fun onSuccess(response: String) {
                val updated = inAppInteractionDatabaseManager.deleteInteraction(interaction)
                pushingInApp.set(false)
                if (updated) pushInAppInteractions() else PushOperationQueue.nextOperation()
            }

            override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
                pushingInApp.set(false)
                if (isNonRepeatableError(statusCode)) {
                    if (inAppInteractionDatabaseManager.deleteInteraction(interaction)) pushInAppInteractions()
                    else PushOperationQueue.nextOperation()
                } else {
                    Logger.w(TAG, "In-app observation delivery deferred. statusCode=", statusCode)
                    PushOperationQueue.removeAllOperations()
                }
            }
        }
        if (interaction.workflowContext == null) {
            apiClient.post(ApiContract.InAppMessages.RegisterInteraction, interaction.toRemote().toJson(), callback)
        } else {
            apiClient.postInWorkflowContext(
                interaction.workflowContext, ApiContract.InAppMessages.RegisterInteraction,
                interaction.toRemote().toJson(), callback
            )
        }
    }

    override fun clearOldInAppInteractions(outdatedTime: ZonedDateTime) {
        /*@formatter:off*/ Logger.i(TAG, "clearOldInAppInteractions(): ", "outdatedTime = [" , outdatedTime , "]")
        /*@formatter:on*/
        OperationQueue.addOperation {
            val removedInteractions: List<InAppInteractionDb> =
                inAppInteractionDatabaseManager.deleteInteractionsByTime(outdatedTime.formatToRemote())
            /*@formatter:off*/ Logger.i(TAG, "clearOldInAppInteractions(): ", "removedInteractionsCount = [" , removedInteractions.count() , "]")
            /*@formatter:on*/
            if (removedInteractions.isNotEmpty()) {
                removedInteractions
                    .groupBy { it.status }
                    .map { it.key to "${it.value.size}" }
                    .forEach {
                        val status = it.first
                        val count = it.second

                        val msg = "$REMOVE_IN_APP_INTERACTIONS($status) - $count"
                        val event = RetenoLogEvent(
                            logLevel = LogLevel.INFO,
                            errorMessage = msg
                        )
                        Logger.captureEvent(event)
                    }
            }
        }
    }

    companion object {
        private val TAG = InteractionRepositoryImpl::class.java.simpleName

        private const val MAX_WORKFLOW_ATTEMPTS = 3
        private const val REMOVE_INTERACTIONS = "Removed interactions"
        private const val REMOVE_IN_APP_INTERACTIONS = "Removed inAppInteractions"
    }
}
