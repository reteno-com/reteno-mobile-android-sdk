package com.reteno.core.internal.action.iam

import com.reteno.core.data.local.file.FileManager
import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.HttpMethod
import com.reteno.core.data.remote.api.RestClientImpl.Companion.HEADER_X_AMZ_META_VERSION
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.network.ApiResult
import com.reteno.core.internal.network.SuspendApiClient
import kotlinx.coroutines.withContext

internal interface PreloadIamHtml {
    suspend operator fun invoke(): Result<Unit>
}

internal class PreloadIamHtmlAction(
    private val api: SuspendApiClient,
    private val prefs: SharedPrefsManager,
    private val files: FileManager,
    private val dispatchers: RetenoDispatchers,
    private val runner: ActionRunner
) : PreloadIamHtml {

    override suspend operator fun invoke(): Result<Unit> = runner("PreloadIamHtml") {
        preload()
    }

    private suspend fun ActionTrace.preload() {
        moveHtmlFromSettingsToFile()
        val address = step("read base html address from local settings") {
            withContext(dispatchers.io) { prefs.getIamBaseUrl() }
        }
        val contract = if (address == null) ApiContract.InAppMessages.BaseHtml else ApiContract.Custom(address)
        val serverVersion = readServerVersion(contract)
        if (serverVersion == null) {
            note { "base html version unknown, keeping current html" }
            return
        }
        val localVersion = step("read base html version from local settings") {
            withContext(dispatchers.io) { prefs.getIamBaseHtmlVersion() }
        } ?: DEFAULT_VERSION
        if (serverVersion == localVersion) {
            note { "base html is up to date" }
            return
        }
        val response = step("load base html from server") {
            api.request(HttpMethod.GET, contract)
        }
        if (response is ApiResult.Failure) {
            note { "base html not updated, retrying on next preload" }
            return
        }
        val written = step("save base html to file") {
            files.saveBaseHtmlContent((response as ApiResult.Success).body)
        }
        if (written.isFailure) {
            note { "base html file not written, retrying on next preload" }
            return
        }
        step("save base html version to local settings") {
            withContext(dispatchers.io) { prefs.saveIamBaseHtmlVersion(serverVersion) }
        }
    }

    private suspend fun ActionTrace.moveHtmlFromSettingsToFile() {
        val html = step("read base html from local settings") {
            withContext(dispatchers.io) { prefs.getIamBaseHtmlContent() }
        }
        if (html.isBlank()) return
        step("move base html from local settings to file") {
            files.saveBaseHtmlContent(html)
            withContext(dispatchers.io) { prefs.clearBaseHtmlContent() }
        }
    }

    private suspend fun ActionTrace.readServerVersion(contract: ApiContract): String? {
        val response = step("load base html version from server") {
            api.request(HttpMethod.HEAD, contract)
        }
        if (response is ApiResult.Failure) return null
        return (response as ApiResult.Success).headers[HEADER_X_AMZ_META_VERSION]?.firstOrNull() ?: DEFAULT_VERSION
    }

    private companion object {
        const val DEFAULT_VERSION = "20250307-1527-f34d4c8"
    }
}
