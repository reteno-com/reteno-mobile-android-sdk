package com.reteno.core.domain

internal sealed class ResultDomain<out Success : Any> {
    data class Success<Success : Any>(val body: Success) : ResultDomain<Success>()

    data class Error(val errorBody: String, val code: Int = -1) : ResultDomain<Nothing>()

    object Loading : ResultDomain<Nothing>()
}