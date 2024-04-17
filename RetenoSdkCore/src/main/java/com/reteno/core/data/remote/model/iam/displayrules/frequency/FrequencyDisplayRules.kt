package com.reteno.core.data.remote.model.iam.displayrules.frequency

class FrequencyDisplayRules {
    val predicates = mutableListOf<FrequencyRule>()

    override fun equals(other: Any?): Boolean {
        return if (other is FrequencyDisplayRules)
            this.predicates == other.predicates
        else super.equals(other)

    }

    override fun hashCode(): Int {
        return predicates.hashCode()
    }
}