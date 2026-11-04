package com.reteno.core.data.local.file

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.reteno.core.R
import com.reteno.core.fixtures.CountingDispatcher
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class FileManagerTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    private class SutFixture(scope: TestScope) {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val root = File(application.filesDir, "reteno").also { it.deleteRecursively() }
        val countingIo = CountingDispatcher(StandardTestDispatcher(scope.testScheduler))
        val dispatchers = object : RetenoDispatchers {
            override val main: CoroutineDispatcher = countingIo
            override val io: CoroutineDispatcher = countingIo
            override val default: CoroutineDispatcher = countingIo
        }
        val sut = FileManager(application, dispatchers)

        fun bundledBaseHtml(): String =
            application.resources.openRawResource(R.raw.base_html).use { it.readBytes().toString(Charsets.UTF_8) }
    }

    @Test
    fun `should read back base html saved to file`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        val saved = fixture.sut.saveBaseHtmlContent(BASE_HTML)

        then()
        assertTrue(saved.isSuccess)
        assertEquals(BASE_HTML, fixture.sut.getBaseHtmlContent())
        assertEquals(BASE_HTML, File(fixture.root, "inapp/inapp_base.html").readText())
    }

    @Test
    fun `should replace base html saved before`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.sut.saveBaseHtmlContent("<html>old</html>")

        whenn()
        fixture.sut.saveBaseHtmlContent(BASE_HTML)

        then()
        assertEquals(BASE_HTML, fixture.sut.getBaseHtmlContent())
    }

    @Test
    fun `should read bundled base html until one is saved`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        val html = fixture.sut.getBaseHtmlContent()

        then()
        assertEquals(fixture.bundledBaseHtml(), html)
    }

    @Test
    fun `should fail to save base html when its folder cannot be created`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.root.writeText("not a folder")

        whenn()
        val saved = fixture.sut.saveBaseHtmlContent(BASE_HTML)

        then()
        assertTrue(saved.isFailure)
        fixture.root.delete()
    }

    @Test
    fun `should work with files on io dispatcher`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut.saveBaseHtmlContent(BASE_HTML)
        fixture.sut.getBaseHtmlContent()

        then()
        assertEquals(2, fixture.countingIo.dispatchCount)
    }

    companion object {
        private const val BASE_HTML = "<html>Привіт</html>"
    }
}
