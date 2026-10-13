package com.reteno.core.data.repository

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerRecomEvents
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.remote.OperationQueue
import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.mapper.convertRecoms
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.mapper.toRemote
import com.reteno.core.data.remote.model.recommendation.get.RecomBase
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.domain.model.recommendation.get.RecomRequest
import com.reteno.core.domain.model.recommendation.post.RecomEvents
import com.reteno.core.features.recommendation.GetRecommendationResponseCallback
import com.reteno.core.features.recommendation.GetRecommendationResponseJsonCallback
import com.reteno.core.util.Logger


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

    override fun getRecommendationJson(
        recomVariantId: String,
        recomRequest: RecomRequest,
        responseCallback: GetRecommendationResponseJsonCallback
    ) {
        /*@formatter:off*/ Logger.i(TAG, "getRecommendationJson(): ", "recomVariantId = [" , recomVariantId , "], recomRequest = [" , recomRequest , "]")
        /*@formatter:on*/
        apiClient.post(
            ApiContract.Recommendation.GetRecoms(recomVariantId),
            recomRequest.toRemote().toJson(),
            object : ResponseCallback {
                override fun onSuccess(response: String) {
                    OperationQueue.addUiOperation {
                        responseCallback.onSuccess(response)
                    }
                }

                override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
                    /*@formatter:off*/ Logger.e(TAG, "recomVariantId = [$recomVariantId], recomRequest = [$recomRequest]", throwable ?: Throwable("null"))
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
        OperationQueue.addParallelOperation {
            databaseManager.insertRecomEvents(recomEvents.toDb())
        }
    }

    companion object {
        private val TAG: String = RecommendationRepositoryImpl::class.java.simpleName
    }
}