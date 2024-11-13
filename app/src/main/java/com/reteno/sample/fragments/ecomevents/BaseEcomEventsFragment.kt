package com.reteno.sample.fragments.ecomevents

import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import androidx.viewbinding.ViewBinding
import com.reteno.core.domain.model.ecom.Attributes
import com.reteno.sample.BaseFragment
import com.reteno.sample.R
import com.reteno.sample.util.Util
import java.util.Arrays

open class BaseEcomEventsFragment : BaseFragment() {
    protected fun createNewFields(viewBinding: ViewBinding, container: LinearLayout?): View {
        return LayoutInflater
            .from(viewBinding.root.context)
            .inflate(R.layout.view_user_custom_fields_horizontal, container, false)
    }

    protected fun getCustomAttributes(llCustomAttributes: LinearLayout): List<Attributes>? {
        val countView = llCustomAttributes.childCount
        if (countView == 0) return null
        val list: MutableList<Attributes> = ArrayList()
        for (i in 0 until countView) {
            val parent = llCustomAttributes.getChildAt(i) as LinearLayout
            val etKey = parent.getChildAt(0) as EditText
            val etValue = parent.getChildAt(1) as EditText
            val key = Util.getTextOrNull(etKey)
            val value = Util.getTextOrNull(etValue)
            if (key != null && value != null) {
                val valueList =
                    listOf(*value.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray())
                list.add(Attributes(key, valueList))
            }
        }
        return list
    }
}
