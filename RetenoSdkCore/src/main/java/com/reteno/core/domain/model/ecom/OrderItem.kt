package com.reteno.core.domain.model.ecom

import com.google.gson.annotations.SerializedName
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.ORDER_ITEM_CATEGORY
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.ORDER_ITEM_COST
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.ORDER_ITEM_DESCRIPTION
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.ORDER_ITEM_EXTERNAL_ITEM_ID
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.ORDER_ITEM_IMAGE_URL
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.ORDER_ITEM_NAME
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.ORDER_ITEM_QUANTITY
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.ORDER_ITEM_URL

data class OrderItem(
    /**
     * ID of product in the external system (required).
     */
    @SerializedName(ORDER_ITEM_EXTERNAL_ITEM_ID)
    val externalItemId: String,
    /**
     * Product name (required).
     */
    @SerializedName(ORDER_ITEM_NAME)
    val name: String,
    /**
     * Product category (required).
     */
    @SerializedName(ORDER_ITEM_CATEGORY)
    val category: String,
    /**
     * Number of items (required).
     */
    @SerializedName(ORDER_ITEM_QUANTITY)
    val quantity: Double,
    /**
     * Product price (required).
     */
    @SerializedName(ORDER_ITEM_COST)
    val cost: Double,
    /**
     * Link to product page (required).
     */
    @SerializedName(ORDER_ITEM_URL)
    val url: String,
    /**
     * Link to a product image.
     */
    @SerializedName(ORDER_ITEM_IMAGE_URL)
    val imageUrl: String? = null,
    /**
     * Short description of product.
     */
    @SerializedName(ORDER_ITEM_DESCRIPTION)
    val description: String? = null
)
