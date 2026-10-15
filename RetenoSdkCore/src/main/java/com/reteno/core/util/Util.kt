package com.reteno.core.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.Bundle
import com.reteno.core.domain.SchedulerUtils
import com.reteno.core.domain.model.event.LifecycleEventType
import com.reteno.core.domain.model.event.LifecycleTrackingOptions
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.concurrent.TimeUnit

fun <T : Any> allElementsNull(vararg elements: T?) = elements.all { it == null }

fun <T : Any> allElementsNotNull(vararg elements: T?) = elements.all { it != null }

fun Context.getAppName(): String {
    val stringId = applicationInfo.labelRes
    return if (stringId == 0) {
        applicationInfo.loadLabel(packageManager).toString()
    } else {
        getString(stringId)
    }
}

fun Context.getApplicationMetaData(): Bundle = getApplicationMetaData(Build.VERSION.SDK_INT)

internal fun Context.getApplicationMetaData(sdkInt: Int): Bundle =
    if (sdkInt >= 33) {
        packageManager.getApplicationInfo(
            packageName,
            PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())
        ).metaData
    } else {
        packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA).metaData
    }

fun Context.getResolveInfoList(intent: Intent): List<ResolveInfo?> = getResolveInfoList(intent, Build.VERSION.SDK_INT)

internal fun Context.getResolveInfoList(intent: Intent, sdkInt: Int): List<ResolveInfo?> =
    if (sdkInt >= 33) {
        packageManager.queryIntentActivities(
            intent,
            PackageManager.ResolveInfoFlags.of(0)
        )
    } else {
        packageManager.queryIntentActivities(intent, 0)
    }

fun Context.queryBroadcastReceivers(intent: Intent): List<ResolveInfo?> =
    queryBroadcastReceivers(intent, Build.VERSION.SDK_INT)

internal fun Context.queryBroadcastReceivers(intent: Intent, sdkInt: Int): List<ResolveInfo?> =
    if (sdkInt >= 33) {
        packageManager.queryBroadcastReceivers(
            intent,
            PackageManager.ResolveInfoFlags.of(0)
        )
    } else {
        packageManager.queryBroadcastReceivers(intent, 0)
    }


fun Bundle?.toStringVerbose(): String {
    if (this == null) return "Bundle = [null]"
    return keySet().joinToString(separator = "; ", prefix = "Bundle: [", postfix = "]") { key -> "$key = ${get(key)}" }
}

fun String?.toTimeUnit(): TimeUnit? {
    return when (this) {
        "DAY" -> TimeUnit.DAYS
        "HOUR" -> TimeUnit.HOURS
        "MINUTE" -> TimeUnit.MINUTES
        "SECOND" -> TimeUnit.SECONDS
        "MILLISECOND" -> TimeUnit.MILLISECONDS
        else -> null
    }
}

fun isRepeatableError(statusCode: Int?): Boolean {
    return statusCode == 429 || statusCode !in 400..499
}

fun isOsVersionSupported(): Boolean = isSupportedSdk(Build.VERSION.SDK_INT)

internal fun isSupportedSdk(sdkInt: Int): Boolean = sdkInt >= 26

object Util {

    private var isDebugViewCached: Boolean = false

    private val formatter = DateTimeFormatter
        .ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
        .withZone(ZoneId.of("UTC"))

    private val millisFormatter = DateTimeFormatter
        .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .withZone(ZoneId.of("UTC"))

    private val sqlToTimestampFormat by lazy {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    }

    private val patternWithNoSeconds = "yyyy-MM-dd'T'HH:mm"

    @JvmStatic
    fun readFromRaw(context: Context, rawResourceId: Int): String? {
        val inputStream: InputStream = context.resources.openRawResource(rawResourceId)
        val outputStream = ByteArrayOutputStream()

        val buf = ByteArray(1024)
        var len: Int
        return try {
            while (inputStream.read(buf).also { len = it } != -1) {
                outputStream.write(buf, 0, len)
            }
            outputStream.toString()
        } catch (e: IOException) {
            /*@formatter:off*/ Logger.e(TAG, "readTextFile(): ", e)
            /*@formatter:on*/
            null
        } finally {
            outputStream.close()
            inputStream.close()
        }
    }

    @JvmStatic
    fun getCurrentTimeStamp(): String {
        val currentDate = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        return currentDate.toString()
    }

    /**
     * To enable debugView mode run adb shell with the following command
     * adb shell setprop debug.com.reteno.debug.view enable
     * To disable change system property to any other one
     * adb shell setprop debug.com.reteno.debug.view disable
     */
    internal fun isDebugView(): Boolean {
        return isDebugViewCached
    }

    internal fun setIsDebug(isDebug: Boolean) {
        isDebugViewCached = isDebug
    }

    fun ZonedDateTime.formatToRemote(): String {
        return formatter.format(this)
    }

    fun ZonedDateTime.formatToRemoteExplicitMillis(): String {
        return millisFormatter.format(this)
    }

    fun String.fromRemoteExplicitMillis(): ZonedDateTime {
        return ZonedDateTime.parse(this, millisFormatter)
    }

    internal fun Long.asZonedDateTime(): ZonedDateTime {
        return ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(this),
            ZoneId.systemDefault()
        )
    }

    fun formatSqlDateToTimestamp(sqlDate: String): Long {
        return sqlToTimestampFormat.parse(sqlDate).time
    }

    fun parseWithTimeZone(dateTime: String, timeZone: String): ZonedDateTime {
        // TODO currently we are using Java 11, it does not recognize Kyiv timezone.
        // After updating java to 17.0.6 or higher this fix should not be required.
        val timeZoneFixed = timeZone.replace("Kyiv", "Kiev")

        val formatter = DateTimeFormatter
            .ofPattern(patternWithNoSeconds)
            .withZone(ZoneId.of(timeZoneFixed))

        return ZonedDateTime.parse(dateTime, formatter)
    }

    /**
     *  Returns true if timestamp is older than 40 hours
     */
    fun isTimestampOutdated(oldTimestamp: Long, newTimestamp: Long): Boolean {
        if (newTimestamp < oldTimestamp) return true

        val diff = newTimestamp - oldTimestamp
        return diff > SchedulerUtils.getOutdatedDeviceAndUserTime()
    }

    /**
     * Check to see if the database is encrypted
     *
     * Database assumed to be encrypted if it is not contains required header "SQLite format 3\000"
     * Hex representation: 53 51 4c 69 74 65 20 66 6f 72 6d 61 74 20 33 00
     *
     * This header is ensured by
     * @see <a href="https://www.sqlite.org/fileformat.html">SQLite specification</a>
     *
     * Android uses SQLite 3.** since API 1
     * @see <a href="https://developer.android.com/reference/android/database/sqlite/package-summary.html">Android SQLite package summary</a>
     */
    fun isEncryptedDatabase(context: Context, databaseName: String): Boolean {
        var isEncrypted = false

        val requiredSQLiteFileHeaderString = byteArrayOf(
            0x53,
            0x51,
            0x4c,
            0x69,
            0x74,
            0x65,
            0x20,
            0x66,
            0x6F,
            0x72,
            0x6D,
            0x61,
            0x74,
            0x20,
            0x33,
            0x0
        )
        val dbFileHeader = ByteArray(requiredSQLiteFileHeaderString.size)
        try {
            FileInputStream(context.getDatabasePath(databaseName)).use {
                it.read(dbFileHeader)
                if (!dbFileHeader.contentEquals(requiredSQLiteFileHeaderString)) {
                    isEncrypted = true
                }
            }
        } catch (e: IOException) {
            isEncrypted = true
        }
        return isEncrypted
    }

    fun LifecycleTrackingOptions.toTypeMap(): Map<LifecycleEventType, Boolean> {
        return mapOf(
            LifecycleEventType.APP_LIFECYCLE to appLifecycleEnabled,
            LifecycleEventType.FOREGROUND_LIFECYCLE to foregroundLifecycleEnabled,
            LifecycleEventType.PUSH to pushSubscriptionEnabled,
            LifecycleEventType.SESSION_START to sessionStartEventsEnabled,
            LifecycleEventType.SESSION_END to sessionEndEventsEnabled
        )
    }
}

private const val TAG = "Util"