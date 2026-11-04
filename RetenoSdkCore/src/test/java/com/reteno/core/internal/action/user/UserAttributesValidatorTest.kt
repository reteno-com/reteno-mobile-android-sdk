package com.reteno.core.internal.action.user

import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class UserAttributesValidatorTest {

    @Test
    fun `should reject blank external user id`() = runUnitTest {
        given()
        val externalUserId = "   "

        whenn()
        val result = runCatching { UserAttributesValidator.requireValidExternalUserId(externalUserId) }

        then()
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("externalUserId should not be null or blank", result.exceptionOrNull()?.message)
    }

    @Test
    fun `should accept non blank external user id`() = runUnitTest {
        given()
        val externalUserId = "user-1"

        whenn()
        val result = runCatching { UserAttributesValidator.requireValidExternalUserId(externalUserId) }

        then()
        assertTrue(result.isSuccess)
    }

    @Test
    fun `should accept missing market id`() = runUnitTest {
        given()
        val marketId: String? = null

        whenn()
        val result = runCatching { UserAttributesValidator.requireValidMarketId(marketId) }

        then()
        assertTrue(result.isSuccess)
    }

    @Test
    fun `should accept market id of letters digits dash and underscore that is exactly 64 characters long`() = runUnitTest {
        given()
        val marketId = "Ab-9_" + "x".repeat(59)

        whenn()
        val result = runCatching { UserAttributesValidator.requireValidMarketId(marketId) }

        then()
        assertTrue(result.isSuccess)
    }

    @Test
    fun `should reject market id that is 65 characters long`() = runUnitTest {
        given()
        val marketId = "x".repeat(65)

        whenn()
        val result = runCatching { UserAttributesValidator.requireValidMarketId(marketId) }

        then()
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals(INVALID_MARKET_ID_MESSAGE, result.exceptionOrNull()?.message)
    }

    @Test
    fun `should reject market id with unsupported characters`() = runUnitTest {
        given()
        val marketId = "market id!"

        whenn()
        val result = runCatching { UserAttributesValidator.requireValidMarketId(marketId) }

        then()
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals(INVALID_MARKET_ID_MESSAGE, result.exceptionOrNull()?.message)
    }

    private companion object {
        const val INVALID_MARKET_ID_MESSAGE =
            "Invalid marketId, max length is 64, allowed symbols: latin characters, digits, '-', '_'"
    }
}
