package com.reteno.core.domain

import com.reteno.core.domain.model.user.Address
import com.reteno.core.domain.model.user.User
import com.reteno.core.domain.model.user.UserAttributes
import com.reteno.core.domain.model.user.UserAttributesAnonymous
import com.reteno.core.domain.model.user.UserCustomField
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class ValidatorTest {

    @Test
    fun `should drop user without anything to send`() = runUnitTest {
        given()
        val user = User(userAttributes = UserAttributes(address = Address(null, null, null, null)))

        whenn()
        val validated = Validator.validateUser(user)

        then()
        assertNull(validated)
    }

    @Test
    fun `should keep user with only subscriptions or groups`() = runUnitTest {
        given()
        val users = listOf(
            User(subscriptionKeys = listOf("news")),
            User(groupNamesInclude = listOf("vip")),
            User(groupNamesExclude = listOf("churned"))
        )

        whenn()
        val validated = users.map { Validator.validateUser(it) }

        then()
        assertEquals(users, validated)
    }

    @Test
    fun `should drop empty attributes of a user`() = runUnitTest {
        given()
        val user = User(userAttributes = UserAttributes(), subscriptionKeys = listOf("news"))

        whenn()
        val validated = Validator.validateUser(user)

        then()
        assertEquals(User(subscriptionKeys = listOf("news")), validated)
    }

    @Test
    fun `should keep user attributes when any of them is set`() = runUnitTest {
        given()
        val attributes = listOf(
            UserAttributes(phone = "+100"),
            UserAttributes(email = "a@b.c"),
            UserAttributes(firstName = "John"),
            UserAttributes(lastName = "Doe"),
            UserAttributes(languageCode = "uk"),
            UserAttributes(timeZone = "Europe/Kyiv"),
            UserAttributes(address = Address(region = "Kyiv", town = null, address = null, postcode = null)),
            UserAttributes(fields = listOf(UserCustomField("plan", "pro"))),
            UserAttributes(marketId = "market-1")
        )

        whenn()
        val validated = attributes.map { Validator.validateUser(User(userAttributes = it))?.userAttributes }

        then()
        assertEquals(attributes, validated)
    }

    @Test
    fun `should drop empty address but keep other user attributes`() = runUnitTest {
        given()
        val user = User(userAttributes = UserAttributes(email = "a@b.c", address = Address(null, null, null, null)))

        whenn()
        val validated = Validator.validateUser(user)

        then()
        assertEquals(User(userAttributes = UserAttributes(email = "a@b.c")), validated)
    }

    @Test
    fun `should keep address when any of its parts is set`() = runUnitTest {
        given()
        val addresses = listOf(
            Address(region = "Kyiv", town = null, address = null, postcode = null),
            Address(region = null, town = "Kyiv", address = null, postcode = null),
            Address(region = null, town = null, address = "Khreshchatyk 1", postcode = null),
            Address(region = null, town = null, address = null, postcode = "01001")
        )

        whenn()
        val validated = addresses.map {
            Validator.validateUser(User(userAttributes = UserAttributes(address = it)))?.userAttributes?.address
        }

        then()
        assertEquals(addresses, validated)
    }

    @Test
    fun `should drop anonymous attributes without anything to send`() = runUnitTest {
        given()
        val attributes = UserAttributesAnonymous(address = Address(null, null, null, null))

        whenn()
        val validated = Validator.validateAnonymousUserAttributes(attributes)

        then()
        assertNull(validated)
    }

    @Test
    fun `should keep anonymous attributes when any of them is set`() = runUnitTest {
        given()
        val attributes = listOf(
            UserAttributesAnonymous(firstName = "John"),
            UserAttributesAnonymous(lastName = "Doe"),
            UserAttributesAnonymous(languageCode = "uk"),
            UserAttributesAnonymous(timeZone = "Europe/Kyiv"),
            UserAttributesAnonymous(address = Address(region = null, town = "Kyiv", address = null, postcode = null)),
            UserAttributesAnonymous(fields = listOf(UserCustomField("plan", null))),
            UserAttributesAnonymous(marketId = "market-1")
        )

        whenn()
        val validated = attributes.map { Validator.validateAnonymousUserAttributes(it) }

        then()
        assertEquals(attributes, validated)
    }

    @Test
    fun `should drop empty address but keep other anonymous attributes`() = runUnitTest {
        given()
        val attributes = UserAttributesAnonymous(firstName = "John", address = Address(null, null, null, null))

        whenn()
        val validated = Validator.validateAnonymousUserAttributes(attributes)

        then()
        assertEquals(UserAttributesAnonymous(firstName = "John"), validated)
    }
}
