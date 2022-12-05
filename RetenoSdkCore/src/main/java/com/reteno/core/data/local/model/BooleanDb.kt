package com.reteno.core.data.local.model

enum class BooleanDb {
    TRUE,
    FALSE;

    companion object {
        @JvmStatic
        fun fromString(value: String?): BooleanDb? =
            when (value) {
                TRUE.toString() -> TRUE
                FALSE.toString() -> FALSE
                else -> null
            }
    }
}