package com.reteno.core.util

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.Resources
import android.os.Bundle
import com.reteno.core.domain.SchedulerUtils
import com.reteno.core.fixtures.FakeLogSink
import com.reteno.core.fixtures.FakeRemoteLog
import com.reteno.core.fixtures.installCapturing
import com.reteno.core.internal.log.AndroidLogSink
import com.reteno.core.util.Util.asZonedDateTime
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.time.Instant
import java.time.ZoneId
import java.util.TimeZone
import java.util.concurrent.TimeUnit

internal class UtilTest {

    private class SutFixture {
        val packageManager = mockk<PackageManager>()
        val applicationInfo = mockk<ApplicationInfo>()
        val context = mockk<Context>().also {
            every { it.packageManager } returns packageManager
            every { it.packageName } returns PACKAGE
            every { it.applicationInfo } returns applicationInfo
        }
        val metaData = mockk<Bundle>()
        val intent = mockk<Intent>()
        val receivers = listOf(mockk<ResolveInfo>())

        fun <T> withAndroid13Flags(block: () -> T): T {
            mockkStatic(PackageManager.ApplicationInfoFlags::class, PackageManager.ResolveInfoFlags::class)
            try {
                every { PackageManager.ApplicationInfoFlags.of(any()) } returns mockk()
                every { PackageManager.ResolveInfoFlags.of(any()) } returns mockk()
                return block()
            } finally {
                unmockkStatic(PackageManager.ApplicationInfoFlags::class, PackageManager.ResolveInfoFlags::class)
            }
        }

        fun rawResource(stream: InputStream) {
            val resources = mockk<Resources>()
            every { resources.openRawResource(RAW_ID) } returns stream
            every { context.resources } returns resources
        }
    }

    @Test
    fun `should tell whether all elements are absent`() = runUnitTest {
        given()
        val samples = listOf(arrayOf<String?>(null, null), arrayOf<String?>(null, "a"))

        whenn()
        val allNull = samples.map { allElementsNull(*it) }

        then()
        assertEquals(listOf(true, false), allNull)
    }

    @Test
    fun `should tell whether all elements are present`() = runUnitTest {
        given()
        val samples = listOf(arrayOf<String?>("a", "b"), arrayOf<String?>("a", null))

        whenn()
        val allPresent = samples.map { allElementsNotNull(*it) }

        then()
        assertEquals(listOf(true, false), allPresent)
    }

    @Test
    fun `should read time unit names from server`() = runUnitTest {
        given()
        val names = listOf("DAY", "HOUR", "MINUTE", "SECOND", "MILLISECOND", "WEEK", null)

        whenn()
        val units = names.map { it.toTimeUnit() }

        then()
        val expected = listOf(
            TimeUnit.DAYS,
            TimeUnit.HOURS,
            TimeUnit.MINUTES,
            TimeUnit.SECONDS,
            TimeUnit.MILLISECONDS,
            null,
            null
        )
        assertEquals(expected, units)
    }

    @Test
    fun `should retry only errors that are not client errors or are rate limits`() = runUnitTest {
        given()
        val codes = listOf(null, 399, 400, 429, 499, 500)

        whenn()
        val repeatable = codes.map { isRepeatableError(it) }

        then()
        assertEquals(listOf(true, true, false, true, false, true), repeatable)
    }

    @Test
    fun `should support android 8 and newer`() = runUnitTest {
        given()
        val levels = listOf(25, 26, 34)

        whenn()
        val supported = levels.map { isSupportedSdk(it) }

        then()
        assertEquals(listOf(false, true, true), supported)
        assertFalse(isOsVersionSupported())
    }

    @Test
    fun `should name app by its label when it has no label resource`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.applicationInfo.labelRes = 0
        every { fixture.applicationInfo.loadLabel(fixture.packageManager) } returns "Sample"

        whenn()
        val name = fixture.context.getAppName()

        then()
        assertEquals("Sample", name)
    }

    @Test
    fun `should name app by its label resource`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.applicationInfo.labelRes = LABEL_ID
        every { fixture.context.getString(LABEL_ID) } returns "Sample Shop"

        whenn()
        val name = fixture.context.getAppName()

        then()
        assertEquals("Sample Shop", name)
    }

    @Test
    fun `should read app meta data before android 13`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.applicationInfo.metaData = fixture.metaData
        every {
            fixture.packageManager.getApplicationInfo(PACKAGE, PackageManager.GET_META_DATA)
        } returns fixture.applicationInfo

        whenn()
        val metaData = listOf(fixture.context.getApplicationMetaData(32), fixture.context.getApplicationMetaData())

        then()
        metaData.forEach { assertSame(fixture.metaData, it) }
    }

    @Test
    fun `should read app meta data on android 13`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.applicationInfo.metaData = fixture.metaData
        every {
            fixture.packageManager.getApplicationInfo(PACKAGE, any<PackageManager.ApplicationInfoFlags>())
        } returns fixture.applicationInfo

        whenn()
        val metaData = fixture.withAndroid13Flags { fixture.context.getApplicationMetaData(33) }

        then()
        assertSame(fixture.metaData, metaData)
    }

    @Test
    fun `should find activities for intent before android 13`() = runUnitTest {
        given()
        val fixture = SutFixture()
        every { fixture.packageManager.queryIntentActivities(fixture.intent, 0) } returns fixture.receivers

        whenn()
        val activities = listOf(
            fixture.context.getResolveInfoList(fixture.intent, 32),
            fixture.context.getResolveInfoList(fixture.intent)
        )

        then()
        activities.forEach { assertEquals(fixture.receivers, it) }
    }

    @Test
    fun `should find activities for intent on android 13`() = runUnitTest {
        given()
        val fixture = SutFixture()
        every {
            fixture.packageManager.queryIntentActivities(fixture.intent, any<PackageManager.ResolveInfoFlags>())
        } returns fixture.receivers

        whenn()
        val activities = fixture.withAndroid13Flags { fixture.context.getResolveInfoList(fixture.intent, 33) }

        then()
        assertEquals(fixture.receivers, activities)
    }

    @Test
    fun `should find broadcast receivers for intent before android 13`() = runUnitTest {
        given()
        val fixture = SutFixture()
        every { fixture.packageManager.queryBroadcastReceivers(fixture.intent, 0) } returns fixture.receivers

        whenn()
        val receivers = listOf(
            fixture.context.queryBroadcastReceivers(fixture.intent, 32),
            fixture.context.queryBroadcastReceivers(fixture.intent)
        )

        then()
        receivers.forEach { assertEquals(fixture.receivers, it) }
    }

    @Test
    fun `should find broadcast receivers for intent on android 13`() = runUnitTest {
        given()
        val fixture = SutFixture()
        every {
            fixture.packageManager.queryBroadcastReceivers(fixture.intent, any<PackageManager.ResolveInfoFlags>())
        } returns fixture.receivers

        whenn()
        val receivers = fixture.withAndroid13Flags { fixture.context.queryBroadcastReceivers(fixture.intent, 33) }

        then()
        assertEquals(fixture.receivers, receivers)
    }

    @Test
    fun `should describe bundle with every key and value`() = runUnitTest {
        given()
        val bundle = mockk<Bundle>()
        every { bundle.keySet() } returns linkedSetOf("es_title", "es_count")
        every { bundle.get("es_title") } returns "Sale"
        every { bundle.get("es_count") } returns 3

        whenn()
        val described = bundle.toStringVerbose()

        then()
        assertEquals("Bundle: [es_title = Sale; es_count = 3]", described)
    }

    @Test
    fun `should describe empty and absent bundles`() = runUnitTest {
        given()
        val empty = mockk<Bundle>()
        every { empty.keySet() } returns emptySet()

        whenn()
        val described = listOf(empty.toStringVerbose(), (null as Bundle?).toStringVerbose())

        then()
        assertEquals(listOf("Bundle: []", "Bundle = [null]"), described)
    }

    @Test
    fun `should read raw resource as text`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val text = "{\"widget\":\"ок\"}" + "x".repeat(3000)
        fixture.rawResource(ByteArrayInputStream(text.toByteArray()))

        whenn()
        val read = Util.readFromRaw(fixture.context, RAW_ID)

        then()
        assertEquals(text, read)
    }

    @Test
    fun `should report raw resource that cannot be read`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val remoteLog = FakeRemoteLog()
        fixture.rawResource(object : InputStream() {
            override fun read(): Int = throw IOException("broken resource")
        })

        whenn()
        Logger.installCapturing(FakeLogSink(), remoteLog)
        val read = try {
            Util.readFromRaw(fixture.context, RAW_ID)
        } finally {
            Logger.install(AndroidLogSink(), null)
        }

        then()
        assertNull(read)
        assertTrue(remoteLog.errors.single().second is IOException)
    }

    @Test
    fun `should give current time to the second`() = runUnitTest {
        given()
        val pattern = Regex("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z""")

        whenn()
        val timestamp = Util.getCurrentTimeStamp()

        then()
        assertTrue(pattern.matches(timestamp)) { timestamp }
    }

    @Test
    fun `should read time of local database row as utc whatever the device time zone`() = runUnitTest {
        given()
        val sqlDate = "2024-03-01 10:15:30"
        val deviceZone = TimeZone.getDefault()

        whenn()
        val timestamps = listOf("UTC", "Europe/Kyiv", "America/Los_Angeles").map { zone ->
            TimeZone.setDefault(TimeZone.getTimeZone(zone))
            try {
                Util.formatSqlDateToTimestamp(sqlDate)
            } finally {
                TimeZone.setDefault(deviceZone)
            }
        }

        then()
        val expected = Instant.parse("2024-03-01T10:15:30Z").toEpochMilli()
        assertEquals(listOf(expected, expected, expected), timestamps)
    }

    @Test
    fun `should read schedule time in kyiv time zone`() = runUnitTest {
        given()
        val dateTime = "2024-03-01T10:15"

        whenn()
        val parsed = Util.parseWithTimeZone(dateTime, "Europe/Kyiv")

        then()
        assertEquals(ZoneId.of("Europe/Kiev"), parsed.zone)
        assertEquals(listOf(10, 15), listOf(parsed.hour, parsed.minute))
    }

    @Test
    fun `should tell outdated timestamps`() = runUnitTest {
        given()
        val now = 10_000_000L
        val window = SchedulerUtils.getOutdatedDeviceAndUserTime()

        whenn()
        val outdated = listOf(
            Util.isTimestampOutdated(now - window, now),
            Util.isTimestampOutdated(now - window - 1, now),
            Util.isTimestampOutdated(now + 1, now)
        )

        then()
        assertEquals(listOf(false, true, true), outdated)
    }

    @Test
    fun `should convert millis to date time in device time zone`() = runUnitTest {
        given()
        val millis = 1_700_000_000_000L

        whenn()
        val dateTime = millis.asZonedDateTime()

        then()
        assertEquals(millis, dateTime.toInstant().toEpochMilli())
        assertEquals(ZoneId.systemDefault(), dateTime.zone)
    }

    companion object {
        private const val PACKAGE = "com.example.shop"
        private const val LABEL_ID = 7
        private const val RAW_ID = 11
    }
}
