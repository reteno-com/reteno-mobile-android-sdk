package com.reteno.core.domain.model.recommendation.get

/**
 * This class describes the paramter model for getting Recommendations from Reteno SDK
 *
 * Return all product fields if a request does not contain fields parameter.
 *
 * Return only productId if fields array in a request is empty.
 *
 *
 * @property products product IDs for product-based algorithms
 * @property category product category key for category-based algorithms
 * @property fields fields to return in response. If fields is null - all fields in the model returned
 * @property filters additional algorithm filters map. Filters are not supported yet. Reserved for the future.
 */
data class RecomRequest @JvmOverloads constructor(
    val products: List<String>?,
    val category: String?,
    val fields: List<String>? = null,
    val filters: RecomFilter? = null
)
