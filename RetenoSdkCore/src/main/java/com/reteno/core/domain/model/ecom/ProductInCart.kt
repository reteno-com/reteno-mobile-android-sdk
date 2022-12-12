package com.reteno.core.domain.model.ecom

data class ProductInCart(

    /**
     * ID of the product (required).
     */
    val productId: String,

    /**
     * Quantity of items (required).
     */
    val quantity: Int,

    /**
     * Price per item (required).
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