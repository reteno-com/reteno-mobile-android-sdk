package com.reteno.core.data.repository

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.reteno.core.data.remote.OperationQueue
import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.mapper.fromJson
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.mapper.toRemote
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.data.remote.model.recommendation.get.RecomBase
import com.reteno.core.domain.model.recommendation.get.RecomRequest
import com.reteno.core.data.remote.model.recommendation.get.Recoms
import com.reteno.core.recommendation.GetRecommendationResponseCallback
import com.reteno.core.util.Logger


class RecommendationRepositoryImpl(private val apiClient: ApiClient) : RecommendationRepository {

    override fun <T : RecomBase> getRecommendation(
        recomVariantId: String,
        recomRequest: RecomRequest,
        responseClass: Class<T>,
        responseCallback: GetRecommendationResponseCallback<T>
    ) {
        /*@formatter:off*/ Logger.i(TAG, "getRecommendation(): ", "recomVariantId = [" , recomVariantId , "], recomRequest = [" , recomRequest , "], responseClass = [" , responseClass , "]")
        /*@formatter:on*/
        apiClient.post(
            ApiContract.Recommendation.Get(recomVariantId),
            recomRequest.toRemote().toJson(),
            object : ResponseCallback {
                override fun onSuccess(response: String) {
                    try {
                        val recomList = mutableListOf<T>()

                        val jsonObjectRoot = Gson().fromJson(response, JsonObject::class.java)
                        val recomsJsonArray =
                            jsonObjectRoot.get(Recoms.FIELD_NAME_RECOMS).asJsonArray
                        for (jsonObj in recomsJsonArray) {
                            val singleRecom: T = jsonObj.fromJson(responseClass)
                            recomList.add(singleRecom)
                        }

                        val result = Recoms(recomList)
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

    companion object {
        val TAG: String = RecommendationRepositoryImpl::class.java.simpleName
    }
}