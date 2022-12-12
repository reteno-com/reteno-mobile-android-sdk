package com.reteno.core.domain.model.ecom

import java.time.ZonedDateTime

data class OrderBuilder(
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
    val date: ZonedDateTime
) {
    /**
     * Shopping cart ID. Allows to match an order with shopping cart actions. *Important optional parameter*
     */
    var cartId: String? = null

    /**
     * Customer email address.
     */
    var email: String? = null

    /**
     * Customer phone number.
     */
    var phone: String? = null

    /**
     * Customer first name.
     */
    var firstName: String? = null

    /**
     * Customer last name.
     */
    var lastName: String? = null

    /**
     * Shipping cost.
     */
    var shipping: Double? = null

    /**
     * Discount.
     */
    var discount: Double? = null

    /**
     * Amount of tax.
     */
    var taxes: Double? = null

    /**
     * Link to order.
     */
    var restoreUrl: String? = null

    /**
     * Order status description.
     */
    var statusDescription: String? = null

    /**
     * Store ID (if you work with several stores in one eSputnik account).
     */
    var storeId: String? = null

    /**
     * "Online" \ "offline" values for segmentation. If the field is empty or some other value, by default the order will be accepted as online.
     */
    var source: String? = null

    /**
     * Delivery method.
     */
    var deliveryMethod: String? = null

    /**
     * Payment method.
     */
    var paymentMethod: String? = null

    /**
     * Delivery address.
     */
    var deliveryAddress: String? = null

    /**
     * Array of ordered products.
     */
    var items: List<OrderItem>? = null

    /**
     * Additional fields.
     */
    var attributes: List<Attributes>? = null

    fun build(): Order = Order(
        externalOrderId = externalOrderId,
        externalCustomerId = externalCustomerId,
        totalCost = totalCost,
        status = status,
        date = date,
        cartId = cartId,
        email = email,
        phone = phone,
        firstName = firstName,
        lastName = lastName,
        shipping = shipping,
        discount = discount,
        taxes = taxes,
        restoreUrl = restoreUrl,
        statusDescription = statusDescription,
        storeId = storeId,
        source = source,
        deliveryMethod = deliveryMethod,
        paymentMethod = paymentMethod,
        deliveryAddress = deliveryAddress,
        items = items,
        attributes = attributes
    )
}
