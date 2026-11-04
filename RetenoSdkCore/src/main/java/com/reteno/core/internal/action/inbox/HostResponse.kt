package com.reteno.core.internal.action.inbox

import com.reteno.core.domain.callback.appinbox.RetenoResultCallback
import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.network.ApiResult
import kotlinx.coroutines.withContext

internal suspend fun <T> ActionTrace.passResponseToHost(
    response: ApiResult,
    callback: RetenoResultCallback<T>,
    dispatchers: RetenoDispatchers,
    read: (String) -> T
) {
    if (response is ApiResult.Failure) {
        step("pass server failure to host callback") {
            withContext(dispatchers.main) { callback.onFailure(response.statusCode, response.body, response.error) }
        }
        return
    }
    val body = (response as ApiResult.Success).body
    val result = runCatching {
        step("read result from server response") {
            read(body)
        }
    }.getOrElse { error ->
        step("pass unreadable response to host callback") {
            withContext(dispatchers.main) { callback.onFailure(null, null, error) }
        }
        throw error
    }
    step("pass result to host callback") {
        withContext(dispatchers.main) { callback.onSuccess(result) }
    }
}
