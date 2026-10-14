package com.reteno.core.data.remote.api

import com.reteno.core.RetenoConfig
import com.reteno.core.data.local.config.RestConfig
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.util.Logger
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.net.ssl.HttpsURLConnection

internal class RestClientImplLoggingTest {

    private class SutFixture {
        val accessKey = "secret-access-key-1234"
        val requestBody = """{"email":"john.doe@example.com","phone":"+380501234567"}"""
        val responseBody = """{"firstName":"John","pushToken":"fcm-token-abcdef"}"""
        val connection = mockk<HttpsURLConnection>(relaxed = true)
        val logged = mutableListOf<String>()
        var failureStatus: Int? = -1
        var successBody: String? = null
        val callback = object : ResponseCallback {
            override fun onSuccess(response: String) {
                successBody = response
            }

            override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
                failureStatus = statusCode
            }
        }
        val sut = RestClientImpl(
            RestConfig(mockk(relaxed = true), mockk(relaxed = true)),
            retenoConfigProvider = { RetenoConfig.Builder().accessKey(accessKey).setPlatform("Android").build() }
        )

        fun <T> withCapturedLogs(block: () -> T): T {
            mockkStatic(Logger::class)
            mockkObject(ConnectionManager)
            try {
                every { Logger.i(any(), any(), *anyVararg()) } answers { logged += flatten(args) }
                every { Logger.d(any(), any(), *anyVararg()) } answers { logged += flatten(args) }
                justRun { Logger.e(any(), any(), any()) }
                justRun { Logger.e(any(), any(), any(), *anyVararg()) }
                return block()
            } finally {
                unmockkObject(ConnectionManager)
                unmockkStatic(Logger::class)
            }
        }

        private fun flatten(args: List<Any?>): String =
            args.flatMap { if (it is Array<*>) it.toList() else listOf(it) }.joinToString(" ")
    }

    @Test
    fun `should not log access key bodies or url for successful request`() = runUnitTest {
        given()
        val fixture = SutFixture()
        every { fixture.connection.responseCode } returns 200
        every { fixture.connection.inputStream } returns ByteArrayInputStream(fixture.responseBody.toByteArray())
        every { fixture.connection.outputStream } returns ByteArrayOutputStream()
        every { fixture.connection.requestProperties } returns mapOf("X-Reteno-Access-Key" to listOf(fixture.accessKey))

        whenn()
        fixture.withCapturedLogs {
            every { ConnectionManager.openConnection(any()) } returns fixture.connection
            fixture.sut.makeRequest(
                HttpMethod.POST,
                ApiContract.MobileApi.Device,
                fixture.requestBody,
                responseCallback = fixture.callback
            )
        }

        then()
        assertEquals(fixture.responseBody, fixture.successBody)
        assertTrue(fixture.logged.isNotEmpty())
        assertNoSensitiveData(fixture)
    }

    @Test
    fun `should not log url or error body when request fails`() = runUnitTest {
        given()
        val fixture = SutFixture()
        every { fixture.connection.responseCode } returns 400
        every { fixture.connection.errorStream } returns ByteArrayInputStream(fixture.responseBody.toByteArray())
        every { fixture.connection.outputStream } returns ByteArrayOutputStream()

        whenn()
        fixture.withCapturedLogs {
            every { ConnectionManager.openConnection(any()) } returns fixture.connection
            fixture.sut.makeRequest(
                HttpMethod.POST,
                ApiContract.MobileApi.Device,
                fixture.requestBody,
                responseCallback = fixture.callback
            )
        }

        then()
        assertEquals(400, fixture.failureStatus)
        assertNoSensitiveData(fixture)
    }

    @Test
    fun `should log redacted error without url when connection throws`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val error = IOException("Unable to reach https://mobile-api.reteno.com/api/v1/device")

        whenn()
        fixture.withCapturedLogs {
            every { ConnectionManager.openConnection(any()) } throws error
            fixture.sut.makeRequest(
                HttpMethod.POST,
                ApiContract.MobileApi.Device,
                fixture.requestBody,
                responseCallback = fixture.callback
            )
        }

        then()
        assertEquals(null, fixture.failureStatus)
        assertTrue(fixture.logged.any { it.contains("IOException: Unable to reach <url>") })
        assertNoSensitiveData(fixture)
    }

    @Test
    fun `should send sdk version header without stray dollar sign`() = runUnitTest {
        given()
        val fixture = SutFixture()
        every { fixture.connection.responseCode } returns 200
        every { fixture.connection.inputStream } returns ByteArrayInputStream("ok".toByteArray())
        val versions = mutableListOf<String>()
        every { fixture.connection.setRequestProperty("X-Reteno-SDK-Version", capture(versions)) } returns Unit

        whenn()
        fixture.withCapturedLogs {
            every { ConnectionManager.openConnection(any()) } returns fixture.connection
            fixture.sut.makeRequest(
                HttpMethod.GET,
                ApiContract.MobileApi.Device,
                null,
                responseCallback = fixture.callback
            )
        }

        then()
        assertTrue(versions.single().startsWith("Android "))
    }

    private fun assertNoSensitiveData(fixture: SutFixture) {
        val sensitive = listOf(
            fixture.accessKey,
            "john.doe@example.com",
            "+380501234567",
            "fcm-token-abcdef",
            "https://",
            "reteno.com",
            "/api/v1/"
        )
        fixture.logged.forEach { line ->
            sensitive.forEach { value -> assertTrue(!line.contains(value)) { "Log line leaks '$value': $line" } }
        }
    }
}
