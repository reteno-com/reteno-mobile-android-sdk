package com.reteno.core.data.repository

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerRecomEvents
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.local.model.recommendation.RecomEventsDb
import com.reteno.core.data.remote.OperationQueue
import com.reteno.core.data.remote.PushOperationQueue
import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.mapper.convertRecoms
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.mapper.toRemote
import com.reteno.core.data.remote.model.recommendation.get.RecomBase
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.domain.model.recommendation.get.RecomRequest
import com.reteno.core.domain.model.recommendation.post.RecomEvents
import com.reteno.core.recommendation.GetRecommendationResponseCallback
import com.reteno.core.util.Logger
import com.reteno.core.util.Util.formatToRemote
import com.reteno.core.util.isNonRepeatableError
import java.time.ZonedDateTime


internal class RecommendationRepositoryImpl(
    private val databaseManager: RetenoDatabaseManagerRecomEvents,
    private val apiClient: ApiClient
) : RecommendationRepository {

    override fun <T : RecomBase> getRecommendation(
        recomVariantId: String,
        recomRequest: RecomRequest,
        responseClass: Class<T>,
        responseCallback: GetRecommendationResponseCallback<T>
    ) {
        /*@formatter:off*/ Logger.i(TAG, "getRecommendation(): ", "recomVariantId = [" , recomVariantId , "], recomRequest = [" , recomRequest , "], responseClass = [" , responseClass , "]")
        /*@formatter:on*/
        apiClient.post(
            ApiContract.Recommendation.GetRecoms(recomVariantId),
            recomRequest.toRemote().toJson(),
            object : ResponseCallback {
                override fun onSuccess(response: String) {
                    try {
                        val result = response.convertRecoms(responseClass)
                        OperationQueue.addUiOperation {
                            responseCallback.onSuccess(result)
                        }
                    } catch (e: Throwable) {
                        OperationQueue.addUiOperation {
                            responseCallback.onSuccessFallbackToJson(response)
                        }
                    }
                }

                override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
                    /*@formatter:off*/ Logger.e(TAG, "recomVariantId = [$recomVariantId], recomRequest = [$recomRequest], responseClass = [$responseClass]", throwable ?: Throwable("null"))
                    /*@formatter:on*/
                    OperationQueue.addUiOperation {
                        responseCallback.onFailure(statusCode, response, throwable)
                    }
                }
            }
        )
    }

    override fun saveRecommendations(recomEvents: RecomEvents) {
        /*@formatter:off*/ Logger.i(TAG, "saveRecommendations(): ", "recomEvents = [" , recomEvents , "]")
        /*@formatter:on*/
        OperationQueue.addOperation {
            try {
                databaseManager.insertRecomEvents(recomEvents.toDb())
            } catch (e: Exception) {
                Logger.e(TAG, "saveRecommendations()", e)
            }
        }
    }

    override fun pushRecommendations() {
        /*@formatter:off*/ Logger.i(TAG, "pushRecommendations(): ", "")
        /*@formatter:on*/
        val recomEventsList: List<RecomEventsDb> = databaseManager.getRecomEvents()
        if (recomEventsList.isEmpty()) {
            PushOperationQueue.nextOperation()
            return
        } else if (recomEventsList.all { it.recomEvents.isNullOrEmpty() }) {
            PushOperationQueue.nextOperation()
            return
        }
        /*@formatter:off*/ Logger.i(TAG, "pushRecommendations(): ", "recomEventsList = [" , recomEventsList , "]")
        /*@formatter:on*/

        val recomEventsListSize = recomEventsList.sumOf { it.recomEvents?.size ?: 0 }
        apiClient.post(
            ApiContract.Recommendation.PostRecoms,
            recomEventsList.toRemote().toJson(),
            object : ResponseCallback {

                override fun onSuccess(response: String) {
                    /*@formatter:off*/ Logger.i(TAG, "pushRecommendations() onSuccess(): ", "response = [" , response , "]")
                    /*@formatter:on*/
                    databaseManager.deleteRecomEvents(recomEventsListSize)
                    pushRecommendations()
                }

                override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
                    /*@formatter:off*/ Logger.i(TAG, "pushRecommendations() onFailure(): ", "statusCode = [" , statusCode , "], response = [" , response , "], throwable = [" , throwable , "]")
                    /*@formatter:on*/
                    if (isNonRepeatableError(statusCode)) {
                        databaseManager.deleteRecomEvents(recomEventsListSize)
                        pushRecommendations()
                    }
                    PushOperationQueue.removeAllOperations()
                }
            }
        )
    }

    override fun clearOldRecommendations(outdatedTime: ZonedDateTime) {
        /*@formatter:off*/ Logger.i(TAG, "clearOldRecommendations(): ", "outdatedTime = [" , outdatedTime.formatToRemote() , "]")
        /*@formatter:on*/
        OperationQueue.addOperation {
            val removedRecomEventsCount = databaseManager.deleteRecomEventsByTime(outdatedTime.formatToRemote())
            /*@formatter:off*/ Logger.i(TAG, "clearOldRecommendations(): ", "removedRecomEventsCount = [" , removedRecomEventsCount , "]")
            /*@formatter:on*/
            if (removedRecomEventsCount > 0) {
                val msg = "Outdated Recommendation Events: - $removedRecomEventsCount"
                Logger.captureEvent(msg)
            }
        }
    }

    companion object {
        val TAG: String = RecommendationRepositoryImpl::class.java.simpleName
    }
}