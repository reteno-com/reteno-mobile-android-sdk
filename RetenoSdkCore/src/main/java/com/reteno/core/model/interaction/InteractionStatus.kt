package com.reteno.core.model.interaction

enum class InteractionStatus {
    DELIVERED,
    OPENED;

    companion object {
        @JvmStatic
        fun fromString(value: String?): InteractionStatus =
            when (value) {
                DELIVERED.toString() -> DELIVERED
                OPENED.toString() -> OPENED
                else -> DELIVERED
            }
    }
}