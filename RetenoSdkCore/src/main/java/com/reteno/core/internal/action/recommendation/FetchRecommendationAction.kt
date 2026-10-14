package com.reteno.core.internal.action.recommendation

import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.HttpMethod
import com.reteno.core.data.remote.mapper.convertRecoms
import com.reteno.core.data.remote.model.recommendation.get.RecomBase
import com.reteno.core.features.recommendation.GetRecommendationResponseCallback
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.action.trace.stepOrNull
import com.reteno.core.internal.coroutine.InitGate
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.network.ApiResult
import com.reteno.core.internal.network.SuspendApiClient
import kotlinx.coroutines.withContext

internal class FetchRecommendationAction(
    private val api: SuspendApiClient,
    private val initGate: InitGate,
    private val dispatchers: RetenoDispatchers,
    private val runner: ActionRunner
) {

    suspend operator fun <T : RecomBase> invoke(
        recomVariantId: String,
        requestBody: String,
        responseClass: Class<T>,
        callback: GetRecommendationResponseCallback<T>
    ): Result<Unit> = runner("FetchRecommendation") {
        args("recomVariantId" to recomVariantId, "responseClass" to responseClass.simpleName)
        step("wait for sdk initialization") {
            initGate.awaitInit()
        }
        val response = step("load recommendations from server") {
            api.request(HttpMethod.POST, ApiContract.Recommendation.GetRecoms(recomVariantId), requestBody)
        }
        answerHost(response, responseClass, callback)
    }

    private suspend fun <T : RecomBase> ActionTrace.answerHost(
        response: ApiResult,
        responseClass: Class<T>,
        callback: GetRecommendationResponseCallback<T>
    ) {
        if (response is ApiResult.Failure) {
            step("pass server failure to host callback") {
                withContext(dispatchers.main) { callback.onFailure(response.statusCode, response.body, response.error) }
            }
            return
        }
        val body = (response as ApiResult.Success).body
        val recoms = stepOrNull("read recommendations from server response", detail = { "count=${it.recoms.size}" }) {
            body.convertRecoms(responseClass)
        }
        if (recoms == null) {
            step("pass raw response to host callback") {
                withContext(dispatchers.main) { callback.onSuccessFallbackToJson(body) }
            }
            return
        }
        step("pass recommendations to host callback") {
            withContext(dispatchers.main) { callback.onSuccess(recoms) }
        }
    }
}
