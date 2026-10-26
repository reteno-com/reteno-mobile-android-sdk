package com.reteno.core.domain.model.ecom

/** eventTypeKeys **/
internal const val EVENT_TYPE_PRODUCT_VIEWED = "productViewed"
internal const val EVENT_TYPE_PRODUCT_CATEGORY_VIEWED = "productCategoryViewed"
internal const val EVENT_TYPE_PRODUCT_ADDED_TO_WISHLIST = "productAddedToWishlist"
internal const val EVENT_TYPE_CART_UPDATED = "cartUpdated"
internal const val EVENT_TYPE_ORDER_CREATED = "orderCreated"
internal const val EVENT_TYPE_ORDER_UPDATED = "orderUpdated"
internal const val EVENT_TYPE_ORDER_DELIVERED = "orderDelivered"
internal const val EVENT_TYPE_ORDER_CANCELLED = "orderCancelled"
internal const val EVENT_TYPE_SEARCH_REQUEST = "searchRequest"

/** General **/
internal const val CURRENCY_CODE = "currencyCode"
internal const val PRICE = "price"
internal const val CATEGORY = "category"
internal const val EXTERNAL_ORDER_ID = "externalOrderId"
internal const val CART_ID = "cartId"
internal const val ATTRIBUTES = "attributes"

/** ProductViewed  **/
internal const val PRODUCT = "product"
internal const val PRODUCT_ID = "productId"
// boolean value in int format
internal const val IS_IN_STOCK = "isInStock"

/** ProductCategoryView **/
internal const val PRODUCT_CATEGORY_ID = "productCategoryId"

/** CardUpdated **/
internal const val PRODUCTS = "products"
internal const val QUANTITY = "quantity"
internal const val DISCOUNT = "discount"
internal const val PRODUCT_NAME = "name"
internal const val PRODUCT_CATEGORY = "category"

/** OrderCreated **/
internal const val EXTERNAL_CUSTOMER_ID = "externalCustomerId"
internal const val TOTAL_COST = "totalCost"
internal const val STATUS = "status"
internal const val DATE = "date"
internal const val EMAIL = "email"
internal const val PHONE = "phone"
internal const val FIRST_NAME = "firstName"
internal const val LAST_NAME = "lastName"
internal const val SHIPPING = "shipping"
internal const val ORDER_DISCOUNT = "discount"
internal const val TAXES = "taxes"
internal const val RESTORE_URL = "restoreUrl"
internal const val STATUS_DESCRIPTION = "statusDescription"
internal const val STORE_ID = "storeId"
internal const val SOURCE = "source"
internal const val DELIVERY_METHOD = "deliveryMethod"
internal const val PAYMENT_METHOD = "paymentMethod"
internal const val DELIVERY_ADDRESS = "deliveryAddress"
internal const val ITEMS = "items"

internal const val ORDER_ITEM_EXTERNAL_ITEM_ID = "externalItemId"
internal const val ORDER_ITEM_NAME = "name"
internal const val ORDER_ITEM_CATEGORY = "category"
internal const val ORDER_ITEM_QUANTITY = "quantity"
internal const val ORDER_ITEM_COST = "cost"
internal const val ORDER_ITEM_URL = "url"
internal const val ORDER_ITEM_IMAGE_URL = "imageUrl"
internal const val ORDER_ITEM_DESCRIPTION = "description"

/** SearchRequest **/
internal const val SEARCH = "search"
// boolean value in int format
internal const val IS_FOUND = "isFound"
