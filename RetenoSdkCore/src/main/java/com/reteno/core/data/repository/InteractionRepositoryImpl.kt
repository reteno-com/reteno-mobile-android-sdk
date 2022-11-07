package com.reteno.core.data.repository

import com.reteno.core.data.local.database.RetenoDatabaseManager
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.remote.OperationQueue
import com.reteno.core.data.remote.PushOperationQueue
import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.model.interaction.Interaction
import com.reteno.core.util.Logger
import com.reteno.core.util.isNonRepeatableError

class InteractionRepositoryImpl(
    private val apiClient: ApiClient,
    private val databaseManager: RetenoDatabaseManager
) : InteractionRepository {

    override fun saveInteraction(interactionId: String, interaction: Interaction) {
        /*@formatter:off*/ Logger.i(TAG, "saveInteraction(): ", "interactionId = [" , interactionId , "], interaction = [" , interaction , "]")
        /*@formatter:on*/
        OperationQueue.addOperation {
            databaseManager.insertInteraction(interaction.toDb(interactionId))
        }
    }

    override fun pushInteractions() {
        val interactions = databaseManager.getInteractions(1).firstOrNull() ?: kotlin.run {
            PushOperationQueue.nextOperation()
            return
        }
        /*@formatter:off*/ Logger.i(TAG, "pushInteractions(): ", "interactions = [" , interactions , "]")
        /*@formatter:on*/
        val interaction = Interaction(
            status = interactions.status,
            time = interactions.time,
            token = interactions.token
        )

        apiClient.put(
            ApiContract.RetenoApi.InteractionStatus(interactions.interactionId),
            interaction.toJson(),
            object : ResponseCallback {

                override fun onSuccess(response: String) {
                    /*@formatter:off*/ Logger.i(TAG, "onSuccess(): ", "response = [" , response , "]")
                    /*@formatter:on*/
                    databaseManager.deleteInteractions(1)
                    pushInteractions()
                }

                override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
                    /*@formatter:off*/ Logger.i(TAG, "onFailure(): ", "statusCode = [" , statusCode , "], response = [" , response , "], throwable = [" , throwable , "]")
                    /*@formatter:on*/
                    if (isNonRepeatableError(statusCode)) {
                        databaseManager.deleteInteractions(1)
                        pushInteractions()
                    }
                    PushOperationQueue.removeAllOperations()
                }

            }
        )
    }

    companion object {
        private val TAG = InteractionRepositoryImpl::class.java.simpleName
    }
}