package com.reteno.core.data.repository

import com.reteno.core.RetenoImpl
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInteraction
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.local.model.interaction.InteractionDb
import com.reteno.core.data.remote.OperationQueue
import com.reteno.core.data.remote.PushOperationQueue
import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.mapper.toRemote
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.domain.model.interaction.Interaction
import com.reteno.core.util.Logger
import com.reteno.core.util.Util.formatToRemote
import com.reteno.core.util.isNonRepeatableError
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.protocol.Message
import java.time.ZonedDateTime

internal class InteractionRepositoryImpl(
    private val apiClient: ApiClient,
    private val databaseManager: RetenoDatabaseManagerInteraction
) : InteractionRepository {

    override fun saveInteraction(interactionId: String, interaction: Interaction) {
        /*@formatter:off*/ Logger.i(TAG, "saveInteraction(): ", "interactionId = [" , interactionId , "], interaction = [" , interaction , "]")
        /*@formatter:on*/
        OperationQueue.addParallelOperation {
            databaseManager.insertInteraction(interaction.toDb(interactionId))
        }
    }

    override fun pushInteractions() {
        val interactionDb: InteractionDb = databaseManager.getInteractions(1).firstOrNull() ?: kotlin.run {
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
                    val cacheUpdated = databaseManager.deleteInteraction(interactionDb)
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
                        val cacheUpdated = databaseManager.deleteInteraction(interactionDb)
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
            val removedInteractions: List<InteractionDb> = databaseManager.deleteInteractionByTime(outdatedTime.formatToRemote())
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
                        val event = SentryEvent().apply {
                            message = Message().apply {
                                message = msg
                            }
                            level = SentryLevel.INFO
                            fingerprints = listOf(
                                RetenoImpl.application.packageName,
                                REMOVE_INTERACTIONS,
                                status.toString()
                            )

                            setTag(TAG_KEY_INTERACTION_STATUS, status.toString())
                        }
                        Logger.captureEvent(event)
                    }
            }
        }
    }

    companion object {
        private val TAG = InteractionRepositoryImpl::class.java.simpleName

        private const val REMOVE_INTERACTIONS = "Removed interactions"
        private const val TAG_KEY_INTERACTION_STATUS = "interaction_status"
    }
}