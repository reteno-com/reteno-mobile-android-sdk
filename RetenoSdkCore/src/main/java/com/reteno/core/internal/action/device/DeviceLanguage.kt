package com.reteno.core.internal.action.device

import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.platform.AppEnvironment
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Locale

internal interface DeviceLanguage {
    suspend fun resolve(trace: ActionTrace): String

    suspend fun settle(trace: ActionTrace, displayed: Locale)

    suspend fun useExplicit(trace: ActionTrace, languageCode: String)

    suspend fun useDetected(trace: ActionTrace): Boolean
}

internal data class DetectedLanguage(
    val languageCode: String,
    val systemLanguageCode: String?,
    val source: String?
)

internal class DeviceLanguageResolver(
    private val prefs: SharedPrefsManager,
    private val environment: AppEnvironment,
    private val dispatchers: RetenoDispatchers
) : DeviceLanguage {

    private val detection = Mutex()

    override suspend fun resolve(trace: ActionTrace): String {
        val resolved = trace.step<ResolvedLanguage>("resolve device language", detail = { "source=${it.source}" }) {
            detection.withLock { resolveNow() }
        }
        return resolved.languageCode
    }

    override suspend fun settle(trace: ActionTrace, displayed: Locale) {
        trace.step<DetectedLanguage>(
            "detect device language from the settled screen",
            detail = { "source=${it.source}" }
        ) {
            detection.withLock { settleNow(displayed.stripExtensions()) }
        }
    }

    override suspend fun useExplicit(trace: ActionTrace, languageCode: String) {
        trace.step("save explicit device language to local settings") {
            withContext(dispatchers.io) { prefs.saveExplicitDeviceLanguage(languageCode) }
        }
    }

    override suspend fun useDetected(trace: ActionTrace): Boolean = trace.step(
        "forget explicit device language in local settings",
        detail = { forgotten -> if (forgotten) "forgotten" else "none set" }
    ) {
        withContext(dispatchers.io) {
            val hadExplicit = prefs.getExplicitDeviceLanguage() != null
            if (hadExplicit) prefs.saveExplicitDeviceLanguage(null)
            hadExplicit
        }
    }

    private suspend fun settleNow(displayed: Locale): DetectedLanguage {
        val stored = withContext(dispatchers.io) { prefs.getDetectedDeviceLanguage() }
        val detected = detectOnScreen(displayed, stored)
        if (detected != stored) {
            withContext(dispatchers.io) { prefs.saveDetectedDeviceLanguage(detected) }
        }
        return detected
    }

    private suspend fun resolveNow(): ResolvedLanguage {
        val explicit = withContext(dispatchers.io) { prefs.getExplicitDeviceLanguage() }
        if (explicit != null) return ResolvedLanguage(explicit, SOURCE_EXPLICIT)
        val stored = withContext(dispatchers.io) { prefs.getDetectedDeviceLanguage() }
        if (stored != null) return ResolvedLanguage(stored.languageCode, SOURCE_STORED)
        val detected = detectWithoutScreen()
        withContext(dispatchers.io) { prefs.saveDetectedDeviceLanguage(detected) }
        return ResolvedLanguage(detected.languageCode, detected.source)
    }

    private fun detectWithoutScreen(): DetectedLanguage {
        val system = environment.systemLocale().stripExtensions()
        val app = appChosenLocale(system)
        if (app != null) return detection(app, system, SOURCE_APP)
        return detection(defaultOrSystem(system), system, SOURCE_PROVISIONAL)
    }

    private fun detectOnScreen(displayed: Locale, stored: DetectedLanguage?): DetectedLanguage {
        val system = environment.systemLocale().stripExtensions()
        val systemTag = system.toLanguageTag()
        val app = appChosenLocale(system)
        if (app != null) return detection(app, system, SOURCE_APP)
        if (stored != null && stored.isKeptOn(systemTag, displayed)) return stored
        if (displayed.toLanguageTag() != systemTag) return detection(displayed, system, SOURCE_ACTIVITY)
        return detection(defaultOrSystem(system), system, SOURCE_DEFAULT)
    }

    private fun DetectedLanguage.isKeptOn(systemTag: String, displayed: Locale): Boolean =
        systemLanguageCode == systemTag && displayed.toLanguageTag() == systemTag && source == SOURCE_DEFAULT

    private fun appChosenLocale(system: Locale): Locale? {
        val app = environment.appLocale().stripExtensions()
        return if (app.toLanguageTag() != system.toLanguageTag()) app else null
    }

    private fun defaultOrSystem(system: Locale): Locale {
        val default = environment.defaultLocale().stripExtensions()
        return if (default.toLanguageTag() != system.toLanguageTag()) default else system
    }

    private fun detection(locale: Locale, system: Locale, source: String) =
        DetectedLanguage(withRegion(locale, system), system.toLanguageTag(), source)

    private fun withRegion(locale: Locale, system: Locale): String {
        if (locale.country.isNotEmpty()) return locale.toLanguageTag()
        val systemRegion = if (locale.language == system.language) system.country else ""
        val region = systemRegion.ifEmpty { environment.likelyRegion(locale.language) }
        if (region.isEmpty()) return locale.toLanguageTag()
        return Locale.Builder()
            .setLanguage(locale.language)
            .setScript(locale.script)
            .setRegion(region)
            .build()
            .toLanguageTag()
    }

    private class ResolvedLanguage(val languageCode: String, val source: String?)

    private companion object {
        const val SOURCE_EXPLICIT = "explicit"
        const val SOURCE_STORED = "stored"
        const val SOURCE_APP = "app"
        const val SOURCE_ACTIVITY = "activity"
        const val SOURCE_DEFAULT = "default"
        const val SOURCE_PROVISIONAL = "provisional"
    }
}
