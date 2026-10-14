package com.reteno.core.internal.log

import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

internal class RedactTest {

    @Test
    fun `should show only last four characters of token`() = runUnitTest {
        given()
        val token = "dGhpcy1pcy1hLXRva2VuLTEyMzQ"

        whenn()
        val redacted = Redact.token(token)

        then()
        assertEquals("…yMzQ", redacted)
    }

    @Test
    fun `should hide short token completely`() = runUnitTest {
        given()
        val token = "abcd"

        whenn()
        val redacted = Redact.token(token)

        then()
        assertEquals("<hidden>", redacted)
    }

    @Test
    fun `should mark missing token as none`() = runUnitTest {
        given()
        val token: String? = null

        whenn()
        val redacted = Redact.token(token)

        then()
        assertEquals("<none>", redacted)
    }

    @Test
    fun `should mark empty token as none`() = runUnitTest {
        given()
        val token = ""

        whenn()
        val redacted = Redact.token(token)

        then()
        assertEquals("<none>", redacted)
    }

    @Test
    fun `should mask email keeping first character and top level domain`() = runUnitTest {
        given()
        val email = "john.doe@example.com"

        whenn()
        val redacted = Redact.email(email)

        then()
        assertEquals("j***@***.com", redacted)
    }

    @Test
    fun `should mask email without top level domain`() = runUnitTest {
        given()
        val email = "john@localhost"

        whenn()
        val redacted = Redact.email(email)

        then()
        assertEquals("j***@***", redacted)
    }

    @Test
    fun `should not treat dot before at sign as top level domain`() = runUnitTest {
        given()
        val email = "john.doe@localhost"

        whenn()
        val redacted = Redact.email(email)

        then()
        assertEquals("j***@***", redacted)
    }

    @Test
    fun `should hide value that is not an email`() = runUnitTest {
        given()
        val email = "@example.com"

        whenn()
        val redacted = Redact.email(email)

        then()
        assertEquals("<hidden>", redacted)
    }

    @Test
    fun `should mark missing email as none`() = runUnitTest {
        given()
        val email: String? = null

        whenn()
        val redacted = Redact.email(email)

        then()
        assertEquals("<none>", redacted)
    }

    @Test
    fun `should mask phone keeping last four digits`() = runUnitTest {
        given()
        val phone = "+38 (050) 123-4567"

        whenn()
        val redacted = Redact.phone(phone)

        then()
        assertEquals("***4567", redacted)
    }

    @Test
    fun `should hide short phone completely`() = runUnitTest {
        given()
        val phone = "1-23"

        whenn()
        val redacted = Redact.phone(phone)

        then()
        assertEquals("***", redacted)
    }

    @Test
    fun `should mark absent phone as none`() = runUnitTest {
        given()
        val phone: String? = null

        whenn()
        val redacted = Redact.phone(phone)

        then()
        assertEquals("<none>", redacted)
    }

    @Test
    fun `should mark empty phone as none`() = runUnitTest {
        given()
        val phone = ""

        whenn()
        val redacted = Redact.phone(phone)

        then()
        assertEquals("<none>", redacted)
    }

    @Test
    fun `should list names of present fields only`() = runUnitTest {
        given()
        val fields = arrayOf("firstName" to "John", "lastName" to null, "email" to "j@e.com")

        whenn()
        val redacted = Redact.fields(*fields)

        then()
        assertEquals("{firstName, email}", redacted)
    }

    @Test
    fun `should replace urls with placeholder`() = runUnitTest {
        given()
        val text = "failed to call https://mobile-api.reteno.com/api/v1/device?x=1 and ws://host:8080/a"

        whenn()
        val redacted = Redact.text(text)

        then()
        assertEquals("failed to call <url> and <url>", redacted)
    }

    @Test
    fun `should replace quoted host names with placeholder`() = runUnitTest {
        given()
        val text = "Unable to resolve host \"mobile-api.reteno.com\": No address associated with hostname"

        whenn()
        val redacted = Redact.text(text)

        then()
        assertEquals("Unable to resolve host \"<host>\": No address associated with hostname", redacted)
    }

    @Test
    fun `should summarize throwable with simple class name and redacted message`() = runUnitTest {
        given()
        val error = IOException("timeout calling https://mobile-api.reteno.com/logs/v1/events")

        whenn()
        val summary = Redact.summary(error)

        then()
        assertEquals("IOException: timeout calling <url>", summary)
    }

    @Test
    fun `should summarize throwable without message with class name only`() = runUnitTest {
        given()
        val error = IllegalStateException()

        whenn()
        val summary = Redact.summary(error)

        then()
        assertEquals("IllegalStateException", summary)
    }

    @Test
    fun `should print redacted stack trace with causes`() = runUnitTest {
        given()
        val cause = IOException("host https://mobile-api.reteno.com/x unreachable")
        val error = IllegalStateException("outer", cause)

        whenn()
        val trace = Redact.stackTrace(error)

        then()
        val lines = trace.lines()
        assertEquals("java.lang.IllegalStateException: outer", lines.first())
        assertTrue(lines.contains("Caused by: java.io.IOException: host <url> unreachable"))
        assertTrue(lines.any { it.startsWith("\tat com.reteno.core.internal.log.RedactTest") })
        assertFalse(trace.contains("reteno.com"))
    }

    @Test
    fun `should stop printing stack trace on cyclic cause`() = runUnitTest {
        given()
        val first = IllegalStateException("first")
        val second = IllegalArgumentException("second", first)
        first.initCause(second)

        whenn()
        val trace = Redact.stackTrace(first)

        then()
        assertEquals(1, trace.lines().count { it.startsWith("Caused by: ") })
    }

    @Test
    fun `should drop interpolated values from uploadable message`() = runUnitTest {
        given()
        val message = "getDevices(). Removed invalid entry from database. " +
            "device=DeviceDb(email=john@example.com, pushToken=abc)"

        whenn()
        val uploadable = Redact.uploadable(message)

        then()
        assertEquals("getDevices(). Removed invalid entry from database. device", uploadable)
    }

    @Test
    fun `should drop bracketed values and urls from uploadable message`() = runUnitTest {
        given()
        val message = "Failed on scale image https://cdn.example/x.png to (10, 20)"

        whenn()
        val uploadable = Redact.uploadable(message)

        then()
        assertEquals("Failed on scale image <url> to (10, 20)", uploadable)
    }

    @Test
    fun `should keep uploadable message without values unchanged`() = runUnitTest {
        given()
        val message = "Removed events(purchase) - 3"

        whenn()
        val uploadable = Redact.uploadable(message)

        then()
        assertEquals(message, uploadable)
    }

    @Test
    fun `should sanitize every exception message in uploadable stack trace`() = runUnitTest {
        given()
        val cause = IllegalStateException("recomEvents = RecomEvents(productId=secret)")
        val error = RuntimeException("token = [fcm-secret]", cause)

        whenn()
        val trace = Redact.uploadableStackTrace(error)

        then()
        val lines = trace.lines()
        assertEquals("java.lang.RuntimeException: token", lines.first())
        assertTrue(lines.contains("Caused by: java.lang.IllegalStateException: recomEvents"))
        assertFalse(trace.contains("secret"))
    }

    @Test
    fun `should mask email addresses in text`() = runUnitTest {
        given()
        val text = "user john.doe+tag@example.co.uk rejected"

        whenn()
        val redacted = Redact.text(text)

        then()
        assertEquals("user <email> rejected", redacted)
    }

    @Test
    fun `should mask long digit sequences such as phone numbers in text`() = runUnitTest {
        given()
        val text = "phone +38 (050) 123-45-67 is invalid"

        whenn()
        val redacted = Redact.text(text)

        then()
        assertEquals("phone <number> is invalid", redacted)
    }

    @Test
    fun `should keep short numbers in text`() = runUnitTest {
        given()
        val text = "Removed events(purchase) - 123456"

        whenn()
        val redacted = Redact.text(text)

        then()
        assertEquals(text, redacted)
    }

    @Test
    fun `should mask quoted json values in text`() = runUnitTest {
        given()
        val text = """Unterminated object {"firstName": "John", "email":"j@x.io"}"""

        whenn()
        val redacted = Redact.text(text)

        then()
        assertEquals("""Unterminated object {"firstName": "<hidden>", "email":"<hidden>"}""", redacted)
    }
}
