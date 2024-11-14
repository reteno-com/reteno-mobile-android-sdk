package com.reteno.sample.fragments.database

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.reteno.core.Reteno
import com.reteno.core.RetenoImpl
import com.reteno.core.di.ServiceLocator
import com.reteno.sample.SampleApp
import kotlin.properties.Delegates

open class BaseDatabaseDialogFragment : DialogFragment() {

    private var serviceLocatorInternal: ServiceLocator? = null
    protected val serviceLocator: ServiceLocator
        get() = requireNotNull(serviceLocatorInternal)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val field = RetenoImpl::class.java.getDeclaredField("serviceLocator")
            field.isAccessible = true
            serviceLocatorInternal = field[RetenoImpl.instance] as ServiceLocator
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
