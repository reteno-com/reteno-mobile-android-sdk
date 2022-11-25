package com.reteno.sample.model

import com.reteno.core.data.remote.model.recommendation.get.RecomBase

data class RecommendationResponseMinimal(
    override val productId: String,
    val descr: String?
) : RecomBase