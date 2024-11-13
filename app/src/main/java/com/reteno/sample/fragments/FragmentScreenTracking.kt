package com.reteno.sample.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.reteno.core.lifecycle.ScreenTrackingConfig
import com.reteno.core.lifecycle.ScreenTrackingTrigger
import com.reteno.sample.BaseFragment
import com.reteno.sample.databinding.FragmentScreenTrackingBinding
import java.util.Arrays

class FragmentScreenTracking : BaseFragment() {

    private var binding: FragmentScreenTrackingBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentScreenTrackingBinding.inflate(layoutInflater, container, false)
        return binding!!.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListeners()
        binding!!.spnTrigger.setAdapter(
            ArrayAdapter(
                view.context,
                android.R.layout.simple_spinner_item,
                ScreenTrackingTrigger.values()
            )
        )
        binding!!.spnTrigger.setSelection(0)
    }

    private fun initListeners() {
        binding!!.btnTrackScreen.setOnClickListener {
            reteno.logScreenView(
                binding!!.etScreenName.getText().toString()
            )
        }
        binding!!.btnUpdateAutoScreenTrackingConfig.setOnClickListener {
            val excludeScreens = listOf(
                *binding!!.etExcludeScreens.getText().toString().split("\\s*,\\s*".toRegex())
                    .dropLastWhile { it.isEmpty() }
                    .toTypedArray())
            val trigger = ScreenTrackingTrigger.values()[binding!!.spnTrigger.selectedItemPosition]
            val config = ScreenTrackingConfig(
                binding!!.cbEnableScreenTracking.isChecked,
                excludeScreens,
                trigger
            )
            reteno.autoScreenTracking(config)
        }
    }
}
