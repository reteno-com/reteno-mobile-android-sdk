package com.reteno.core.internal.action.device

import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import com.reteno.core.fixtures.FakeAppPlatform
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.testDispatchers
import com.reteno.core.internal.action.trace.TraceFormatter
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import com.reteno.test.workScope
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Locale

internal class DeviceLanguageResolverTest {

    private class SutFixture(scope: TestScope) {
        val prefs = mockk<SharedPrefsManager>()
        val platform = FakeAppPlatform()
        val dispatchers = scope.testDispatchers()
        val traced = TestActionRunner(scope)
        var explicit: String? = null
        var detected: DetectedLanguage? = null
        val savedDetections = mutableListOf<DetectedLanguage>()
        var detectionSavedOnIo: Boolean? = null
        val sut = DeviceLanguageResolver(prefs, platform, dispatchers)

        init {
            every { prefs.getExplicitDeviceLanguage() } answers { explicit }
            every { prefs.saveExplicitDeviceLanguage(any()) } answers { saveExplicit(firstArg()) }
            every { prefs.getDetectedDeviceLanguage() } answers { detected }
            every { prefs.saveDetectedDeviceLanguage(any()) } answers { saveDetected(firstArg()) }
        }

        fun locales(system: String, app: String = system, default: String = system) {
            platform.systemLocale = Locale.forLanguageTag(system)
            platform.appLocale = Locale.forLanguageTag(app)
            platform.defaultLocale = Locale.forLanguageTag(default)
        }

        suspend fun resolve(): String = traced.runner("Test") {
            sut.resolve(this)
        }.getOrThrow()

        suspend fun settle(displayed: String) {
            traced.runner("Test") {
                sut.settle(this, Locale.forLanguageTag(displayed))
            }.getOrThrow()
        }

        private fun saveExplicit(languageCode: String?) {
            explicit = languageCode
        }

        private fun saveDetected(language: DetectedLanguage) {
            detected = language
            savedDetections += language
            detectionSavedOnIo = dispatchers.markedIo.isRunningHere
        }
    }

    @Test
    fun `should use explicit language before the remembered one`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.explicit = "de-AT"
        fixture.detected = DetectedLanguage(languageCode = "fr-FR", systemLanguageCode = "uk-UA", source = "activity")

        whenn()
        val language = fixture.resolve()

        then()
        assertEquals("de-AT", language)
    }

    @Test
    fun `should use the remembered language without reading app languages again`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.detected = DetectedLanguage(languageCode = "fr-FR", systemLanguageCode = "uk-UA", source = "activity")
        fixture.locales(system = "en-US", app = "en-GB", default = "de-DE")

        whenn()
        val language = fixture.resolve()

        then()
        assertEquals("fr-FR", language)
        assertEquals(emptyList<DetectedLanguage>(), fixture.savedDetections)
    }

    @Test
    fun `should remember the app configuration language when nothing is remembered yet`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.locales(system = "uk-UA", app = "en-GB")

        whenn()
        val language = fixture.resolve()

        then()
        assertEquals("en-GB", language)
        val expected = DetectedLanguage(languageCode = "en-GB", systemLanguageCode = "uk-UA", source = "app")
        assertEquals(listOf(expected), fixture.savedDetections)
    }

    @Test
    fun `should remember the default locale provisionally when nothing is remembered yet`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.locales(system = "uk-UA", default = "de-DE")

        whenn()
        val language = fixture.resolve()

        then()
        assertEquals("de-DE", language)
        val expected = DetectedLanguage(languageCode = "de-DE", systemLanguageCode = "uk-UA", source = "provisional")
        assertEquals(listOf(expected), fixture.savedDetections)
    }

    @Test
    fun `should remember the system language provisionally when the app sets no other language`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.locales(system = "uk-UA")

        whenn()
        val language = fixture.resolve()

        then()
        assertEquals("uk-UA", language)
    }

    @Test
    fun `should save the remembered language on the io thread`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.resolve()

        then()
        assertEquals(true, fixture.detectionSavedOnIo)
    }

    @Test
    fun `should trace the source of the resolved language`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.locales(system = "uk-UA")

        whenn()
        fixture.resolve()

        then()
        val expected = TraceFormatter.stepSuccess(0, "r1", "resolve device language", "source=provisional", 0)
        assertTrue(expected in fixture.traced.sink.lines)
    }

    @Test
    fun `should complete app configuration language without region with its likely region`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.locales(system = "uk-UA", app = "en")

        whenn()
        val language = fixture.resolve()

        then()
        assertEquals("en-US", language)
    }

    @Test
    fun `should complete detected language with the system region when the languages match`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.platform.likelyRegions.clear()
        fixture.locales(system = "uk-UA", default = "uk")

        whenn()
        val language = fixture.resolve()

        then()
        assertEquals("uk-UA", language)
    }

    @Test
    fun `should complete detected language with its likely region when the system has no region`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.locales(system = "uk")

        whenn()
        val language = fixture.resolve()

        then()
        assertEquals("uk-UA", language)
    }

    @Test
    fun `should keep detected language without region when no likely region is known`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.locales(system = "uk-UA", default = "zz")

        whenn()
        val language = fixture.resolve()

        then()
        assertEquals("zz", language)
    }

    @Test
    fun `should keep script of a detected language when completing its region`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.platform.likelyRegions["sr"] = "RS"
        fixture.locales(system = "uk-UA", default = "sr-Latn")

        whenn()
        val language = fixture.resolve()

        then()
        assertEquals("sr-Latn-RS", language)
    }

    @Test
    fun `should drop unicode extensions from the app configuration language`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.locales(system = "uk-UA", app = "en-GB-u-mu-celsius")

        whenn()
        val language = fixture.resolve()

        then()
        assertEquals("en-GB", language)
    }

    @Test
    fun `should drop unicode extensions from the default locale`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.locales(system = "uk-UA", default = "de-DE-u-mu-celsius")

        whenn()
        val language = fixture.resolve()

        then()
        assertEquals("de-DE", language)
    }

    @Test
    fun `should not take app configuration language that differs from the system only in extensions`() =
        runUnitTest {
            given()
            val fixture = SutFixture(this)
            fixture.locales(system = "uk-UA-u-mu-celsius", app = "uk-UA")

            whenn()
            val language = fixture.resolve()

            then()
            assertEquals("uk-UA", language)
            assertEquals("provisional", fixture.savedDetections.single().source)
        }

    @Test
    fun `should remember the language the settled screen shows when it differs from the system`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.locales(system = "uk-UA", default = "de-DE")

        whenn()
        fixture.settle(displayed = "en-US")

        then()
        val expected = DetectedLanguage(languageCode = "en-US", systemLanguageCode = "uk-UA", source = "activity")
        assertEquals(listOf(expected), fixture.savedDetections)
    }

    @Test
    fun `should remember the app configuration language before the screen language`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.locales(system = "uk-UA", app = "en-GB")

        whenn()
        fixture.settle(displayed = "fr-FR")

        then()
        val expected = DetectedLanguage(languageCode = "en-GB", systemLanguageCode = "uk-UA", source = "app")
        assertEquals(listOf(expected), fixture.savedDetections)
    }

    @Test
    fun `should remember the default locale the app set when the screen shows the system language`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.locales(system = "uk-UA", default = "de-DE")

        whenn()
        fixture.settle(displayed = "uk-UA")

        then()
        val expected = DetectedLanguage(languageCode = "de-DE", systemLanguageCode = "uk-UA", source = "default")
        assertEquals(listOf(expected), fixture.savedDetections)
    }

    @Test
    fun `should keep the remembered default locale while the system language is unchanged`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.detected = DetectedLanguage(languageCode = "fr-FR", systemLanguageCode = "uk-UA", source = "default")
        fixture.locales(system = "uk-UA")

        whenn()
        fixture.settle(displayed = "uk-UA")

        then()
        assertEquals("fr-FR", fixture.detected?.languageCode)
        assertEquals(emptyList<DetectedLanguage>(), fixture.savedDetections)
    }

    @Test
    fun `should detect again after the system language changed`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.detected = DetectedLanguage(languageCode = "en-US", systemLanguageCode = "en-US", source = "default")
        fixture.locales(system = "uk-UA")

        whenn()
        fixture.settle(displayed = "uk-UA")

        then()
        val expected = DetectedLanguage(languageCode = "uk-UA", systemLanguageCode = "uk-UA", source = "default")
        assertEquals(listOf(expected), fixture.savedDetections)
    }

    @Test
    fun `should take the screen language over a remembered default locale`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.detected = DetectedLanguage(languageCode = "de-DE", systemLanguageCode = "uk-UA", source = "default")
        fixture.locales(system = "uk-UA")

        whenn()
        fixture.settle(displayed = "en-US")

        then()
        val expected = DetectedLanguage(languageCode = "en-US", systemLanguageCode = "uk-UA", source = "activity")
        assertEquals(listOf(expected), fixture.savedDetections)
    }

    @Test
    fun `should replace the remembered screen language when the screen shows another language`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.detected = DetectedLanguage(languageCode = "en-US", systemLanguageCode = "uk-UA", source = "activity")
        fixture.locales(system = "uk-UA")

        whenn()
        fixture.settle(displayed = "fr-FR")

        then()
        val expected = DetectedLanguage(languageCode = "fr-FR", systemLanguageCode = "uk-UA", source = "activity")
        assertEquals(listOf(expected), fixture.savedDetections)
    }

    @Test
    fun `should not save the language again when the screen shows the remembered one`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.detected = DetectedLanguage(languageCode = "en-US", systemLanguageCode = "uk-UA", source = "activity")
        fixture.locales(system = "uk-UA")

        whenn()
        fixture.settle(displayed = "en-US")

        then()
        assertEquals(emptyList<DetectedLanguage>(), fixture.savedDetections)
    }

    @Test
    fun `should detect again when the screen returns to the system language`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.detected = DetectedLanguage(languageCode = "en-US", systemLanguageCode = "uk-UA", source = "activity")
        fixture.locales(system = "uk-UA")

        whenn()
        fixture.settle(displayed = "uk-UA")

        then()
        val expected = DetectedLanguage(languageCode = "uk-UA", systemLanguageCode = "uk-UA", source = "default")
        assertEquals(listOf(expected), fixture.savedDetections)
    }

    @Test
    fun `should replace a provisional language once a screen settled`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.detected = DetectedLanguage(
            languageCode = "de-DE",
            systemLanguageCode = "uk-UA",
            source = "provisional"
        )
        fixture.locales(system = "uk-UA")

        whenn()
        fixture.settle(displayed = "uk-UA")

        then()
        val expected = DetectedLanguage(languageCode = "uk-UA", systemLanguageCode = "uk-UA", source = "default")
        assertEquals(listOf(expected), fixture.savedDetections)
    }

    @Test
    fun `should detect again when the remembered language has no source`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.detected = DetectedLanguage(languageCode = "en-US", systemLanguageCode = "uk-UA", source = null)
        fixture.locales(system = "uk-UA")

        whenn()
        fixture.settle(displayed = "uk-UA")

        then()
        assertEquals("uk-UA", fixture.detected?.languageCode)
    }

    @Test
    fun `should drop unicode extensions from the language the screen shows`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.locales(system = "uk-UA")

        whenn()
        fixture.settle(displayed = "en-US-u-fw-mon")

        then()
        assertEquals("en-US", fixture.detected?.languageCode)
    }

    @Test
    fun `should drop unicode extensions from the system language`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.locales(system = "uk-UA-u-mu-celsius")

        whenn()
        fixture.settle(displayed = "uk-UA-u-mu-celsius")

        then()
        val expected = DetectedLanguage(languageCode = "uk-UA", systemLanguageCode = "uk-UA", source = "default")
        assertEquals(listOf(expected), fixture.savedDetections)
    }

    @Test
    fun `should keep the settled language when a device save resolves at the same time`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.locales(system = "uk-UA")
        val work = workScope()
        work.launch { fixture.resolve() }

        whenn()
        work.launch { fixture.settle(displayed = "en-US") }
        runCurrent()

        then()
        val expected = DetectedLanguage(languageCode = "en-US", systemLanguageCode = "uk-UA", source = "activity")
        assertEquals(expected, fixture.detected)
    }

    @Test
    fun `should save the settled language on the io thread`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.settle(displayed = "en-US")

        then()
        assertEquals(true, fixture.detectionSavedOnIo)
    }

    @Test
    fun `should trace the source of the settled language`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.settle(displayed = "en-US")

        then()
        val expected = TraceFormatter.stepSuccess(
            0,
            "r1",
            "detect device language from the settled screen",
            "source=activity",
            0
        )
        assertTrue(expected in fixture.traced.sink.lines)
    }

    @Test
    fun `should save explicit language`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.traced.runner("Test") {
            fixture.sut.useExplicit(this, "de-AT")
        }

        then()
        assertEquals("de-AT", fixture.explicit)
    }

    @Test
    fun `should forget explicit language when returning to the device language`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.explicit = "de-AT"

        whenn()
        val forgotten = fixture.traced.runner("Test") {
            fixture.sut.useDetected(this)
        }.getOrThrow()

        then()
        assertTrue(forgotten)
        assertNull(fixture.explicit)
    }

    @Test
    fun `should report no explicit language to forget when none is stored`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        val forgotten = fixture.traced.runner("Test") {
            fixture.sut.useDetected(this)
        }.getOrThrow()

        then()
        assertFalse(forgotten)
        verify(exactly = 0) { fixture.prefs.saveExplicitDeviceLanguage(any()) }
    }
}
