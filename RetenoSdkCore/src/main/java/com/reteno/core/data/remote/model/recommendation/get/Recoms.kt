package com.reteno.core.data.remote.model.recommendation.get

import com.google.gson.annotations.SerializedName

data class Recoms<T: RecomBase>(
    @SerializedName(FIELD_NAME_RECOMS)
    val recoms: List<T>,
) {
    companion object {
        internal const val FIELD_NAME_RECOMS = "recoms"
    }
}
