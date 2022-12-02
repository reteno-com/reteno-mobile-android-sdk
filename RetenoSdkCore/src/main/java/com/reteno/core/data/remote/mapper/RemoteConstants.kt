package com.reteno.core.data.remote.mapper


object RemoteConstants {

    object EcomEvent {
        /** eventTypeKeys **/
        const val EVENT_TYPE_PRODUCT_VIEWED = "productViewed"
        const val EVENT_TYPE_PRODUCT_CATEGORY_VIEWED = "productCategoryViewed"
        const val EVENT_TYPE_PRODUCT_ADDED_TO_WISHLIST = "productAddedToWishlist"
        const val EVENT_TYPE_CART_UPDATED = "cartUpdated"
        const val EVENT_TYPE_ORDER_CREATED = "orderCreated"
        const val EVENT_TYPE_ORDER_UPDATED = "orderUpdated"
        const val EVENT_TYPE_ORDER_DELIVERED = "orderDelivered"
        const val EVENT_TYPE_ORDER_CANCELLED = "productViewed"
        const val EVENT_TYPE_SEARCH_REQUEST = "productViewed"

        /** General **/
        const val CURRENCY_CODE = "currencyCode"
        const val PRICE = "price"
        const val CATEGORY = "category"
        const val EXTERNAL_ORDER_ID = "externalOrderId"
        const val CART_ID = "cartId"

        /** ProductViewed  **/
        const val PRODUCT = "product"
        const val PRODUCT_ID = "productId"
        // boolean value in int format
        const val IS_IN_STOCK = "isInStock"

        /** ProductCategoryView **/
        const val PRODUCT_CATEGORY_ID = "productCategoryId"

        /** CardUpdated **/
        const val PRODUCTS = "products"
        const val QUANTITY = "quantity"
        const val DISCOUNT = "discount"

        /** OrderCreated **/
        const val TOTAL_COST = "totalCost"
        const val STATUS = "status"
        const val DATE = "date"

        /** SearchRequest **/
        const val SEARCH = "search"
        // boolean value in int format
        const val IS_FOUND = "isFound"
    }
}