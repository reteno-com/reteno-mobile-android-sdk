package com.reteno.core.domain.model.ecom

data class ProductCategoryView(

    /**
     * ID of the category.
     */
    val productCategoryId: String,

    /**
     * Extended category attributes in list.
     */
    val attributes: List<Attributes>? = null

)
