package com.reteno.sample.fragments.database

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.reteno.core.RetenoInternalImpl
import com.reteno.core.di.ServiceLocator

open class BaseDatabaseDialogFragment : DialogFragment() {

    private var serviceLocatorInternal: ServiceLocator? = null
    protected val serviceLocator: ServiceLocator
        get() = requireNotNull(serviceLocatorInternal)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val field = RetenoInternalImpl::class.java.getDeclaredField("serviceLocator")
            field.isAccessible = true
            serviceLocatorInternal = field[RetenoInternalImpl.instance] as ServiceLocator
            field.isAccessible = false
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        serviceLocatorInternal = null
        super.onDestroy()
    }
}
