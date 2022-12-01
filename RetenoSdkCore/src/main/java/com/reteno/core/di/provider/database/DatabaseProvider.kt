package com.reteno.core.di.provider.database

import android.content.Context
import com.reteno.core.data.local.database.RetenoDatabase
import com.reteno.core.data.local.database.RetenoDatabaseImpl
import com.reteno.core.di.base.ProviderWeakReference
import net.sqlcipher.database.SQLiteDatabase

class DatabaseProvider(private val context: Context) :
    ProviderWeakReference<RetenoDatabase>() {

    override fun create(): RetenoDatabase {
        SQLiteDatabase.loadLibs(context)
        return RetenoDatabaseImpl(context)
    }
}