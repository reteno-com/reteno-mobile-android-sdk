package com.reteno.core.domain.model.ecom

import com.google.gson.annotations.SerializedName
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.ATTRIBUTES
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.CART_ID
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.DATE
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.DELIVERY_ADDRESS
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.DELIVERY_METHOD
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.DISCOUNT
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.EMAIL
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.EXTERNAL_CUSTOMER_ID
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.EXTERNAL_ORDER_ID
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.FIRST_NAME
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.ITEMS
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.LAST_NAME
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.PAYMENT_METHOD
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.PHONE
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.RESTORE_URL
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.SHIPPING
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.SOURCE
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.STATUS
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.STATUS_DESCRIPTION
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.STORE_ID
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.TAXES
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.TOTAL_COST
import java.time.ZonedDateTime

data class Order(
    /**
     * Order ID in external system (required).
     */
    @SerializedName(EXTERNAL_ORDER_ID)
    val externalOrderId: String,
    /**
     * ID of customer in the external system
     */
    @SerializedName(EXTERNAL_CUSTOMER_ID)
    val externalCustomerId: String?,
    /**
     * Total cost of order (required).
     */
    @SerializedName(TOTAL_COST)
    val totalCost: Double,
    /**
     * Order status (required).
     */
    @SerializedName(STATUS)
    val status: OrderStatus,
    /**
     * Status changing date and time (required).
     */
    @SerializedName(DATE)
    val date: ZonedDateTime,
    /**
     * Shopping cart ID. Allows to match an order with shopping cart actions. *Important optional parameter*
     */
    @SerializedName(CART_ID)
    val cartId: String? = null,
    /**
     * Customer email address.
     */
    @SerializedName(EMAIL)
    val email: String? = null,
    /**
     * Customer phone number.
     */
    @SerializedName(PHONE)
    val phone: String? = null,
    /**
     * Customer first name.
     */
    @SerializedName(FIRST_NAME)
    val firstName: String? = null,
    /**
     * Customer last name.
     */
    @SerializedName(LAST_NAME)
    val lastName: String? = null,
    /**
     * Shipping cost.
     */
    @SerializedName(SHIPPING)
    val shipping: Double? = null,
    /**
     * Discount.
     */
    @SerializedName(DISCOUNT)
    val discount: Double? = null,
    /**
     * Amount of tax.
     */
    @SerializedName(TAXES)
    val taxes: Double? = null,
    /**
     * Link to order.
     */
    @SerializedName(RESTORE_URL)
    val restoreUrl: String? = null,
    /**
     * Order status description.
     */
    @SerializedName(STATUS_DESCRIPTION)
    val statusDescription: String? = null,
    /**
     * Store ID (if you work with several stores in one eSputnik account).
     */
    @SerializedName(STORE_ID)
    val storeId: String? = null,
    /**
     * "Online" \ "offline" values for segmentation. If the field is empty or some other value, by default the order will be accepted as online.
     */
    @SerializedName(SOURCE)
    val source: String? = null,
    /**
     * Delivery method.
     */
    @SerializedName(DELIVERY_METHOD)
    val deliveryMethod: String? = null,
    /**
     * Payment method.
     */
    @SerializedName(PAYMENT_METHOD)
    val paymentMethod: String? = null,
    /**
     * Delivery address.
     */
    @SerializedName(DELIVERY_ADDRESS)
    val deliveryAddress: String? = null,
    /**
     * Array of ordered products.
     */
    @SerializedName(ITEMS)
    val items: List<OrderItem>? = null,
    /**
     * Additional fields.
     */
    @SerializedName(ATTRIBUTES)
    val attributes: List<Attributes>? = null
)
