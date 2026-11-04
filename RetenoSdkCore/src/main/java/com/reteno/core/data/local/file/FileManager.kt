package com.reteno.core.data.local.file

import android.content.Context
import com.reteno.core.R
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.util.Logger
import kotlinx.coroutines.withContext
import java.io.File

internal class FileManager(
    private val context: Context,
    private val dispatchers: RetenoDispatchers
) {

    suspend fun saveBaseHtmlContent(content: String): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            val inAppDir = File(context.filesDir, "$ROOT_DIR/$INAPP_DIR")
            inAppDir.mkdirs()
            File(inAppDir, BASE_HTML_FILE).writeText(content)
        }.onFailure {
            Logger.e(TAG, "saveBaseHtmlContent()", it)
        }
    }

    suspend fun getBaseHtmlContent(): String = withContext(dispatchers.io) {
        runCatching {
            File(context.filesDir, "$ROOT_DIR/$INAPP_DIR/$BASE_HTML_FILE").readText()
        }.getOrElse {
            context.resources.openRawResource(R.raw.base_html).use { it.readBytes().toString(Charsets.UTF_8) }
        }
    }

    companion object {
        private const val TAG = "FileManager"
        private const val ROOT_DIR = "reteno"
        private const val INAPP_DIR = "inapp"
        private const val BASE_HTML_FILE = "inapp_base.html"
    }
}
