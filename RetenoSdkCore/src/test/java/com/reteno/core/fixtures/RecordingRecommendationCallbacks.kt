package com.reteno.core.fixtures

import com.reteno.core.data.remote.model.recommendation.get.RecomBase
import com.reteno.core.data.remote.model.recommendation.get.Recoms
import com.reteno.core.features.recommendation.GetRecommendationResponseCallback
import com.reteno.core.features.recommendation.GetRecommendationResponseJsonCallback

internal data class StubRecom(override val productId: String, val name: String? = null) : RecomBase

internal class RecordingRecommendationCallback<T : RecomBase>(
    private val main: MarkingDispatcher? = null
) : GetRecommendationResponseCallback<T> {
    val results = mutableListOf<Recoms<T>>()
    val rawResponses = mutableListOf<String>()
    val failures = mutableListOf<Triple<Int?, String?, Throwable?>>()
    val calledOnMain = mutableListOf<Boolean>()

    override fun onSuccess(response: Recoms<T>) {
        results += response
        calledOnMain += main?.isRunningHere == true
    }

    override fun onSuccessFallbackToJson(response: String) {
        rawResponses += response
        calledOnMain += main?.isRunningHere == true
    }

    override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
        failures += Triple(statusCode, response, throwable)
        calledOnMain += main?.isRunningHere == true
    }
}

internal class RecordingRecommendationJsonCallback(
    private val main: MarkingDispatcher? = null
) : GetRecommendationResponseJsonCallback {
    val results = mutableListOf<String>()
    val failures = mutableListOf<Triple<Int?, String?, Throwable?>>()
    val calledOnMain = mutableListOf<Boolean>()

    override fun onSuccess(response: String) {
        results += response
        calledOnMain += main?.isRunningHere == true
    }

    override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
        failures += Triple(statusCode, response, throwable)
        calledOnMain += main?.isRunningHere == true
    }
}
