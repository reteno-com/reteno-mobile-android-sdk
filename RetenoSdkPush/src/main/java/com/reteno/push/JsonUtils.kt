package com.reteno.push

import org.json.JSONArray
import org.json.JSONObject

internal object JsonUtils {

    internal fun JSONObject.getStringOrNull(key: String) =
        if (has(key)) {
            getString(key)
        } else {
            null
        }

    internal fun jsonObjectToMap(json: JSONObject): HashMap<String, Any?> {
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
        return when (value) {
            is JSONObject -> jsonObjectToMap(value)
            is JSONArray -> jsonArrayToList(value)
            else -> value
        }
    }

    private fun jsonArrayToList(array: JSONArray): List<Any> {
        val list: MutableList<Any> = ArrayList()
        for (i in 0 until array.length()) {
            list.add(convertNestedJSONType(array[i]))
        }
        return list
    }
}
