package com.reteno.sample.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.reteno.core.domain.model.event.LifecycleTrackingOptions
import com.reteno.sample.BaseFragment
import com.reteno.sample.databinding.FragmentAppLifecycleEventsBinding
import com.reteno.sample.util.AppSharedPreferencesManager

class FragmentAppLifecycleEvents : BaseFragment() {

    private var binding: FragmentAppLifecycleEventsBinding? = null
    private var config = LifecycleTrackingOptions.DEFAULT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        config = AppSharedPreferencesManager.getOptions(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentAppLifecycleEventsBinding.inflate(inflater, container, false).run {
            binding = this
            root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.apply {
            itemAppLifecycle.tvTitle.text = "AppLifecycle"
            itemAppLifecycle.cbEnabled.isChecked = config.appLifecycleEnabled
            itemAppLifecycle.cbEnabled.setOnCheckedChangeListener { _, b ->
                config = config.copy(appLifecycleEnabled = b)
            }
            itemPush.tvTitle.text = "PushSubsription"
            itemPush.cbEnabled.isChecked = config.pushSubscriptionEnabled
            itemPush.cbEnabled.setOnCheckedChangeListener { _, b ->
                config = config.copy(pushSubscriptionEnabled = b)
            }
            itemForegroundLifecycle.tvTitle.text = "ForegroundLifecycle"
            itemForegroundLifecycle.cbEnabled.isChecked = config.foregroundLifecycleEnabled
            itemForegroundLifecycle.cbEnabled.setOnCheckedChangeListener { _, b ->
                config = config.copy(foregroundLifecycleEnabled = b)
            }
            itemSessionStart.tvTitle.text = "Session start"
            itemSessionStart.cbEnabled.isChecked = config.sessionStartEventsEnabled
            itemSessionStart.cbEnabled.setOnCheckedChangeListener { _, b ->
                config = config.copy(sessionStartEventsEnabled = b)
            }
            itemSessionEnd.tvTitle.text = "Session end"
            itemSessionEnd.cbEnabled.isChecked = config.sessionEndEventsEnabled
            itemSessionEnd.cbEnabled.setOnCheckedChangeListener { _, b ->
                config = config.copy(sessionEndEventsEnabled = b)
            }
            btnSave.setOnClickListener {
                reteno.setLifecycleEventConfig(config)
                AppSharedPreferencesManager.saveOptions(requireContext(), config)
                Toast.makeText(requireContext(), "SAVED", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}