package com.reteno.core.domain

import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ResponseCallbackTest {

    @Test
    fun `should pass response without headers to callback that ignores them`() = runUnitTest {
        given()
        val received = mutableListOf<String>()
        val callback = object : ResponseCallback {
            override fun onSuccess(response: String) {
                received += response
            }

            override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) = Unit
        }

        whenn()
        callback.onSuccess(mapOf("etag" to listOf("v1")), "{}")

        then()
        assertEquals(listOf("{}"), received)
    }
}
