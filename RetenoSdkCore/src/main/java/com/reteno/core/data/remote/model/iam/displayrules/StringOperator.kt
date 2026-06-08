package com.reteno.core.data.remote.model.iam.displayrules

enum class StringOperator {
    EQUALS,
    CONTAINS,
    CONTAINS_ONE_OF,
    STARTS_WITH,
    ENDS_WITH,
    REG_EX,
    DOES_NOT_EQUAL;

    companion object {
        fun fromString(name: String?): StringOperator? {
            return when (name) {
                "EQUALS" -> EQUALS
                "CONTAINS" -> CONTAINS
                "CONTAINS_ONE_OF" -> CONTAINS_ONE_OF
                "STARTS_WITH" -> STARTS_WITH
                "ENDS_WITH" -> ENDS_WITH
                "REG_EX" -> REG_EX
                "DOES_NOT_EQUAL" -> DOES_NOT_EQUAL
                else -> null
            }
        }
    }
}