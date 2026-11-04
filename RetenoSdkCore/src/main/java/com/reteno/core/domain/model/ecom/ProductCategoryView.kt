package com.reteno.core.domain.model.ecom

import com.google.gson.annotations.SerializedName

data class ProductCategoryView(

    /**
     * ID of the category (required).
     */
    @SerializedName(PRODUCT_CATEGORY_ID)
    val productCategoryId: String,

    /**
     * Extended category attributes in list.
     */
    @SerializedName(ATTRIBUTES)
    val attributes: List<Attributes>? = null

)
