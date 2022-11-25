package com.reteno.core.data.remote.mapper

import com.reteno.core.data.remote.model.recommendation.get.RecomFilterRemote
import com.reteno.core.data.remote.model.recommendation.get.RecomRequestRemote
import com.reteno.core.domain.model.recommendation.get.RecomFilter
import com.reteno.core.domain.model.recommendation.get.RecomRequest

fun RecomRequest.toRemote() = RecomRequestRemote(
    products = products,
    category = category,
    fields = fields,
    filters = filters?.toRemote()
)

fun RecomFilter.toRemote() = RecomFilterRemote(
    name = name,
    values = values
)