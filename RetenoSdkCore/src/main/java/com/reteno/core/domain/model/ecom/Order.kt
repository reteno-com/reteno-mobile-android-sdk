package com.reteno.core.domain.model.ecom

import java.time.ZonedDateTime

data class Order(
    /**
     * Order ID in external system (required).
     */
    val externalOrderId: String,
    /**
     * ID of customer in the external system
     */
    val externalCustomerId: String?,
    /**
     * Total cost of order (required).
     */
    val totalCost: Double,
    /**
     * Order status (required).
     */
    val status: OrderStatus,
    /**
     * Status changing date and time (required).
     */
    val date: ZonedDateTime,
    /**
     * Shopping cart ID. Allows to match an order with shopping cart actions. *Important optional parameter*
     */
    val cartId: String? = null,
    /**
     * Customer email address.
     */
    val email: String? = null,
    /**
     * Customer phone number.
     */
    val phone: String? = null,
    /**
     * Customer first name.
     */
    val firstName: String? = null,
    /**
     * Customer last name.
     */
    val lastName: String? = null,
    /**
     * Shipping cost.
     */
    val shipping: Double? = null,
    /**
     * Discount.
     */
    val discount: Double? = null,
    /**
     * Amount of tax.
     */
    val taxes: Double? = null,
    /**
     * Link to order.
     */
    val restoreUrl: String? = null,
    /**
     * Order status description.
     */
    val statusDescription: String? = null,
    /**
     * Store ID (if you work with several stores in one eSputnik account).
     */
    val storeId: String? = null,
    /**
     * "Online" \ "offline" values for segmentation. If the field is empty or some other value, by default the order will be accepted as online.
     */
    val source: String? = null,
    /**
     * Delivery method.
     */
    val deliveryMethod: String? = null,
    /**
     * Payment method.
     */
    val paymentMethod: String? = null,
    /**
     * Delivery address.
     */
    val deliveryAddress: String? = null,
    /**
     * Array of ordered products.
     */
    val items: List<OrderItem>? = null,
    /**
     * Additional fields.
     */
    val attributes: List<Attributes>? = null
)
