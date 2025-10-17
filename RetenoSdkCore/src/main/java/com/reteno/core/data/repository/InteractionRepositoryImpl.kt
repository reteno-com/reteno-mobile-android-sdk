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

internal class InteractionRepositoryImpl(
    private val apiClient: ApiClient,
    private val interactionDatabaseManager: RetenoDatabaseManagerInteraction,
    private val inAppInteractionDatabaseManager: RetenoDatabaseManagerInAppInteraction
) : InteractionRepository {

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
        /*@formatter:off*/ Logger.i(TAG, "saveInAppInteraction(): ", "inAppInteraction = [" , inAppInteraction , "]")
        /*@formatter:on*/
        OperationQueue.addParallelOperation {
            inAppInteractionDatabaseManager.insertInteraction(inAppInteraction.toDb())
            pushInAppInteractions()
        }
    }

    override fun pushInAppInteractions() {
        val inAppInteractionDb: InAppInteractionDb =
            inAppInteractionDatabaseManager.getInteractions(1).firstOrNull() ?: kotlin.run {
                PushOperationQueue.nextOperation()
                return
            }
        /*@formatter:off*/ Logger.i(TAG, "pushInAppInteractions(): ", "inAppInteractionDb = [" , inAppInteractionDb , "]")
        /*@formatter:on*/
        apiClient.post(
            ApiContract.InAppMessages.RegisterInteraction,
            inAppInteractionDb.toRemote().toJson(),
            object : ResponseCallback {

                override fun onSuccess(response: String) {
                    /*@formatter:off*/ Logger.i(TAG, "onSuccess(): ", "response = [" , response , "]")
                    /*@formatter:on*/
                    val cacheUpdated =
                        inAppInteractionDatabaseManager.deleteInteraction(inAppInteractionDb)
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
                            inAppInteractionDatabaseManager.deleteInteraction(inAppInteractionDb)
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

        private const val REMOVE_INTERACTIONS = "Removed interactions"
        private const val REMOVE_IN_APP_INTERACTIONS = "Removed inAppInteractions"
    }
}