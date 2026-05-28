package com.reteno.sample.util

import android.widget.EditText
import com.reteno.core.util.Logger.d

object Util {
    fun getTextOrNull(editText: EditText): String? {
        val rawText = editText.getText().toString().trim()
        return rawText.ifEmpty { null }
    }

    fun getListFromEditText(editText: EditText): List<String>? {
        val rawText = editText.getText().toString().trim()
        return if (rawText.isEmpty()) null else listOf(
            *rawText.split(
                ",".toRegex()
            ).dropLastWhile { it.isEmpty() }.toTypedArray()
        )
    }

    fun saveParseInt(text: String?): Int? {
        if (text.isNullOrEmpty()) return null
        return try {
            text.toInt()
        } catch (e: Exception) {
            d("saveParseInt", e.message!!)
            null
        }
    }
}
