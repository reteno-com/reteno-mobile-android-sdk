package com.reteno.core.data.local.mappers

import com.reteno.core.data.local.model.event.EventDb
import com.reteno.core.data.local.model.event.ParameterDb
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.domain.model.ecom.Attributes
import com.reteno.core.domain.model.ecom.EcomEvent
import com.reteno.core.domain.model.ecom.ProductCategoryView
import com.reteno.core.domain.model.ecom.ProductInCart
import com.reteno.core.domain.model.ecom.ProductView
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.CART_ID
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.CATEGORY
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.CURRENCY_CODE
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.DATE
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.DELIVERY_ADDRESS
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.DELIVERY_METHOD
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.DISCOUNT
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.EMAIL
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.EVENT_TYPE_CART_UPDATED
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.EVENT_TYPE_ORDER_CANCELLED
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.EVENT_TYPE_ORDER_CREATED
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.EVENT_TYPE_ORDER_DELIVERED
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.EVENT_TYPE_ORDER_UPDATED
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.EVENT_TYPE_PRODUCT_ADDED_TO_WISHLIST
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.EVENT_TYPE_PRODUCT_CATEGORY_VIEWED
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.EVENT_TYPE_PRODUCT_VIEWED
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.EVENT_TYPE_SEARCH_REQUEST
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.EXTERNAL_CUSTOMER_ID
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.EXTERNAL_ORDER_ID
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.FIRST_NAME
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.IS_FOUND
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.IS_IN_STOCK
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.ITEMS
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.LAST_NAME
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.ORDER_DISCOUNT
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.PAYMENT_METHOD
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.PHONE
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.PRICE
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.PRODUCT
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.PRODUCTS
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.PRODUCT_CATEGORY
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.PRODUCT_CATEGORY_ID
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.PRODUCT_ID
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.PRODUCT_NAME
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.QUANTITY
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.RESTORE_URL
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.SEARCH
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.SHIPPING
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.SOURCE
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.STATUS
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.STATUS_DESCRIPTION
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.STORE_ID
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.TAXES
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.TOTAL_COST
import com.reteno.core.util.Logger
import com.reteno.core.util.Util.formatToRemote
import com.reteno.core.util.Util.formatToRemoteExplicitMillis
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

internal fun EcomEvent.toDb() = EventDb(
    eventTypeKey = getTypeKey(),
    occurred = occurred.formatToRemoteExplicitMillis(),
    params = convertToParams()
)

private fun EcomEvent.getTypeKey(): String {
    return when (this) {
        is EcomEvent.ProductViewed -> EVENT_TYPE_PRODUCT_VIEWED
        is EcomEvent.ProductCategoryViewed -> EVENT_TYPE_PRODUCT_CATEGORY_VIEWED
        is EcomEvent.ProductAddedToWishlist -> EVENT_TYPE_PRODUCT_ADDED_TO_WISHLIST
        is EcomEvent.CartUpdated -> EVENT_TYPE_CART_UPDATED
        is EcomEvent.OrderCreated -> EVENT_TYPE_ORDER_CREATED
        is EcomEvent.OrderUpdated -> EVENT_TYPE_ORDER_UPDATED
        is EcomEvent.OrderDelivered -> EVENT_TYPE_ORDER_DELIVERED
        is EcomEvent.OrderCancelled -> EVENT_TYPE_ORDER_CANCELLED
        is EcomEvent.SearchRequest -> EVENT_TYPE_SEARCH_REQUEST
    }
}

private fun EcomEvent.convertToParams(): List<ParameterDb> {
    return when (this) {
        is EcomEvent.ProductViewed -> formatToEventParams()
        is EcomEvent.ProductCategoryViewed -> formatToEventParams()
        is EcomEvent.ProductAddedToWishlist -> formatToEventParams()
        is EcomEvent.CartUpdated -> formatToEventParams()
        is EcomEvent.OrderCreated -> formatToEventParams()
        is EcomEvent.OrderUpdated -> formatToEventParams()
        is EcomEvent.OrderDelivered -> formatToEventParams()
        is EcomEvent.OrderCancelled -> formatToEventParams()
        is EcomEvent.SearchRequest -> formatToEventParams()
    }
}

private fun EcomEvent.ProductViewed.formatToEventParams(): List<ParameterDb> {
    val event = this

    fun ProductView.formatToValue(): String {
        val jsonObject = JSONObject().apply {
            put(PRODUCT_ID, productId)
            put(PRICE, price)
            put(IS_IN_STOCK, isInStock.toIntValue())
            attributes?.forEach(::putAttributes)
        }

        return jsonObject.toString()
    }

    return buildList {
        add(ParameterDb(PRODUCT, event.product.formatToValue()))

        event.currencyCode?.let {
            add(ParameterDb(CURRENCY_CODE, it))
        }
    }
}

private fun EcomEvent.ProductCategoryViewed.formatToEventParams(): List<ParameterDb> {
    val event = this

    fun ProductCategoryView.formatToValue(): String {
        val jsonObject = JSONObject().apply {
            put(PRODUCT_CATEGORY_ID, productCategoryId)
            attributes?.forEach(::putAttributes)
        }
        return jsonObject.toString()
    }

    return listOf(ParameterDb(CATEGORY, event.category.formatToValue()))
}

private fun EcomEvent.ProductAddedToWishlist.formatToEventParams(): List<ParameterDb> {
    val event = this

    fun ProductView.formatToValue(): String {
        val jsonObject = JSONObject().apply {
            put(PRODUCT_ID, productId)
            put(PRICE, price)
            put(IS_IN_STOCK, isInStock.toIntValue())
            attributes?.forEach(::putAttributes)
        }
        return jsonObject.toString()
    }

    return buildList {
        add(ParameterDb(PRODUCT, event.product.formatToValue()))
        event.currencyCode?.let { add(ParameterDb(CURRENCY_CODE, it)) }
    }
}

private fun EcomEvent.CartUpdated.formatToEventParams(): List<ParameterDb> {
    val event = this

    fun ProductInCart.formatToValue(): JSONObject {
        val jsonObject = JSONObject().apply {
            put(PRODUCT_ID, productId)
            put(QUANTITY, quantity)
            put(PRICE, price)
            name?.let { put(PRODUCT_NAME, it) }
            category?.let { put(PRODUCT_CATEGORY, it) }
            discount?.let { put(DISCOUNT, it) }
            attributes?.forEach(::putAttributes)
        }
        return jsonObject
    }

    return buildList {
        add(ParameterDb(CART_ID, event.cartId))
        val products: List< JSONObject> = event.products.map { it.formatToValue() }
        add(ParameterDb(PRODUCTS, JSONArray(products).toString()))
        event.currencyCode?.let { add(ParameterDb(CURRENCY_CODE, it)) }
    }
}

private fun EcomEvent.OrderCreated.formatToEventParams(): List<ParameterDb> {
    val event = this

    return buildList {
        add(ParameterDb(EXTERNAL_ORDER_ID, event.order.externalOrderId))
        event.order.externalCustomerId?.let { add(ParameterDb(EXTERNAL_CUSTOMER_ID, it)) }
        add(ParameterDb(TOTAL_COST, event.order.totalCost.toString()))
        add(ParameterDb(STATUS, event.order.status.name))
        add(ParameterDb(DATE, event.order.date.formatToRemote()))

        event.order.cartId?.let { add(ParameterDb(CART_ID, it)) }
        event.currencyCode?.let { add(ParameterDb(CURRENCY_CODE, it)) }
        event.order.email?.let { add(ParameterDb(EMAIL, it)) }
        event.order.phone?.let { add(ParameterDb(PHONE, it)) }
        event.order.firstName?.let { add(ParameterDb(FIRST_NAME, it)) }
        event.order.lastName?.let { add(ParameterDb(LAST_NAME, it)) }
        event.order.shipping?.let { add(ParameterDb(SHIPPING, it.toString())) }
        event.order.discount?.let { add(ParameterDb(ORDER_DISCOUNT, it.toString())) }
        event.order.taxes?.let { add(ParameterDb(TAXES, it.toString())) }
        event.order.restoreUrl?.let { add(ParameterDb(RESTORE_URL, it)) }
        event.order.statusDescription?.let { add(ParameterDb(STATUS_DESCRIPTION, it)) }
        event.order.storeId?.let { add(ParameterDb(STORE_ID, it)) }
        event.order.source?.let { add(ParameterDb(SOURCE, it)) }
        event.order.deliveryMethod?.let { add(ParameterDb(DELIVERY_METHOD, it)) }
        event.order.paymentMethod?.let { add(ParameterDb(PAYMENT_METHOD, it)) }
        event.order.deliveryAddress?.let { add(ParameterDb(DELIVERY_ADDRESS, it)) }
        event.order.items?.let { add(ParameterDb(ITEMS, it.toJson())) }
        event.order.attributes?.forEach {
            val key = it.first
            val escapedValue = orderAttributeEscapeJson(it.second)
            add(ParameterDb(key, escapedValue))
        }
    }
}

private fun orderAttributeEscapeJson(value: String): String {
    val jsonValue = try {
        val json = JSONObject(value)
        json.toString()
    } catch (jsonEx: JSONException) {
        /*@formatter:off*/ Logger.d("EcomEventMapper.kt", "EcomEvent.OrderCreated.formatToEventParams(): ", "failed to parse json in value as JSON Object")
        /*@formatter:on*/
        try {
            val json = JSONArray(value)
            json.toString()
        } catch (jsonEx: JSONException) {
            /*@formatter:off*/ Logger.d("EcomEventMapper.kt", "EcomEvent.OrderCreated.formatToEventParams(): ", "failed to parse json in value as JSON Array")
            /*@formatter:on*/
            value
        }
    }
    return jsonValue
}

private fun EcomEvent.OrderUpdated.formatToEventParams(): List<ParameterDb> {
    val event = this

    return buildList {
        add(ParameterDb(EXTERNAL_ORDER_ID, event.order.externalOrderId))
        event.order.externalCustomerId?.let { add(ParameterDb(EXTERNAL_CUSTOMER_ID, it)) }
        add(ParameterDb(TOTAL_COST, event.order.totalCost.toString()))
        add(ParameterDb(STATUS, event.order.status.name))
        add(ParameterDb(DATE, event.order.date.formatToRemote()))

        event.order.cartId?.let { add(ParameterDb(CART_ID, it)) }
        event.currencyCode?.let { add(ParameterDb(CURRENCY_CODE, it)) }
        event.order.email?.let { add(ParameterDb(EMAIL, it)) }
        event.order.phone?.let { add(ParameterDb(PHONE, it)) }
        event.order.firstName?.let { add(ParameterDb(FIRST_NAME, it)) }
        event.order.lastName?.let { add(ParameterDb(LAST_NAME, it)) }
        event.order.shipping?.let { add(ParameterDb(SHIPPING, it.toString())) }
        event.order.discount?.let { add(ParameterDb(ORDER_DISCOUNT, it.toString())) }
        event.order.taxes?.let { add(ParameterDb(TAXES, it.toString())) }
        event.order.restoreUrl?.let { add(ParameterDb(RESTORE_URL, it)) }
        event.order.statusDescription?.let { add(ParameterDb(STATUS_DESCRIPTION, it)) }
        event.order.storeId?.let { add(ParameterDb(STORE_ID, it)) }
        event.order.source?.let { add(ParameterDb(SOURCE, it)) }
        event.order.deliveryMethod?.let { add(ParameterDb(DELIVERY_METHOD, it)) }
        event.order.paymentMethod?.let { add(ParameterDb(PAYMENT_METHOD, it)) }
        event.order.deliveryAddress?.let { add(ParameterDb(DELIVERY_ADDRESS, it)) }
        event.order.items?.let { add(ParameterDb(ITEMS, it.toJson())) }

        event.order.attributes?.forEach {
            val key = it.first
            val escapedValue = orderAttributeEscapeJson(it.second)
            add(ParameterDb(key, escapedValue))
        }
    }
}

private fun EcomEvent.OrderDelivered.formatToEventParams(): List<ParameterDb> {
    return listOf(
        ParameterDb(EXTERNAL_ORDER_ID, externalOrderId)
    )
}

private fun EcomEvent.OrderCancelled.formatToEventParams(): List<ParameterDb> {
    return listOf(
        ParameterDb(EXTERNAL_ORDER_ID, externalOrderId)
    )
}

private fun EcomEvent.SearchRequest.formatToEventParams(): List<ParameterDb> {
    return listOf(
        ParameterDb(SEARCH, search),
        ParameterDb(IS_FOUND, isFound.toIntValue().toString())
    )
}

private fun JSONObject.putAttributes(attrs: Attributes) {
    val jsonArray = JSONArray()
    for (element in attrs.value) {
        jsonArray.put(element)
    }
    put(attrs.name, jsonArray)
}
