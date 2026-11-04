package com.reteno.core.util

import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.MalformedURLException

internal class BitmapUtilImageConnectionTest {

    @Test
    fun `should bound image downloads with connect and read timeouts`() = runUnitTest {
        given()
        val imageUrl = "https://cdn.example/image.png"

        whenn()
        val connection = BitmapUtil.openImageConnection(imageUrl)

        then()
        assertEquals(3_000, connection.connectTimeout)
        assertEquals(5_000, connection.readTimeout)
    }

    @Test
    fun `should refuse image urls that are not http or https`() = runUnitTest {
        given()
        val urls = listOf("file:///data/local/image.png", "ftp://cdn.example/image.png")

        whenn()
        val failures = urls.map { runCatching { BitmapUtil.openImageConnection(it) }.exceptionOrNull() }

        then()
        assertTrue(failures.all { it is MalformedURLException })
    }

    @Test
    fun `should accept plain http image urls`() = runUnitTest {
        given()
        val imageUrl = "http://cdn.example/image.png"

        whenn()
        val connection = BitmapUtil.openImageConnection(imageUrl)

        then()
        assertEquals(imageUrl, connection.url.toString())
    }
}
