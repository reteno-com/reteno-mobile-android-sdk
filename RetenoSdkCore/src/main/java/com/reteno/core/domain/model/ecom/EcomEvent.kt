package com.reteno.core.domain.model.ecom

import java.time.ZonedDateTime

sealed class EcomEvent(open val occurred: ZonedDateTime) {

    /**
     * Track a product card a user is viewing to rank items / categories and send triggers for abandoned browses.
     *
     * @param product that has been viewed.
     * @param currencyCode if is not set then org's default is used.
     *
     * @see com.reteno.core.domain.model.ecom.ProductView
     */
    data class ProductViewed @JvmOverloads constructor(
        val product: ProductView,
        val currencyCode: String? = null,
        override val occurred: ZonedDateTime = ZonedDateTime.now()
    ) : EcomEvent(occurred)

    /**
     * Track a product category a user is viewing for triggers like Website visit with a category view and Website visit without a category view.
     *
     * @param category ProductCategoryView model
     *
     * @see com.reteno.core.domain.model.ecom.ProductCategoryView
     */
    data class ProductCategoryViewed @JvmOverloads constructor(
        val category: ProductCategoryView,
        override val occurred: ZonedDateTime = ZonedDateTime.now()
    ) : EcomEvent(occurred)

    /**
     * Track adding product to a wishlist to calculate and display recoms and send triggers related to a wishlist.
     *
     * @param product that have been added to wishlist.
     * @param currencyCode If is not set then org's default is used.
     *
     * @see com.reteno.core.domain.model.ecom.ProductView
     */
    data class ProductAddedToWishlist @JvmOverloads constructor(
        val product: ProductView,
        val currencyCode: String? = null,
        override val occurred: ZonedDateTime = ZonedDateTime.now()
    ) : EcomEvent(occurred)

    /**
     * Track updating a shopping cart for triggers.
     *
     * @param cartId Shopping cart ID.
     * @param products
     * @param currencyCode If is not set then org's default is used.
     *
     * @see com.reteno.core.domain.model.ecom.ProductInCart
     */
    data class CartUpdated @JvmOverloads constructor(
        val cartId: String,
        val products: List<ProductInCart>,
        val currencyCode: String? = null,
        override val occurred: ZonedDateTime = ZonedDateTime.now()
    ) : EcomEvent(occurred)

    /**
     * Create an order.
     *
     * Requirements:
     *  - Fill the order required parameters. The system ignores an event if any of required parameters is missed.
     *  - Extend event parameters with non-standard order attributes using `attributes` field if necessary.
     *
     *  @param order OrderModel.
     *  @param currencyCode If is not set then org's default is used.
     *
     *  @see com.reteno.core.domain.model.ecom.Order
     */
    data class OrderCreated @JvmOverloads constructor(
        val order: Order,
        val currencyCode: String? = null,
        override val occurred: ZonedDateTime = ZonedDateTime.now()
    ) : EcomEvent(occurred)

    /**
     * Update an order with the specified [Order.externalOrderId] value.
     * If an order does not exist then the system creates it.
     * If an order must be created then requirements to orderCreated are applied.
     *
     *  @param order OrderModel.
     *  @param currencyCode If is not set then org's default is used.
     *  @param attributes Extended order fields.
     *
     *  @see com.reteno.core.domain.model.ecom.Order
     */
    data class OrderUpdated @JvmOverloads constructor(
        val order: Order,
        val currencyCode: String? = null,
        val attributes: List<Attributes>? = null,
        override val occurred: ZonedDateTime = ZonedDateTime.now()
    ) : EcomEvent(occurred)

    /**
     * Updates a status of an order with the specified [externalOrderId] to ***DELIVERED***.
     * If an order does not exist then an event is ignored.
     *
     * @param externalOrderId Order ID in external system.
     */
    data class OrderDelivered @JvmOverloads constructor(
        val externalOrderId: String,
        override val occurred: ZonedDateTime = ZonedDateTime.now()
    ) : EcomEvent(occurred)

    /**
     * Change an existing order status to ***CANCELLED***.
     *
     * @param externalOrderId Order ID in external system.
     */
    data class OrderCancelled @JvmOverloads constructor(
        val externalOrderId: String,
        override val occurred: ZonedDateTime = ZonedDateTime.now()
    ) : EcomEvent(occurred)

    /**
     * Track search requests for triggers like Abandoned search.
     *
     * @param search Value from the search string. What we are looking for on the site.
     * @param isFound true is search returned results. False by default.
     */
    data class SearchRequest @JvmOverloads constructor(
        val search: String,
        val isFound: Boolean = false,
        override val occurred: ZonedDateTime = ZonedDateTime.now()
    ) : EcomEvent(occurred)

}
