package com.reteno.core.data.remote.model.iam.message

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class InAppMessageContent(
    @SerializedName("messageInstanceId")
    val messageInstanceId: Long,

    @SerializedName("layoutType")
    val layoutType: InAppLayoutType,

    @SerializedName("layoutParams")
    val layoutParams: InAppLayoutParams?,

    @SerializedName("model")
    val model: JsonElement
) {
    enum class InAppLayoutType(val key: String) {
        @SerializedName("FULL")
        FULL("FULL"),

        @SerializedName("SLIDE_UP")
        SLIDE_UP("SLIDE_UP"),

        @SerializedName("BOTTOM_BAR")
        BOTTOM_BAR("BOTTOM_BAR"),

        @SerializedName("POP_UP")
        POP_UP("POP_UP");

        companion object {
            fun from(key: String): InAppLayoutType {
                return values().firstOrNull { it.key == key } ?: FULL
            }
        }
    }

    data class InAppLayoutParams(
        @SerializedName("position")
        val position: Position?
    ) {
        enum class Position(val key: String) {
            @SerializedName("TOP")
            TOP("TOP"),

            @SerializedName("BOTTOM")
            BOTTOM("BOTTOM");

            companion object {
                fun from(key: String): Position {
                    return Position.values()
                        .firstOrNull { it.key == key } ?: TOP
                }
            }
        }
    }

    override fun toString(): String {
        return "InAppMessageContent(messageInstanceId=$messageInstanceId, layoutType=$layoutType, layoutParams=${layoutParams}, model=*removed for memory optimization*"
    }
}