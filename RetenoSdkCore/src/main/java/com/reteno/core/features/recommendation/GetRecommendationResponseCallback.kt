package com.reteno.core.features.recommendation

import com.reteno.core.data.remote.model.recommendation.get.RecomBase
import com.reteno.core.data.remote.model.recommendation.get.Recoms

interface GetRecommendationResponseCallback<T : RecomBase> {

    /**
     * @param response Response casted to a provided type
     */
    fun onSuccess(response: Recoms<T>)

    /**
     * @param response This callback is invoked if casting to a provided type fails. Provides raw JSON String
     */
    fun onSuccessFallbackToJson(response: String)

    /**
     * @param statusCode Rest status code (500, 400, etc)
     * @param response response message from the server
     * @param throwable exception thrown returned in this parameter if any
     */
    fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?)
}