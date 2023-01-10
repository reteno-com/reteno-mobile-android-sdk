package com.reteno.core.domain.model.ecom


internal object RemoteConstants {

    internal object EcomEvent {
        /** eventTypeKeys **/
        const val EVENT_TYPE_PRODUCT_VIEWED = "productViewed"
        const val EVENT_TYPE_PRODUCT_CATEGORY_VIEWED = "productCategoryViewed"
        const val EVENT_TYPE_PRODUCT_ADDED_TO_WISHLIST = "productAddedToWishlist"
        const val EVENT_TYPE_CART_UPDATED = "cartUpdated"
        const val EVENT_TYPE_ORDER_CREATED = "orderCreated"
        const val EVENT_TYPE_ORDER_UPDATED = "orderUpdated"
        const val EVENT_TYPE_ORDER_DELIVERED = "orderDelivered"
        const val EVENT_TYPE_ORDER_CANCELLED = "orderCancelled"
        const val EVENT_TYPE_SEARCH_REQUEST = "searchRequest"

        /** General **/
        const val CURRENCY_CODE = "currencyCode"
        const val PRICE = "price"
        const val CATEGORY = "category"
        const val EXTERNAL_ORDER_ID = "externalOrderId"
        const val CART_ID = "cartId"
        const val ATTRIBUTES = "attributes"

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
        const val PRODUCT_NAME = "name"
        const val PRODUCT_CATEGORY = "category"

        /** OrderCreated **/
        const val EXTERNAL_CUSTOMER_ID = "externalCustomerId"
        const val TOTAL_COST = "totalCost"
        const val STATUS = "status"
        const val DATE = "date"
        const val EMAIL = "email"
        const val PHONE = "phone"
        const val FIRST_NAME = "firstName"
        const val LAST_NAME = "lastName"
        const val SHIPPING = "shipping"
        const val ORDER_DISCOUNT = "discount"
        const val TAXES = "taxes"
        const val RESTORE_URL = "restoreUrl"
        const val STATUS_DESCRIPTION = "statusDescription"
        const val STORE_ID = "storeId"
        const val SOURCE = "source"
        const val DELIVERY_METHOD = "deliveryMethod"
        const val PAYMENT_METHOD = "paymentMethod"
        const val DELIVERY_ADDRESS = "deliveryAddress"
        const val ITEMS = "items"

        const val ORDER_ITEM_EXTERNAL_ITEM_ID = "externalItemId"
        const val ORDER_ITEM_NAME = "name"
        const val ORDER_ITEM_CATEGORY = "category"
        const val ORDER_ITEM_QUANTITY = "quantity"
        const val ORDER_ITEM_COST = "cost"
        const val ORDER_ITEM_URL = "url"
        const val ORDER_ITEM_IMAGE_URL = "imageUrl"
        const val ORDER_ITEM_DESCRIPTION = "description"

        /** SearchRequest **/
        const val SEARCH = "search"
        // boolean value in int format
        const val IS_FOUND = "isFound"
    }
}