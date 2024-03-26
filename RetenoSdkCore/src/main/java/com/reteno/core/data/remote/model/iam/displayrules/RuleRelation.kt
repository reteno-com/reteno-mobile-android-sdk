package com.reteno.core.data.remote.model.iam.displayrules

enum class RuleRelation {
    AND, OR;

    companion object {
        fun fromString(name: String?): RuleRelation? {
            return when (name) {
                "AND" -> AND
                "OR" -> OR
                else -> null
            }
        }
    }
}