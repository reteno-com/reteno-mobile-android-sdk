package com.reteno.core.domain.model.ecom

import com.google.gson.annotations.SerializedName
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.ATTRIBUTES
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.PRODUCT_CATEGORY_ID

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
