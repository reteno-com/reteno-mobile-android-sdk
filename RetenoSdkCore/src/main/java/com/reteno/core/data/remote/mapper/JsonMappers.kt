package com.reteno.core.data.remote.mapper

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type


fun Any.toJson(): String = Gson().toJson(this)

fun Any?.toJsonOrNull(): String? = this?.let(Gson()::toJson)

inline fun <reified T> String.fromJson(): T =
    Gson().fromJson(this, T::class.java)

inline fun <T> JsonElement.fromJson(classOfT: Class<T>):T =
    Gson().fromJson(this, classOfT)

inline fun <reified T> String.listFromJson(): List<T> {
    val listType: Type = object : TypeToken<ArrayList<T?>?>() {}.type
    return Gson().fromJson(this, listType)
}

inline fun <reified T> String.listFromJsonOrNull(): T? =
    if (isBlank()) {
        null
    } else {
        val listType: Type = object : TypeToken<ArrayList<T?>?>() {}.type
        Gson().fromJson(this, listType)
    }

inline fun <reified T> String.fromJsonOrNull(): T? =
    if (isBlank()) {
        null
    } else {
        Gson().fromJson(this, T::class.java)
    }