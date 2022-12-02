package com.reteno.core.domain.model.ecom

import java.time.ZonedDateTime

sealed class EcomEvent(open val occurred: ZonedDateTime) {

    /**
     * Track a product card a user is viewing to rank items / categories and send triggers for abandoned browses.
     *
     * @param product that has been viewed.
     * @param currencyCode if is not set then org's default is used.
     */
    data class ProductViewed(
        val product: ProductView,
        val currencyCode: String? = null,
        override val occurred: ZonedDateTime = ZonedDateTime.now()
    ) : EcomEvent(occurred)

    /**
     * Track a product category a user is viewing for triggers like Website visit with a category view and Website visit without a category view.
     *
     * @param category
     */
    data class ProductCategoryViewed(
        val category: ProductCategoryView,
        override val occurred: ZonedDateTime = ZonedDateTime.now()
    ) : EcomEvent(occurred)

    /**
     * Track adding product to a wishlist to calculate and display recoms and send triggers related to a wishlist.
     *
     * @param product that have been added to wishlist.
     * @param currencyCode If is not set then org's default is used.
     */
    data class ProductAddedToWishlist(
        val product: ProductView,
        val currencyCode: String? = null,
        override val occurred: ZonedDateTime = ZonedDateTime.now()
    ) : EcomEvent(occurred)

    /**
     * Track updating a shopping cart for triggers.
     *
     * @param cardId Shopping cart ID.
     * @param products
     * @param currencyCode If is not set then org's default is used.
     */
    data class CartUpdated(
        val cardId: String,
        val products: List<ProductInCard>,
        val currencyCode: String? = null,
        override val occurred: ZonedDateTime = ZonedDateTime.now()
    ) : EcomEvent(occurred)

    /**
     * Create an order.
     *
     * Requirements:
     *  - Name standard order parameters exactly as they are named in the Public API POST /v1/order method.
     *  - Fill the order required parameters. The system ignores an event if any of required parameters is missed.
     *  - Extend event parameters with non-standard order attributes using `attributes` field if necessary.
     *
     *  @param externalOrderId Order ID in external system.
     *  @param totalCost Total cost of order.
     *  @param status Order status.
     *  @param data Status changing date and time.
     *  @param cartId Shopping cart ID. Allows to match an order with shopping cart actions. *Important optional parameter*
     *  @param currencyCode If is not set then org's default is used.
     *  @param attributes Extended order fields.
     */
    data class OrderCreated(
        val externalOrderId: String,
        val totalCost: Double,
        val status: OrderStatus,
        val data: ZonedDateTime,
        val cartId: String?,
        val currencyCode: String? = null,
        val attributes: List<Attributes>? = null,
        override val occurred: ZonedDateTime = ZonedDateTime.now()
    ) : EcomEvent(occurred)

    /**
     * Update an order with the specified [externalOrderId] value.
     * If an order does not exist then the system creates it.
     * If an order must be created then requirements to orderCreated are applied.
     *
     * Requirements:
     * - Name order parameters exactly as in the Public API POST /v1/order method.
     *
     * @param externalOrderId Order ID in external system.
     */
    data class OrderUpdated(
        val externalOrderId: String,
        override val occurred: ZonedDateTime = ZonedDateTime.now()
    ) : EcomEvent(occurred)

    /**
     * Updates a status of an order with the specified [externalOrderId] to ***DELIVERED***.
     * If an order does not exist then an event is ignored.
     *
     * @param externalOrderId Order ID in external system.
     */
    data class OrderDelivered(
        val externalOrderId: String,
        override val occurred: ZonedDateTime = ZonedDateTime.now()
    ) : EcomEvent(occurred)

    /**
     * Change an existing order status to ***CANCELLED***.
     *
     * @param externalOrderId Order ID in external system.
     */
    data class OrderCancelled(
        val externalOrderId: String,
        override val occurred: ZonedDateTime = ZonedDateTime.now()
    ) : EcomEvent(occurred)

    /**
     * Track search requests for triggers like Abandoned search.
     *
     * @param search Value from the search string. What we are looking for on the site.
     * @param isFound true is search returned results. False by default.
     */
    data class SearchRequest(
        val search: String, val isFound: Boolean = false,
        override val occurred: ZonedDateTime = ZonedDateTime.now()
    ) : EcomEvent(occurred)

}
