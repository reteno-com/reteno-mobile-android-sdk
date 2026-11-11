package com.reteno.core.internal.action.user

import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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

    @Test
    fun `should keep language code with region in canonical case`() = runUnitTest {
        given()
        val languageCode = "DE-at"

        whenn()
        val result = UserAttributesValidator.languageCodeWithRegion(languageCode)

        then()
        assertEquals("de-AT", result)
    }

    @Test
    fun `should keep language code with script and region`() = runUnitTest {
        given()
        val languageCode = "sr-Latn-RS"

        whenn()
        val result = UserAttributesValidator.languageCodeWithRegion(languageCode)

        then()
        assertEquals("sr-Latn-RS", result)
    }

    @Test
    fun `should keep language code with numeric region`() = runUnitTest {
        given()
        val languageCode = "es-419"

        whenn()
        val result = UserAttributesValidator.languageCodeWithRegion(languageCode)

        then()
        assertEquals("es-419", result)
    }

    @Test
    fun `should drop unicode extensions from language code`() = runUnitTest {
        given()
        val languageCode = "de-AT-u-mu-celsius"

        whenn()
        val result = UserAttributesValidator.languageCodeWithRegion(languageCode)

        then()
        assertEquals("de-AT", result)
    }

    @Test
    fun `should reject language code without region`() = runUnitTest {
        given()
        val languageCode = "de"

        whenn()
        val result = UserAttributesValidator.languageCodeWithRegion(languageCode)

        then()
        assertNull(result)
    }

    @Test
    fun `should reject language code separated with underscore`() = runUnitTest {
        given()
        val languageCode = "de_AT"

        whenn()
        val result = UserAttributesValidator.languageCodeWithRegion(languageCode)

        then()
        assertNull(result)
    }

    @Test
    fun `should reject language code with three letter language`() = runUnitTest {
        given()
        val languageCode = "deu-AT"

        whenn()
        val result = UserAttributesValidator.languageCodeWithRegion(languageCode)

        then()
        assertNull(result)
    }

    @Test
    fun `should reject language code that is only partly well formed`() = runUnitTest {
        given()
        val languageCode = "de-AT-"

        whenn()
        val result = UserAttributesValidator.languageCodeWithRegion(languageCode)

        then()
        assertNull(result)
    }

    private companion object {
        const val INVALID_MARKET_ID_MESSAGE =
            "Invalid marketId, max length is 64, allowed symbols: latin characters, digits, '-', '_'"
    }
}
