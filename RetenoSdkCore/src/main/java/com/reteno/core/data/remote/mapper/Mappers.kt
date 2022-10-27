package com.reteno.core.data.remote.mapper

import com.google.gson.Gson

fun Any.toJson(): String = Gson().toJson(this)

inline fun <reified T> String.fromJson(): T =
    Gson().fromJson(this, T::class.java)

inline fun <reified T> String.fromJsonOrNull(): T? =
    if (isBlank()) {
        null
    } else {
        Gson().fromJson(this, T::class.java)
    }