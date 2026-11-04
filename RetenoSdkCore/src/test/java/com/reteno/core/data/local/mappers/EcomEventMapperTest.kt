package com.reteno.core.data.local.mappers

import android.util.Pair
import com.google.gson.JsonParser
import com.reteno.core.data.local.model.event.ParameterDb
import com.reteno.core.domain.model.ecom.Attributes
import com.reteno.core.domain.model.ecom.EcomEvent
import com.reteno.core.domain.model.ecom.Order
import com.reteno.core.domain.model.ecom.OrderItem
import com.reteno.core.domain.model.ecom.OrderStatus
import com.reteno.core.domain.model.ecom.ProductCategoryView
import com.reteno.core.domain.model.ecom.ProductInCart
import com.reteno.core.domain.model.ecom.ProductView
import com.reteno.core.domain.model.ecom.asRegularEvent
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.ZoneOffset
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class EcomEventMapperTest {

    @Test
    fun `should store every ecom event under its event type with occurrence in utc millis`() = runUnitTest {
        given()
        val events = listOf(
            EcomEvent.ProductViewed(PRODUCT, occurred = OCCURRED),
            EcomEvent.ProductCategoryViewed(ProductCategoryView("shoes"), OCCURRED),
            EcomEvent.ProductAddedToWishlist(PRODUCT, occurred = OCCURRED),
            EcomEvent.CartUpdated("cart-1", listOf(ProductInCart("sku-1", 1, 10.0)), occurred = OCCURRED),
            EcomEvent.OrderCreated(minimalOrder(), occurred = OCCURRED),
            EcomEvent.OrderUpdated(minimalOrder(), occurred = OCCURRED),
            EcomEvent.OrderDelivered("order-1", OCCURRED),
            EcomEvent.OrderCancelled("order-1", OCCURRED),
            EcomEvent.SearchRequest("boots", occurred = OCCURRED)
        )

        whenn()
        val stored = events.map { it.toDb() }

        then()
        assertEquals(
            listOf(
                "productViewed", "productCategoryViewed", "productAddedToWishlist", "cartUpdated", "orderCreated",
                "orderUpdated", "orderDelivered", "orderCancelled", "searchRequest"
            ),
            stored.map { it.eventTypeKey }
        )
        assertEquals(setOf("2026-01-02T01:04:05.678Z"), stored.map { it.occurred }.toSet())
    }

    @Test
    fun `should store viewed product with stock as number attributes and currency`() = runUnitTest {
        given()
        val event = EcomEvent.ProductViewed(
            PRODUCT.copy(attributes = listOf(Attributes("size", listOf("42", "43")))),
            "USD",
            OCCURRED
        )

        whenn()
        val params = event.toDb().params.orEmpty()

        then()
        assertEquals(listOf("product", "currencyCode"), params.map { it.name })
        assertJson("""{"productId":"sku-1","price":10.5,"isInStock":1,"size":["42","43"]}""", params[0])
        assertEquals("USD", params[1].value)
    }

    @Test
    fun `should store viewed product out of stock without attributes and currency`() = runUnitTest {
        given()
        val event = EcomEvent.ProductViewed(PRODUCT.copy(isInStock = false), occurred = OCCURRED)

        whenn()
        val params = event.toDb().params.orEmpty()

        then()
        assertEquals(listOf("product"), params.map { it.name })
        assertJson("""{"productId":"sku-1","price":10.5,"isInStock":0}""", params.single())
    }

    @Test
    fun `should store product added to wishlist like a viewed product`() = runUnitTest {
        given()
        val events = listOf(
            EcomEvent.ProductAddedToWishlist(
                PRODUCT.copy(isInStock = false, attributes = listOf(Attributes("color", listOf("red")))),
                "EUR",
                OCCURRED
            ),
            EcomEvent.ProductAddedToWishlist(PRODUCT, occurred = OCCURRED)
        )

        whenn()
        val params = events.map { it.toDb().params.orEmpty() }

        then()
        assertEquals(
            listOf(listOf("product", "currencyCode"), listOf("product")),
            params.map { list -> list.map { it.name } }
        )
        assertJson("""{"productId":"sku-1","price":10.5,"isInStock":0,"color":["red"]}""", params[0][0])
        assertEquals("EUR", params[0][1].value)
        assertJson("""{"productId":"sku-1","price":10.5,"isInStock":1}""", params[1][0])
    }

    @Test
    fun `should store viewed category with and without attributes`() = runUnitTest {
        given()
        val events = listOf(
            EcomEvent.ProductCategoryViewed(
                ProductCategoryView("shoes", listOf(Attributes("brand", listOf("acme")))),
                OCCURRED
            ),
            EcomEvent.ProductCategoryViewed(ProductCategoryView("shoes"), OCCURRED)
        )

        whenn()
        val params = events.map { it.toDb().params.orEmpty().single() }

        then()
        assertEquals(listOf("category", "category"), params.map { it.name })
        assertJson("""{"productCategoryId":"shoes","brand":["acme"]}""", params[0])
        assertJson("""{"productCategoryId":"shoes"}""", params[1])
    }

    @Test
    fun `should store cart with every product detail and currency`() = runUnitTest {
        given()
        val products = listOf(
            ProductInCart(
                "sku-1",
                2,
                10.0,
                discount = 1.5,
                name = "Boot",
                category = "shoes",
                attributes = listOf(Attributes("size", listOf("42")))
            ),
            ProductInCart("sku-2", 1, 5.0)
        )

        whenn()
        val params = EcomEvent.CartUpdated("cart-1", products, "USD", OCCURRED).toDb().params.orEmpty()

        then()
        assertEquals(listOf("cartId", "products", "currencyCode"), params.map { it.name })
        assertEquals(listOf("cart-1", "USD"), listOf(params[0].value, params[2].value))
        assertJson(
            """[{"productId":"sku-1","quantity":2,"price":10.0,"name":"Boot","category":"shoes",""" +
                """"discount":1.5,"size":["42"]},""" +
                """{"productId":"sku-2","quantity":1,"price":5.0}]""",
            params[1]
        )
    }

    @Test
    fun `should store cart without currency`() = runUnitTest {
        given()
        val event = EcomEvent.CartUpdated("cart-1", emptyList(), occurred = OCCURRED)

        whenn()
        val params = event.toDb().params.orEmpty()

        then()
        assertEquals(listOf("cartId", "products"), params.map { it.name })
    }

    @Test
    fun `should store created and updated order with every detail`() = runUnitTest {
        given()
        val order = fullOrder()
        val events = listOf(
            EcomEvent.OrderCreated(order, "USD", OCCURRED),
            EcomEvent.OrderUpdated(order, "USD", OCCURRED)
        )

        whenn()
        val params = events.map { it.toDb().params.orEmpty() }

        then()
        val expectedNames = listOf(
            "externalOrderId", "externalCustomerId", "totalCost", "status", "date", "cartId", "currencyCode", "email",
            "phone", "firstName", "lastName", "shipping", "discount", "taxes", "restoreUrl", "statusDescription",
            "storeId", "source", "deliveryMethod", "paymentMethod", "deliveryAddress", "items", "gift", "tags", "note"
        )
        val expectedValues = listOf(
            "order-1", "customer-1", "99.5", "IN_PROGRESS", "2026-01-02T01:04:05Z", "cart-1", "USD", "a@b.c", "+100",
            "Ann", "Lee", "5.0", "2.5", "1.0", "https://restore", "packed", "store-1", "app", "courier", "card",
            "Main st 1"
        )
        params.forEach { list ->
            assertEquals(expectedNames, list.map { it.name })
            assertEquals(expectedValues, list.take(expectedValues.size).map { it.value })
            assertJson(
                """[{"externalItemId":"item-1","name":"Boot","category":"shoes","quantity":1.0,"cost":99.5,""" +
                    """"url":"https://item","imageUrl":"https://img","description":"leather"}]""",
                list[21]
            )
            assertJson("""{"wrap":true}""", list[22])
            assertJson("""["a","b"]""", list[23])
            assertEquals("plain text", list[24].value)
        }
    }

    @Test
    fun `should store created and updated order with required details only`() = runUnitTest {
        given()
        val events = listOf(
            EcomEvent.OrderCreated(minimalOrder(), occurred = OCCURRED),
            EcomEvent.OrderUpdated(minimalOrder(), occurred = OCCURRED)
        )

        whenn()
        val params = events.map { event -> event.toDb().params.orEmpty().map { it.name to it.value } }

        then()
        val expected = listOf(
            "externalOrderId" to "order-1",
            "totalCost" to "99.5",
            "status" to "INITIALIZED",
            "date" to "2026-01-02T01:04:05Z"
        )
        assertEquals(listOf(expected, expected), params)
    }

    @Test
    fun `should store delivered and cancelled order by its id`() = runUnitTest {
        given()
        val events = listOf(
            EcomEvent.OrderDelivered("order-1", OCCURRED),
            EcomEvent.OrderCancelled("order-2", OCCURRED)
        )

        whenn()
        val params = events.map { it.toDb().params }

        then()
        assertEquals(
            listOf(
                listOf(ParameterDb("externalOrderId", "order-1")),
                listOf(ParameterDb("externalOrderId", "order-2"))
            ),
            params
        )
    }

    @Test
    fun `should store search with whether something was found as number`() = runUnitTest {
        given()
        val events = listOf(
            EcomEvent.SearchRequest("boots", true, OCCURRED),
            EcomEvent.SearchRequest("hats", occurred = OCCURRED)
        )

        whenn()
        val params = events.map { it.toDb().params }

        then()
        assertEquals(
            listOf(
                listOf(ParameterDb("search", "boots"), ParameterDb("isFound", "1")),
                listOf(ParameterDb("search", "hats"), ParameterDb("isFound", "0"))
            ),
            params
        )
    }

    @Test
    fun `should turn ecom event into a regular event of its type`() = runUnitTest {
        given()
        val event = EcomEvent.OrderDelivered("order-1", OCCURRED)

        whenn()
        val regular = event.asRegularEvent()

        then()
        assertEquals(listOf("orderDelivered", OCCURRED), listOf(regular.eventTypeKey, regular.occurred))
    }

    private fun assertJson(expected: String, actual: ParameterDb) {
        assertEquals(JsonParser.parseString(expected), JsonParser.parseString(actual.value))
    }

    private fun minimalOrder() = Order(
        externalOrderId = "order-1",
        externalCustomerId = null,
        totalCost = 99.5,
        status = OrderStatus.INITIALIZED,
        date = OCCURRED
    )

    private fun fullOrder() = Order.Builder(
        externalOrderId = "order-1",
        externalCustomerId = "customer-1",
        totalCost = 99.5,
        status = OrderStatus.IN_PROGRESS,
        date = OCCURRED
    ).apply {
        cartId = "cart-1"
        email = "a@b.c"
        phone = "+100"
        firstName = "Ann"
        lastName = "Lee"
        shipping = 5.0
        discount = 2.5
        taxes = 1.0
        restoreUrl = "https://restore"
        statusDescription = "packed"
        storeId = "store-1"
        source = "app"
        deliveryMethod = "courier"
        paymentMethod = "card"
        deliveryAddress = "Main st 1"
        items = listOf(OrderItem("item-1", "Boot", "shoes", 1.0, 99.5, "https://item", "https://img", "leather"))
        attributes = listOf(
            Pair("gift", """{"wrap":true}"""),
            Pair("tags", """["a","b"]"""),
            Pair("note", "plain text")
        )
    }.build()

    private companion object {
        val OCCURRED: ZonedDateTime = ZonedDateTime.of(2026, 1, 2, 3, 4, 5, 678_000_000, ZoneOffset.ofHours(2))
        val PRODUCT = ProductView(productId = "sku-1", price = 10.5, isInStock = true)
    }
}
