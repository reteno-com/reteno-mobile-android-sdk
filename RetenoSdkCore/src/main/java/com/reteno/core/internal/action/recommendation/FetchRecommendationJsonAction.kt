package com.reteno.core.internal.action.recommendation

import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.HttpMethod
import com.reteno.core.features.recommendation.GetRecommendationResponseJsonCallback
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.coroutine.InitGate
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.network.ApiResult
import com.reteno.core.internal.network.SuspendApiClient
import kotlinx.coroutines.withContext

internal class FetchRecommendationJsonAction(
    private val api: SuspendApiClient,
    private val initGate: InitGate,
    private val dispatchers: RetenoDispatchers,
    private val runner: ActionRunner
) {

    suspend operator fun invoke(
        recomVariantId: String,
        requestBody: String,
        callback: GetRecommendationResponseJsonCallback
    ): Result<Unit> = runner("FetchRecommendationJson") {
        args("recomVariantId" to recomVariantId)
        step("wait for sdk initialization") {
            initGate.awaitInit()
        }
        val response = step("load recommendations from server") {
            api.request(HttpMethod.POST, ApiContract.Recommendation.GetRecoms(recomVariantId), requestBody)
        }
        answerHost(response, callback)
    }

    private suspend fun ActionTrace.answerHost(
        response: ApiResult,
        callback: GetRecommendationResponseJsonCallback
    ) {
        if (response is ApiResult.Failure) {
            step("pass server failure to host callback") {
                withContext(dispatchers.main) { callback.onFailure(response.statusCode, response.body, response.error) }
            }
            return
        }
        val body = (response as ApiResult.Success).body
        step("pass recommendations to host callback") {
            withContext(dispatchers.main) { callback.onSuccess(body) }
        }
    }
}
