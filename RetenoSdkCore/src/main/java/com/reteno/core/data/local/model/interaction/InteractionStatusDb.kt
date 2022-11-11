package com.reteno.core.data.local.model.interaction

import com.reteno.core.domain.model.interaction.InteractionStatus

enum class InteractionStatusDb {
    DELIVERED,
    OPENED;

    companion object {
        @JvmStatic
        fun fromString(value: String?): InteractionStatusDb =
            when (value) {
                InteractionStatus.DELIVERED.toString() -> InteractionStatusDb.DELIVERED
                InteractionStatus.OPENED.toString() -> InteractionStatusDb.OPENED
                else -> InteractionStatusDb.DELIVERED
            }
    }
}
