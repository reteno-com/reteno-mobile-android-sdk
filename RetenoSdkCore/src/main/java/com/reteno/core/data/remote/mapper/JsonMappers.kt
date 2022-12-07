package com.reteno.core.data.remote.mapper

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.reteno.core.data.remote.model.recommendation.get.RecomBase
import com.reteno.core.data.remote.model.recommendation.get.Recoms
import org.json.JSONObject
import java.lang.reflect.Type


internal fun Any.toJson(): String = Gson().toJson(this)

internal fun Any?.toJsonOrNull(): String? = this?.let(Gson()::toJson)

inline fun <reified T> String.fromJson(): T =
    Gson().fromJson(this, T::class.java)

fun <T> String.fromJson(classOfT: Class<T>):T =
    Gson().fromJson(this, classOfT)

internal inline fun <reified T> String.listFromJson(): List<T> {
    val listType: Type = object : TypeToken<ArrayList<T?>?>() {}.type
    return Gson().fromJson(this, listType)
}

internal inline fun <reified T> String.listFromJsonOrNull(): T? =
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

internal fun <T : RecomBase> String.convertRecoms(responseClass: Class<T>): Recoms<T> {
    val recomList = mutableListOf<T>()
    val jsonObjectRoot = JSONObject(this)
    val recomsJsonArray = jsonObjectRoot.getJSONArray(Recoms.FIELD_NAME_RECOMS)
    for (i in 0 until recomsJsonArray.length()) {
        val singleRecom = recomsJsonArray.getJSONObject(i).toString().fromJson(responseClass)
        recomList.add(singleRecom)
    }
    return Recoms(recomList)
}