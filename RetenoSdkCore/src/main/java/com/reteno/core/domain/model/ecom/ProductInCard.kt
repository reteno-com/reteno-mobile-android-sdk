package com.reteno.core.domain.model.ecom

data class ProductInCard(

    /**
     * ID of the product.
     */
    val productId: String,

    /**
     * Quantity of items.
     */
    val quantity: Int,

    /**
     * Price per item.
     */
    val price: Double,

    /**
     * Calculated discount.
     */
    val discount: Double? = null,

    /**
     * Product name.
     */
    val name: String? = null,

    /**
     * Product main category name.
     */
    val category: String? = null,

    /**
     * Extended product fields.
     */
    val attributes: List<Attributes>? = null

)