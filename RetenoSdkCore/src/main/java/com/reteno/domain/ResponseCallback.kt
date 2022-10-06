package com.reteno.domain

interface ResponseCallback {

    fun onSuccess(response: String)

    fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?)

}