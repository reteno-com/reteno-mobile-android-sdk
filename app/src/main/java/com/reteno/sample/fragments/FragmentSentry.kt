package com.reteno.sample.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.reteno.core.RetenoImpl
import com.reteno.sample.BaseFragment
import com.reteno.sample.databinding.FragmentSentryBinding
import java.lang.reflect.InvocationTargetException

class FragmentSentry : BaseFragment() {

    private var binding: FragmentSentryBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSentryBinding.inflate(layoutInflater, container, false)
        return binding!!.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListeners()
    }

    private fun initListeners() {
        binding!!.btnCrashApp.setOnClickListener { throw IllegalArgumentException("This is a test crash from app scope") }
        binding!!.btnCrashSdk.setOnClickListener {
            try {
                val method = RetenoImpl::class.java.getDeclaredMethod("testCrash")
                method.isAccessible = true
                method.invoke(reteno)
                method.isAccessible = false
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            } catch (e: InvocationTargetException) {
                e.printStackTrace()
            } catch (e: NoSuchMethodException) {
                e.printStackTrace()
            }
        }
    }
}
