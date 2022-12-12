package com.reteno.core.domain.model.ecom

data class OrderItem(
    /**
     * ID of product in the external system (required).
     */
    val externalItemId: String,
    /**
     * Product name (required).
     */
    val name: String,
    /**
     * Product category (required).
     */
    val category: String,
    /**
     * Number of items (required).
     */
    val quantity: Double,
    /**
     * Product price (required).
     */
    val cost: Double,
    /**
     * Link to product page (required).
     */
    val url: String,
    /**
     * Link to a product image.
     */
    val imageUrl: String? = null,
    /**
     * Short description of product.
     */
    val description: String? = null
)
