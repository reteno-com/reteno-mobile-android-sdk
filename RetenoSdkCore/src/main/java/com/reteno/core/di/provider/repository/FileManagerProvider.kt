package com.reteno.core.di.provider.repository

import android.content.Context
import com.reteno.core.data.local.file.FileManager
import com.reteno.core.di.base.ProviderWeakReference

internal class FileManagerProvider(
   private val context: Context
): ProviderWeakReference<FileManager>() {
    override fun create(): FileManager {
        return FileManager(context = context)
    }
}