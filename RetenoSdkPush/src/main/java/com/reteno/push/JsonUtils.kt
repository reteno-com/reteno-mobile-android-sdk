package com.reteno.push

import org.json.JSONArray
import org.json.JSONObject

internal object JsonUtils {

    internal fun JSONObject.getJSONObjectOrNull(key: String) =
        if (has(key)) {
            getJSONObject(key)
        } else {
            null
        }

    internal fun JSONObject.getStringOrNull(key: String) =
        if (has(key)) {
            getString(key)
        } else {
            null
        }

    internal fun jsonObjectToMap(json: JSONObject): HashMap<String, Any?>? {
        return if (json === JSONObject.NULL) null else jsonObjectToMapNonNull(json)
    }

    private fun jsonObjectToMapNonNull(json: JSONObject): HashMap<String, Any?> {
        val map = HashMap<String, Any?>()
        val keysItr = json.keys()
        while (keysItr.hasNext()) {
            val key = keysItr.next()
            val value = json[key]
            map[key] = convertNestedJSONType(value)
        }
        return map
    }

    private fun convertNestedJSONType(value: Any): Any {
        if (value is JSONObject) return jsonObjectToMapNonNull(value)
        return if (value is JSONArray) jsonArrayToListNonNull(value) else value
    }

    private fun jsonArrayToListNonNull(array: JSONArray): List<Any> {
        val list: MutableList<Any> = ArrayList()
        for (i in 0 until array.length()) {
            val value = array[i]
            list.add(convertNestedJSONType(value))
        }
        return list
    }
}