package com.reteno.core.domain.callback.appinbox


interface RetenoResultCallback<T> {

    fun onSuccess(result: T)

    fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?)
}