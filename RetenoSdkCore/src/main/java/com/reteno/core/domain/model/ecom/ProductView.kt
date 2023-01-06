package com.reteno.core.domain.model.ecom

import com.google.gson.annotations.SerializedName
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.ATTRIBUTES
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.IS_IN_STOCK
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.PRICE
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.PRODUCT_ID

data class ProductView(

    /**
     * ID of the product (required).
     */
    @SerializedName(PRODUCT_ID)
    val productId: String,

    /**
     * Price per item (required).
     */
    @SerializedName(PRICE)
    val price: Double,

    /**
     * Indicates if items are in stock (required).
     */
    @SerializedName(IS_IN_STOCK)
    val isInStock: Boolean,

    /**
     * Extended product attributes in list.
     */
    @SerializedName(ATTRIBUTES)
    val attributes: List<Attributes>? = null

)
