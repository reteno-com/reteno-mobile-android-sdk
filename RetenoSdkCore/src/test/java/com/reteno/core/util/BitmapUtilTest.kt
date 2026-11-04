package com.reteno.core.util

import android.app.Application
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.reteno.core.fixtures.LocalHttpServer
import com.reteno.core.fixtures.ScriptedResponse
import com.reteno.core.fixtures.stubPng
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class BitmapUtilTest {

    private class SutFixture(val server: LocalHttpServer) {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val imageUrl = "http://127.0.0.1:${server.port}/image.png"

        fun serveImage(width: Int, height: Int) {
            server.answer(ScriptedResponse(200, stubPng(width, height)))
        }
    }

    private suspend fun withSut(block: suspend SutFixture.() -> Unit) {
        LocalHttpServer.serve { server -> SutFixture(server).block() }
    }

    private val Bitmap.size: Pair<Int, Int>
        get() = width to height

    @Test
    fun `should fit wide image into big picture area of the screen`() = runUnitTest {
        withSut {
            given()
            serveImage(width = 800, height = 400)

            whenn()
            val bitmap = BitmapUtil.getScaledBitmap(context, imageUrl)

            then()
            assertEquals(BIG_PICTURE_AREA, bitmap?.size)
        }
    }

    @Test
    fun `should fit small square image into big picture area of the screen`() = runUnitTest {
        withSut {
            given()
            serveImage(width = 100, height = 100)

            whenn()
            val bitmap = BitmapUtil.getScaledBitmap(context, imageUrl)

            then()
            assertEquals(BIG_PICTURE_AREA, bitmap?.size)
        }
    }

    @Test
    fun `should fit long narrow image into big picture area of the screen`() = runUnitTest {
        withSut {
            given()
            serveImage(width = 1000, height = 100)

            whenn()
            val bitmap = BitmapUtil.getScaledBitmap(context, imageUrl)

            then()
            assertEquals(BIG_PICTURE_AREA, bitmap?.size)
        }
    }

    @Test
    fun `should keep image as it is when it is too thin to scale`() = runUnitTest {
        withSut {
            given()
            serveImage(width = 1, height = 4000)

            whenn()
            val bitmap = BitmapUtil.getScaledBitmap(context, imageUrl)

            then()
            assertEquals(1 to 4000, bitmap?.size)
        }
    }

    @Test
    fun `should give no big picture when image cannot be downloaded`() = runUnitTest {
        withSut {
            given()
            server.answer(ScriptedResponse(404, "missing"))

            whenn()
            val bitmap = BitmapUtil.getScaledBitmap(context, imageUrl)

            then()
            assertNull(bitmap)
        }
    }

    @Test
    fun `should download image as it is`() = runUnitTest {
        withSut {
            given()
            serveImage(width = 64, height = 32)

            whenn()
            val bitmap = BitmapUtil.downloadBitmap(imageUrl)

            then()
            assertEquals(64 to 32, bitmap?.size)
        }
    }

    @Test
    fun `should not download image that declares more than five megabytes`() = runUnitTest {
        withSut {
            given()
            server.answer(ScriptedResponse(200, ByteArray(MAX_IMAGE_BYTES + 1)))

            whenn()
            val bitmap = BitmapUtil.downloadBitmap(imageUrl)

            then()
            assertNull(bitmap)
        }
    }

    @Test
    fun `should stop reading image that streams more than five megabytes`() = runUnitTest {
        withSut {
            given()
            server.answer(ScriptedResponse(200, ByteArray(MAX_IMAGE_BYTES + 1), declaresLength = false))

            whenn()
            val bitmap = BitmapUtil.downloadBitmap(imageUrl)

            then()
            assertNull(bitmap)
        }
    }

    @Test
    fun `should keep image as it is when there are no bounds to fit it into`() = runUnitTest {
        given()
        val bitmap = Bitmap.createBitmap(10, 20, Bitmap.Config.ARGB_8888)

        whenn()
        val resized = listOf(BitmapUtil.resize(bitmap, 0, 100), BitmapUtil.resize(bitmap, 100, 0))

        then()
        resized.forEach { assertSame(bitmap, it) }
    }

    companion object {
        private const val MAX_IMAGE_BYTES = 5 * 1024 * 1024
        private val BIG_PICTURE_AREA = 320 to 193
    }
}
