package com.reteno.core

import android.app.Application
import android.content.Context
import androidx.startup.Initializer

open class RetenoInitializer : Initializer<Reteno> {
    override fun create(context: Context): Reteno {
        Reteno.create(context.applicationContext as Application)
        return Reteno.instance
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}