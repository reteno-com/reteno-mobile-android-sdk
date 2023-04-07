package com.reteno.core.domain

interface ResponseCallback {

    fun onSuccess(headers: Map<String, List<String>>, response: String) {
        onSuccess(response)
    }

    fun onSuccess(response: String)

    fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?)

}