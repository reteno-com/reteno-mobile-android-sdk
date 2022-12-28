package com.reteno.core.domain.model.ecom

import com.google.gson.annotations.SerializedName
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.ATTRIBUTES
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.CATEGORY
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.DISCOUNT
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.PRICE
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.PRODUCT_ID
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.PRODUCT_NAME
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.QUANTITY

data class ProductInCart(

    /**
     * ID of the product (required).
     */
    @SerializedName(PRODUCT_ID)
    val productId: String,

    /**
     * Quantity of items (required).
     */
    @SerializedName(QUANTITY)
    val quantity: Int,

    /**
     * Price per item (required).
     */
    @SerializedName(PRICE)
    val price: Double,

    /**
     * Calculated discount.
     */
    @SerializedName(DISCOUNT)
    val discount: Double? = null,

    /**
     * Product name.
     */
    @SerializedName(PRODUCT_NAME)
    val name: String? = null,

    /**
     * Product main category name.
     */
    @SerializedName(CATEGORY)
    val category: String? = null,

    /**
     * Extended product fields.
     */
    @SerializedName(ATTRIBUTES)
    val attributes: List<Attributes>? = null

)