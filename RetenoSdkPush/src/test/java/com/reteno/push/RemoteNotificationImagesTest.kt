package com.reteno.push

import com.reteno.push.fixtures.testApplication
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.Base64
import kotlin.concurrent.thread

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class RemoteNotificationImagesTest {

    private class SutFixture {
        val sut = RemoteNotificationImages(testApplication())

        inline fun <T> withImageServer(block: (imageUrl: String, baseUrl: String) -> T): T {
            val server = ServerSocket(0, 10, InetAddress.getLoopbackAddress())
            val worker = thread { serve(server) }
            try {
                val base = "http://127.0.0.1:${server.localPort}"
                return block("$base/image.png", "$base")
            } finally {
                server.close()
                worker.join()
            }
        }

        fun serve(server: ServerSocket) {
            while (true) {
                val socket = runCatching { server.accept() }.getOrNull() ?: return
                runCatching { respond(socket) }
            }
        }

        fun respond(socket: Socket) = socket.use {
            val requestLine = it.getInputStream().bufferedReader().readLine().orEmpty()
            val output = it.getOutputStream()
            when {
                requestLine.contains("/declared-huge.png") -> {
                    val headers = "HTTP/1.1 200 OK\r\nContent-Length: ${HUGE_PNG.size}\r\nConnection: close\r\n\r\n"
                    output.write(headers.toByteArray())
                    output.write(HUGE_PNG)
                }
                requestLine.contains("/streamed-huge.png") -> {
                    output.write("HTTP/1.1 200 OK\r\nConnection: close\r\n\r\n".toByteArray())
                    output.write(HUGE_PNG)
                }
                requestLine.contains("/image.png") -> {
                    val headers = "HTTP/1.1 200 OK\r\nContent-Type: image/png\r\n" +
                        "Content-Length: ${PNG.size}\r\nConnection: close\r\n\r\n"
                    output.write(headers.toByteArray())
                    output.write(PNG)
                }
                else -> {
                    val headers = "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
                    output.write(headers.toByteArray())
                }
            }
            output.flush()
        }
    }

    @Test
    fun `should download carousel image scaled to carousel size`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val image = fixture.withImageServer { imageUrl, _ -> fixture.sut.carouselImage(imageUrl) }

        then()
        assertNotNull(image)
        assertTrue(image!!.width <= Constants.NOTIFICATION_CAROUSEL_IMAGE_MAX_WIDTH_PX)
        assertTrue(image.height <= Constants.NOTIFICATION_CAROUSEL_IMAGE_MAX_HEIGHT_PX)
    }

    @Test
    fun `should skip carousel image that can not be downloaded`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val image = fixture.withImageServer { _, baseUrl -> fixture.sut.carouselImage("$baseUrl/missing.png") }

        then()
        assertNull(image)
    }

    @Test
    fun `should skip images larger than the download limit`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val images = fixture.withImageServer { _, baseUrl ->
            listOf("declared-huge.png", "streamed-huge.png").map { fixture.sut.carouselImage("$baseUrl/$it") }
        }

        then()
        assertEquals(listOf(null, null), images)
    }

    @Test
    fun `should not load images from non-web urls`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val images = listOf("file:///data/local/image.png", "ftp://cdn.example/image.png")
            .map(fixture.sut::carouselImage)

        then()
        assertEquals(listOf(null, null), images)
    }

    @Test
    fun `should download big picture`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val image = fixture.withImageServer { imageUrl, _ -> fixture.sut.bigPicture(imageUrl) }

        then()
        assertNotNull(image)
    }

    private companion object {
        const val MAX_IMAGE_BYTES = 5 * 1024 * 1024
        val PNG: ByteArray = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg=="
        )
        val HUGE_PNG: ByteArray = PNG + ByteArray(MAX_IMAGE_BYTES + 1 - PNG.size)
    }
}
