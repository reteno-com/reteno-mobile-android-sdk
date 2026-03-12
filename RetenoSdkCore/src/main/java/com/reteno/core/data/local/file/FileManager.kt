package com.reteno.core.data.local.file

import android.content.Context
import com.reteno.core.R
import com.reteno.core.util.Logger
import com.reteno.core.util.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal class FileManager(private val context: Context) {

    suspend fun saveBaseHtmlContent(content: String) = withContext(Dispatchers.IO) {
        runCatching {
            val inAppDir = File(context.filesDir, "$ROOT_DIR/$INAPP_DIR")
            if (!inAppDir.exists()) inAppDir.mkdirs()
            val baseHtmlFile = File(inAppDir, "inapp_base.html")
            if (!baseHtmlFile.exists()) baseHtmlFile.createNewFile()
            baseHtmlFile.outputStream().use {
                it.write(content.toByteArray())
            }
        }.onFailure {
            Logger.e(TAG, "saveBaseHtmlContent()", it)
        }
    }

    suspend fun getBaseHtmlContent(): String = withContext(Dispatchers.IO) {
        runCatching {
            val htmlFileDir = File(context.filesDir, "$ROOT_DIR/$INAPP_DIR/inapp_base.html")
            htmlFileDir.inputStream().use { it.readBytes().toString(Charsets.UTF_8) }
        }.getOrElse {
            Util.readFromRaw(context, R.raw.base_html).orEmpty()
        }
    }

    companion object {
        private const val TAG = "FileManager"
        private const val ROOT_DIR = "reteno"
        private const val INAPP_DIR = "inapp"
    }
}