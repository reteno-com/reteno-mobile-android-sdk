package com.reteno.core.domain.model.ecom

data class ProductView(

    /**
     * ID of the product.
     */
    val productId: String,

    /**
     * Price per item.
     */
    val price: Double,

    /**
     * Indicates if items are in stock.
     */
    val isInStock: Boolean,

    /**
     * Extended product attributes in list.
     */
    val attributes: List<Attributes>? = null

)
