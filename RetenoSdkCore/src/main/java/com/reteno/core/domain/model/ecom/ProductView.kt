package com.reteno.core.domain.model.ecom

data class ProductView(

    /**
     * ID of the product (required).
     */
    val productId: String,

    /**
     * Price per item (required).
     */
    val price: Double,

    /**
     * Indicates if items are in stock (required).
     */
    val isInStock: Boolean,

    /**
     * Extended product attributes in list.
     */
    val attributes: List<Attributes>? = null

)
