package com.reteno.data.remote.mapper

import com.google.gson.Gson

fun Any.toJson(): String {
    return Gson().toJson(this)
}

internal inline fun <reified T> String.toObject() : T {
    return Gson().fromJson(this, T::class.java)
}