package com.reteno.sample.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.reteno.sample.BaseFragment
import com.reteno.sample.databinding.FragmentErrorReportingBinding
import com.reteno.sample.util.SdkTestError

class FragmentErrorReporting : BaseFragment() {

    private var binding: FragmentErrorReportingBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentErrorReportingBinding.inflate(layoutInflater, container, false)
        return binding!!.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding!!.btnCrashApp.setOnClickListener {
            throw IllegalArgumentException("This is a test crash from app scope")
        }
        binding!!.btnCrashSdk.isEnabled = SdkTestError.isAvailable
        binding!!.btnCrashSdk.setOnClickListener {
            SdkTestError.log()
            Toast.makeText(requireContext(), "SDK test error logged", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
