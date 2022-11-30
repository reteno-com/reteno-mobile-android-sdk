package com.reteno.core.data.local.model.appinbox

enum class AppInboxMessageStatusDb {
    OPENED;

    companion object {
        @JvmStatic
        fun fromString(value: String?): AppInboxMessageStatusDb = OPENED
    }
}
