package com.reteno.core.data.local.mappers

import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.local.model.event.EventDb
import com.reteno.core.data.local.model.event.ParameterDb
import com.reteno.core.domain.model.ecom.Attributes
import com.reteno.core.domain.model.ecom.EcomEvent
import com.reteno.core.domain.model.ecom.Order
import com.reteno.core.domain.model.ecom.OrderItem
import com.reteno.core.domain.model.ecom.OrderStatus
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
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.ITEMS
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.LAST_NAME
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.ORDER_DISCOUNT
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.ORDER_ITEM_CATEGORY
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.ORDER_ITEM_COST
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.ORDER_ITEM_DESCRIPTION
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.ORDER_ITEM_EXTERNAL_ITEM_ID
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.ORDER_ITEM_IMAGE_URL
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.ORDER_ITEM_NAME
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.ORDER_ITEM_QUANTITY
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.ORDER_ITEM_URL
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.PAYMENT_METHOD
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.PHONE
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
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.IS_IN_STOCK as KEY_IS_IN_STOCK
import com.reteno.core.domain.model.ecom.RemoteConstants.EcomEvent.PRICE as KEY_PRICE


class EcomEventMapperKtTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    private object General {
        const val CURRENCY_CODE = "UAH"
        const val CURRENCY_CODE_EXPECTED = "UAH"
        val OCCURRED: ZonedDateTime = ZonedDateTime.of(
            1996, 8, 15,
            11, 11, 11, 0,
            ZoneId.of("UTC+3")
        )
        const val OCCURRED_EXPECTED = "1996-08-15T08:11:11.000Z"
        const val OCCURRED_EXPECTED_ORDER = "1996-08-15T08:11:11Z"

        const val ATTRIBUTE_1_KEY = "key1"
        const val ATTRIBUTE_2_KEY = "key2"
        val ATTRIBUTE_1_VALUE = listOf("value11", "value12", "value13", "value14")
        const val ATTRIBUTE_1_VALUE_EXPECTED = "[\"value11\",\"value12\",\"value13\",\"value14\"]"
        val ATTRIBUTE_2_VALUE = listOf("value21", "value22", "value23", "value24")
        const val ATTRIBUTE_2_VALUE_EXPECTED = "[\"value21\",\"value22\",\"value23\",\"value24\"]"
        val ATTRIBUTE_1 = Attributes(ATTRIBUTE_1_KEY, ATTRIBUTE_1_VALUE)
        val ATTRIBUTE_2 = Attributes(ATTRIBUTE_2_KEY, ATTRIBUTE_2_VALUE)
        val ATTRIBUTES_EXPECTED = listOf(ATTRIBUTE_1, ATTRIBUTE_2)

        const val ATTRIBUTE_1_EXPECTED: String = "\"$ATTRIBUTE_1_KEY\":$ATTRIBUTE_1_VALUE_EXPECTED"
        const val ATTRIBUTE_2_EXPECTED: String = "\"$ATTRIBUTE_2_KEY\":$ATTRIBUTE_2_VALUE_EXPECTED"
    }

    private object Product {
        const val ID = "product_id_here"
        const val ID_EXPECTED = "\"product_id_here\""
        const val PRICE = 123.3
        const val PRICE_EXPECTED = "123.3"
        const val IS_IN_STOCK = true
        const val IS_IN_STOCK_EXPECTED = "1"
        val PARAMS_EXPECTED = listOf<String>(
            "\"${PRODUCT_ID}\":$ID_EXPECTED",
            "\"${KEY_PRICE}\":$PRICE_EXPECTED",
            "\"${KEY_IS_IN_STOCK}\":$IS_IN_STOCK_EXPECTED",
        )

    }

    private object ProductCategoryViewed {
        const val ID = "product_category_id_here"
        const val ID_EXPECTED = "\"product_category_id_here\""
        val PARAMS_EXPECTED = listOf<String>(
            "\"${PRODUCT_CATEGORY_ID}\":$ID_EXPECTED"
        )
    }

    private object Cart {
        const val ID = "cart_id"
        const val ID_EXPECTED = "cart_id"
        const val PRODUCT_1_ID = "product_id_1"
        const val PRODUCT_1_ID_EXPECTED = "\"product_id_1\""
        const val PRODUCT_1_QUANTITY = 15
        const val PRODUCT_1_QUANTITY_EXPECTED = "15"
        const val PRODUCT_1_PRICE = 1325.1
        const val PRODUCT_1_PRICE_EXPECTED = "1325.1"
        const val PRODUCT_1_NAME = "product_1_name"
        const val PRODUCT_1_NAME_EXPECTED = "\"product_1_name\""
        const val PRODUCT_1_CATEGORY = "product_1_category"
        const val PRODUCT_1_CATEGORY_EXPECTED = "\"product_1_category\""
        const val PRODUCT_1_DISCOUNT = 0.12
        const val PRODUCT_1_DISCOUNT_EXPECTED = "0.12"

        const val PRODUCT_2_ID = "product_id_2"
        const val PRODUCT_2_ID_EXPECTED = "\"product_id_2\""
        const val PRODUCT_2_QUANTITY = 32
        const val PRODUCT_2_QUANTITY_EXPECTED = "32"
        const val PRODUCT_2_PRICE = 15.5
        const val PRODUCT_2_PRICE_EXPECTED = "15.5"
        const val PRODUCT_2_NAME = "product_2_name"
        const val PRODUCT_2_NAME_EXPECTED = "\"product_2_name\""
        const val PRODUCT_2_CATEGORY = "product_2_category"
        const val PRODUCT_2_CATEGORY_EXPECTED = "\"product_2_category\""
        const val PRODUCT_2_DISCOUNT = 0.08
        const val PRODUCT_2_DISCOUNT_EXPECTED = "0.08"

        val PRODUCT_1_EXPECTED = listOf<String>(
            "\"${PRODUCT_ID}\":$PRODUCT_1_ID_EXPECTED",
            "\"${QUANTITY}\":$PRODUCT_1_QUANTITY_EXPECTED",
            "\"${KEY_PRICE}\":$PRODUCT_1_PRICE_EXPECTED",
            "\"${PRODUCT_NAME}\":$PRODUCT_1_NAME_EXPECTED",
            "\"${PRODUCT_CATEGORY}\":$PRODUCT_1_CATEGORY_EXPECTED",
            "\"${DISCOUNT}\":$PRODUCT_1_DISCOUNT_EXPECTED",
            General.ATTRIBUTE_1_EXPECTED,
            General.ATTRIBUTE_2_EXPECTED
        )
        val PRODUCT_2_EXPECTED = listOf<String>(
            "\"${PRODUCT_ID}\":$PRODUCT_2_ID_EXPECTED",
            "\"${QUANTITY}\":$PRODUCT_2_QUANTITY_EXPECTED",
            "\"${KEY_PRICE}\":$PRODUCT_2_PRICE_EXPECTED",
            "\"${PRODUCT_NAME}\":$PRODUCT_2_NAME_EXPECTED",
            "\"${PRODUCT_CATEGORY}\":$PRODUCT_2_CATEGORY_EXPECTED",
            "\"${DISCOUNT}\":$PRODUCT_2_DISCOUNT_EXPECTED",
            General.ATTRIBUTE_1_EXPECTED,
            General.ATTRIBUTE_2_EXPECTED
        )
    }

    private object Order {
        const val ITEM_1_EXTERNAL_ITEM_ID = "external_item_id_1"
        const val ITEM_1_EXTERNAL_ITEM_ID_EXPECTED = "\"external_item_id_1\""
        const val ITEM_1_NAME = "name1"
        const val ITEM_1_NAME_EXPECTED = "\"name1\""
        const val ITEM_1_CATEGORY = "category_1"
        const val ITEM_1_CATEGORY_EXPECTED = "\"category_1\""
        const val ITEM_1_QUANTITY = 131.6
        const val ITEM_1_QUANTITY_EXPECTED = 131.6
        const val ITEM_1_COST = 101.3
        const val ITEM_1_COST_EXPECTED = 101.3
        const val ITEM_1_URL = "https://something.org"
        const val ITEM_1_URL_EXPECTED = "\"https://something.org\""
        const val ITEM_1_IMAGE_URL = "https://image.something.org"
        const val ITEM_1_IMAGE_URL_EXPECTED = "\"https://image.something.org\""
        const val ITEM_1_DESCRIPTION = "description"
        const val ITEM_1_DESCRIPTION_EXPECTED = "\"description\""
        val ITEM_1_EXPECTED = listOf<String>(
            "\"${ORDER_ITEM_EXTERNAL_ITEM_ID}\":${ITEM_1_EXTERNAL_ITEM_ID_EXPECTED}",
            "\"${ORDER_ITEM_NAME}\":${ITEM_1_NAME_EXPECTED}",
            "\"${ORDER_ITEM_CATEGORY}\":${ITEM_1_CATEGORY_EXPECTED}",
            "\"${ORDER_ITEM_QUANTITY}\":${ITEM_1_QUANTITY_EXPECTED}",
            "\"${ORDER_ITEM_COST}\":${ITEM_1_COST_EXPECTED}",
            "\"${ORDER_ITEM_URL}\":${ITEM_1_URL_EXPECTED}",
            "\"${ORDER_ITEM_IMAGE_URL}\":${ITEM_1_IMAGE_URL_EXPECTED}",
            "\"${ORDER_ITEM_DESCRIPTION}\":${ITEM_1_DESCRIPTION_EXPECTED}"
        )
        private const val ATTRIBUTE_1_KEY = "key1"
        private const val ATTRIBUTE_2_KEY = "key2"
        private const val ATTRIBUTE_1_VALUE = "{param1:value11,param2:12,param3:true,param4:12.3}"
        const val ATTRIBUTE_1_VALUE_EXPECTED = "{\"param1\":\"value11\",\"param2\":12,\"param3\":true,\"param4\":12.3}"
        private const val ATTRIBUTE_2_VALUE = "[value21,value22,value23,value24]"
        const val ATTRIBUTE_2_VALUE_EXPECTED = "[\"value21\",\"value22\",\"value23\",\"value24\"]"
        val ATTRIBUTE_1 = android.util.Pair(ATTRIBUTE_1_KEY, ATTRIBUTE_1_VALUE)
        val ATTRIBUTE_2 = android.util.Pair(ATTRIBUTE_2_KEY, ATTRIBUTE_2_VALUE)

        const val ITEM_2_EXTERNAL_ITEM_ID = "external_item_id_2"
        const val ITEM_2_EXTERNAL_ITEM_ID_EXPECTED = "\"external_item_id_2\""
        const val ITEM_2_NAME = "name2"
        const val ITEM_2_NAME_EXPECTED = "\"name2\""
        const val ITEM_2_CATEGORY = "category_2"
        const val ITEM_2_CATEGORY_EXPECTED = "\"category_2\""
        const val ITEM_2_QUANTITY = 1.6
        const val ITEM_2_QUANTITY_EXPECTED = 1.6
        const val ITEM_2_COST = 1.3
        const val ITEM_2_COST_EXPECTED = 1.3
        const val ITEM_2_URL = "https://something222.org"
        const val ITEM_2_URL_EXPECTED = "\"https://something222.org\""
        const val ITEM_2_IMAGE_URL = "https://image222.something.org"
        const val ITEM_2_IMAGE_URL_EXPECTED = "\"https://image222.something.org\""
        const val ITEM_2_DESCRIPTION = "description_222"
        const val ITEM_2_DESCRIPTION_EXPECTED = "\"description_222\""
        val ITEM_2_EXPECTED = listOf<String>(
            "\"${ORDER_ITEM_EXTERNAL_ITEM_ID}\":${ITEM_2_EXTERNAL_ITEM_ID_EXPECTED}",
            "\"${ORDER_ITEM_NAME}\":${ITEM_2_NAME_EXPECTED}",
            "\"${ORDER_ITEM_CATEGORY}\":${ITEM_2_CATEGORY_EXPECTED}",
            "\"${ORDER_ITEM_QUANTITY}\":${ITEM_2_QUANTITY_EXPECTED}",
            "\"${ORDER_ITEM_COST}\":${ITEM_2_COST_EXPECTED}",
            "\"${ORDER_ITEM_URL}\":${ITEM_2_URL_EXPECTED}",
            "\"${ORDER_ITEM_IMAGE_URL}\":${ITEM_2_IMAGE_URL_EXPECTED}",
            "\"${ORDER_ITEM_DESCRIPTION}\":${ITEM_2_DESCRIPTION_EXPECTED}"
        )

        const val EXTERNAL_ORDER_ID = "externalOrderId"
        const val EXTERNAL_ORDER_ID_EXPECTED = "externalOrderId"
        const val EXTERNAL_CUSTOMER_ID = "externalCustomerId"
        const val EXTERNAL_CUSTOMER_ID_EXPECTED = "externalCustomerId"
        const val TOTAL_COST = 123.0
        const val TOTAL_COST_EXPECTED = "123.0"
        val STATUS = OrderStatus.INITIALIZED
        val STATUS_EXPECTED = OrderStatus.INITIALIZED.toString()
        const val CART_ID = "cartId"
        const val CART_ID_EXPECTED = "cartId"
        const val EMAIL = "email"
        const val EMAIL_EXPECTED = "email"
        const val PHONE = "phone"
        const val PHONE_EXPECTED = "phone"
        const val FIRST_NAME = "firstName"
        const val FIRST_NAME_EXPECTED = "firstName"
        const val LAST_NAME = "lastName"
        const val LAST_NAME_EXPECTED = "lastName"
        const val SHIPPING = 33.1
        const val SHIPPING_EXPECTED = "33.1"
        const val DISCOUNT = 0.2
        const val DISCOUNT_EXPECTED = "0.2"
        const val TAXES = 0.15
        const val TAXES_EXPECTED = "0.15"
        const val RESTORE_URL = "restoreUrl"
        const val RESTORE_URL_EXPECTED = "restoreUrl"
        const val STATUS_DESCRIPTION = "statusDescription"
        const val STATUS_DESCRIPTION_EXPECTED = "statusDescription"
        const val STORE_ID = "storeId"
        const val STORE_ID_EXPECTED = "storeId"
        const val SOURCE = "source"
        const val SOURCE_EXPECTED = "source"
        const val DELIVERY_METHOD = "deliveryMethod"
        const val DELIVERY_METHOD_EXPECTED = "deliveryMethod"
        const val PAYMENT_METHOD = "paymentMethod"
        const val PAYMENT_METHOD_EXPECTED = "paymentMethod"
        const val DELIVERY_ADDRESS = "deliveryAddress"
        const val DELIVERY_ADDRESS_EXPECTED = "deliveryAddress"
    }

    private object Search {
        const val CRITERIA = "searchCriteria"
        const val CRITERIA_EXPECTED = "searchCriteria"
        const val IS_FOUND = true
        const val IS_FOUND_EXPECTED = "1"
    }
    // endregion constants -------------------------------------------------------------------------

    @Test
    fun givenProductViewed_whenToDb_thenDbModelReturned() {
        // Given
        val productView =
            ProductView(Product.ID, Product.PRICE, Product.IS_IN_STOCK, General.ATTRIBUTES_EXPECTED)
        val input = EcomEvent.ProductViewed(productView, General.CURRENCY_CODE, General.OCCURRED)
        val expected = EventDb(
            eventTypeKey = EVENT_TYPE_PRODUCT_VIEWED,
            occurred = General.OCCURRED_EXPECTED,
            params = getExpectedProductParams()
        )

        // When
        val result = input.toDb()

        // Then
        assertEquals(expected, result)
    }

    @Test
    fun givenProductCategoryViewed_whenToDb_thenDbModelReturned() {
        // Given
        val productCategoryView =
            ProductCategoryView(ProductCategoryViewed.ID, General.ATTRIBUTES_EXPECTED)
        val input = EcomEvent.ProductCategoryViewed(productCategoryView, General.OCCURRED)
        val expected = EventDb(
            eventTypeKey = EVENT_TYPE_PRODUCT_CATEGORY_VIEWED,
            occurred = General.OCCURRED_EXPECTED,
            params = getExpectedProductCategoryViewedParams()
        )

        // When
        val result = input.toDb()

        // Then
        assertEquals(expected, result)
    }

    @Test
    fun givenProductAddedToWishlist_whenToDb_thenDbModelReturned() {
        // Given
        val productAddedToWishlist =
            ProductView(Product.ID, Product.PRICE, Product.IS_IN_STOCK, General.ATTRIBUTES_EXPECTED)
        val input = EcomEvent.ProductAddedToWishlist(
            productAddedToWishlist,
            General.CURRENCY_CODE,
            General.OCCURRED
        )
        val expected = EventDb(
            eventTypeKey = EVENT_TYPE_PRODUCT_ADDED_TO_WISHLIST,
            occurred = General.OCCURRED_EXPECTED,
            params = getExpectedProductParams()
        )

        // When
        val result = input.toDb()

        // Then
        assertEquals(expected, result)
    }

    @Test
    fun givenProductInCart_whenToDb_thenDbModelReturned() {
        // Given
        val productInCart1 = ProductInCart(
            productId = Cart.PRODUCT_1_ID,
            quantity = Cart.PRODUCT_1_QUANTITY,
            price = Cart.PRODUCT_1_PRICE,
            discount = Cart.PRODUCT_1_DISCOUNT,
            name = Cart.PRODUCT_1_NAME,
            category = Cart.PRODUCT_1_CATEGORY,
            attributes = General.ATTRIBUTES_EXPECTED
        )
        val productInCart2 = ProductInCart(
            productId = Cart.PRODUCT_2_ID,
            quantity = Cart.PRODUCT_2_QUANTITY,
            price = Cart.PRODUCT_2_PRICE,
            discount = Cart.PRODUCT_2_DISCOUNT,
            name = Cart.PRODUCT_2_NAME,
            category = Cart.PRODUCT_2_CATEGORY,
            attributes = General.ATTRIBUTES_EXPECTED
        )
        val input = EcomEvent.CartUpdated(
            cartId = Cart.ID,
            products = listOf(productInCart1, productInCart2),
            currencyCode = General.CURRENCY_CODE,
            occurred = General.OCCURRED
        )
        val expected = EventDb(
            eventTypeKey = EVENT_TYPE_CART_UPDATED,
            occurred = General.OCCURRED_EXPECTED,
            params = getExpectedProductInCartParams()
        )

        // When
        val result = input.toDb()

        // Then
        assertEquals(expected, result)
    }

    @Test
    fun givenOrderCreated_whenToDb_thenDbModelReturned() {
        // Given
        val input = EcomEvent.OrderCreated(getOrder(), General.CURRENCY_CODE, General.OCCURRED)
        val expected = EventDb(
            eventTypeKey = EVENT_TYPE_ORDER_CREATED,
            occurred = General.OCCURRED_EXPECTED,
            params = getExpectedOrderParams()
        )

        // When
        val result = input.toDb()

        // Then
        assertEquals(expected, result)
    }

    @Test
    fun givenOrderUpdated_whenToDb_thenDbModelReturned() {
        // Given
        val input = EcomEvent.OrderUpdated(getOrder(), General.CURRENCY_CODE, General.OCCURRED)
        val expected = EventDb(
            eventTypeKey = EVENT_TYPE_ORDER_UPDATED,
            occurred = General.OCCURRED_EXPECTED,
            params = getExpectedOrderParams()
        )

        // When
        val result = input.toDb()

        // Then
        assertEquals(expected, result)
    }

    @Test
    fun givenOrderDelivered_whenToDb_thenDbModelReturned() {
        // Given
        val input = EcomEvent.OrderDelivered(Order.EXTERNAL_ORDER_ID, General.OCCURRED)
        val expected = EventDb(
            eventTypeKey = EVENT_TYPE_ORDER_DELIVERED,
            occurred = General.OCCURRED_EXPECTED,
            params = listOf(
                ParameterDb(EXTERNAL_ORDER_ID, Order.EXTERNAL_ORDER_ID_EXPECTED)
            )
        )

        // When
        val result = input.toDb()

        // Then
        assertEquals(expected, result)
    }

    @Test
    fun givenOrderCancelled_whenToDb_thenDbModelReturned() {
        // Given
        val input = EcomEvent.OrderCancelled(Order.EXTERNAL_ORDER_ID, General.OCCURRED)
        val expected = EventDb(
            eventTypeKey = EVENT_TYPE_ORDER_CANCELLED,
            occurred = General.OCCURRED_EXPECTED,
            params = listOf(
                ParameterDb(EXTERNAL_ORDER_ID, Order.EXTERNAL_ORDER_ID_EXPECTED)
            )
        )

        // When
        val result = input.toDb()

        // Then
        assertEquals(expected, result)
    }

    @Test
    fun givenSearchRequest_whenToDb_thenDbModelReturned() {
        // Given
        val input = EcomEvent.SearchRequest(Search.CRITERIA, Search.IS_FOUND, General.OCCURRED)
        val expected = EventDb(
            eventTypeKey = EVENT_TYPE_SEARCH_REQUEST,
            occurred = General.OCCURRED_EXPECTED,
            params = listOf(
                ParameterDb(SEARCH, Search.CRITERIA_EXPECTED),
                ParameterDb(IS_FOUND, Search.IS_FOUND_EXPECTED)
            )
        )

        // When
        val result = input.toDb()

        // Then
        assertEquals(expected, result)
    }

    // region helper methods -----------------------------------------------------------------------
    private fun getExpectedProductParams(): List<ParameterDb> {
        val productParamsList = mutableListOf<String>()
            .apply {
                addAll(Product.PARAMS_EXPECTED)
                add(General.ATTRIBUTE_1_EXPECTED)
                add(General.ATTRIBUTE_2_EXPECTED)
            }
        val productParams =
            productParamsList.joinToString(separator = ",", prefix = "{", postfix = "}")
        return listOf<ParameterDb>(
            ParameterDb(PRODUCT, productParams),
            ParameterDb(CURRENCY_CODE, General.CURRENCY_CODE_EXPECTED)
        )
    }

    private fun getExpectedProductCategoryViewedParams(): List<ParameterDb> {
        val productParamsList = mutableListOf<String>()
            .apply {
                addAll(ProductCategoryViewed.PARAMS_EXPECTED)
                add(General.ATTRIBUTE_1_EXPECTED)
                add(General.ATTRIBUTE_2_EXPECTED)
            }

        val productParams =
            productParamsList.joinToString(separator = ",", prefix = "{", postfix = "}")
        return listOf(ParameterDb(CATEGORY, productParams))
    }

    private fun getExpectedProductInCartParams(): List<ParameterDb> {
        val product1Params =
            Cart.PRODUCT_1_EXPECTED.joinToString(separator = ",", prefix = "{", postfix = "}")
        val product2Params =
            Cart.PRODUCT_2_EXPECTED.joinToString(separator = ",", prefix = "{", postfix = "}")
        val productParams = listOf(product1Params, product2Params).joinToString(
            separator = ",",
            prefix = "[",
            postfix = "]"
        )
        return listOf(
            ParameterDb(CART_ID, Cart.ID_EXPECTED),
            ParameterDb(PRODUCTS, productParams),
            ParameterDb(CURRENCY_CODE, General.CURRENCY_CODE_EXPECTED)
        )
    }

    private fun getOrder(): com.reteno.core.domain.model.ecom.Order {
        val orderItem1 = OrderItem(
            externalItemId = Order.ITEM_1_EXTERNAL_ITEM_ID,
            name = Order.ITEM_1_NAME,
            category = Order.ITEM_1_CATEGORY,
            quantity = Order.ITEM_1_QUANTITY,
            cost = Order.ITEM_1_COST,
            url = Order.ITEM_1_URL,
            imageUrl = Order.ITEM_1_IMAGE_URL,
            description = Order.ITEM_1_DESCRIPTION
        )
        val orderItem2 = OrderItem(
            externalItemId = Order.ITEM_2_EXTERNAL_ITEM_ID,
            name = Order.ITEM_2_NAME,
            category = Order.ITEM_2_CATEGORY,
            quantity = Order.ITEM_2_QUANTITY,
            cost = Order.ITEM_2_COST,
            url = Order.ITEM_2_URL,
            imageUrl = Order.ITEM_2_IMAGE_URL,
            description = Order.ITEM_2_DESCRIPTION
        )
        return Order(
            externalOrderId = Order.EXTERNAL_ORDER_ID,
            externalCustomerId = Order.EXTERNAL_CUSTOMER_ID,
            totalCost = Order.TOTAL_COST,
            status = Order.STATUS,
            date = General.OCCURRED,
            cartId = Order.CART_ID,
            email = Order.EMAIL,
            phone = Order.PHONE,
            firstName = Order.FIRST_NAME,
            lastName = Order.LAST_NAME,
            shipping = Order.SHIPPING,
            discount = Order.DISCOUNT,
            taxes = Order.TAXES,
            restoreUrl = Order.RESTORE_URL,
            statusDescription = Order.STATUS_DESCRIPTION,
            storeId = Order.STORE_ID,
            source = Order.SOURCE,
            deliveryMethod = Order.DELIVERY_METHOD,
            paymentMethod = Order.PAYMENT_METHOD,
            deliveryAddress = Order.DELIVERY_ADDRESS,
            items = listOf(orderItem1, orderItem2),
            attributes = listOf(Order.ATTRIBUTE_1, Order.ATTRIBUTE_2),
        )
    }

    private fun getExpectedOrderParams(): List<ParameterDb> {
        val item1Params =
            Order.ITEM_1_EXPECTED.joinToString(separator = ",", prefix = "{", postfix = "}")
        val item2Params =
            Order.ITEM_2_EXPECTED.joinToString(separator = ",", prefix = "{", postfix = "}")
        val itemsParams = listOf(item1Params, item2Params).joinToString(
            separator = ",",
            prefix = "[",
            postfix = "]"
        )

        return listOf(
            ParameterDb(EXTERNAL_ORDER_ID, Order.EXTERNAL_ORDER_ID_EXPECTED),
            ParameterDb(EXTERNAL_CUSTOMER_ID, Order.EXTERNAL_CUSTOMER_ID_EXPECTED),
            ParameterDb(TOTAL_COST, Order.TOTAL_COST_EXPECTED),
            ParameterDb(STATUS, Order.STATUS_EXPECTED),
            ParameterDb(DATE, General.OCCURRED_EXPECTED_ORDER),
            ParameterDb(CART_ID, Order.CART_ID_EXPECTED),
            ParameterDb(CURRENCY_CODE, General.CURRENCY_CODE_EXPECTED),
            ParameterDb(EMAIL, Order.EMAIL_EXPECTED),
            ParameterDb(PHONE, Order.PHONE_EXPECTED),
            ParameterDb(FIRST_NAME, Order.FIRST_NAME_EXPECTED),
            ParameterDb(LAST_NAME, Order.LAST_NAME_EXPECTED),
            ParameterDb(SHIPPING, Order.SHIPPING_EXPECTED),
            ParameterDb(ORDER_DISCOUNT, Order.DISCOUNT_EXPECTED),
            ParameterDb(TAXES, Order.TAXES_EXPECTED),
            ParameterDb(RESTORE_URL, Order.RESTORE_URL_EXPECTED),
            ParameterDb(STATUS_DESCRIPTION, Order.STATUS_DESCRIPTION_EXPECTED),
            ParameterDb(STORE_ID, Order.STORE_ID_EXPECTED),
            ParameterDb(SOURCE, Order.SOURCE_EXPECTED),
            ParameterDb(DELIVERY_METHOD, Order.DELIVERY_METHOD_EXPECTED),
            ParameterDb(PAYMENT_METHOD, Order.PAYMENT_METHOD_EXPECTED),
            ParameterDb(DELIVERY_ADDRESS, Order.DELIVERY_ADDRESS_EXPECTED),
            ParameterDb(ITEMS, itemsParams),
            ParameterDb(Order.ATTRIBUTE_1.first, Order.ATTRIBUTE_1_VALUE_EXPECTED),
            ParameterDb(Order.ATTRIBUTE_2.first, Order.ATTRIBUTE_2_VALUE_EXPECTED)
        )
    }
    // endregion helper methods --------------------------------------------------------------------
}