package com.reteno.core.recommendation

import com.reteno.core.data.remote.model.recommendation.get.RecomBase
import com.reteno.core.data.remote.model.recommendation.get.Recoms

interface GetRecommendationResponseCallback<T : RecomBase> {

    fun onSuccess(response: Recoms<T>)

    fun onSuccessFallbackToJson(response: String)

    fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?)
}