package com.reteno.sample.fragments

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.reteno.sample.BaseFragment
import com.reteno.sample.databinding.FragmentDeviceIdBinding
import com.reteno.sample.util.AppSharedPreferencesManager

class FragmentDeviceId : BaseFragment() {

    private var binding: FragmentDeviceIdBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDeviceIdBinding.inflate(layoutInflater, container, false)
        return binding!!.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initExternalDeviceId(view)
        refreshUi()
        initListeners(view)
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun initExternalDeviceId(view: View) {
        val externalSavedId = AppSharedPreferencesManager.getExternalId(view.context)
        if (!TextUtils.isEmpty(externalSavedId)) {
            reteno.setUserAttributes(externalSavedId)
        }
    }

    private fun refreshUi() {
        val context = requireContext()
        binding!!.etDeviceId.setText(AppSharedPreferencesManager.getDeviceId(context))
        binding!!.etDeviceIdMillis.setText(AppSharedPreferencesManager.getDeviceIdDelay(context).toString())
        binding!!.tvExternalId.text = AppSharedPreferencesManager.getExternalId(context)
    }

    private fun initListeners(view: View) {
        binding!!.rootView.setOnRefreshListener {
            refreshUi()
            binding!!.rootView.isRefreshing = false
        }
        binding!!.tilExternalId.setStartIconOnClickListener {
            val externalId = binding!!.etExternalId.getText().toString()
            AppSharedPreferencesManager.saveExternalId(view.context, externalId)
            reteno.setUserAttributes(externalId)
            refreshUi()
        }
        binding!!.tilExternalId.setEndIconOnClickListener {
            AppSharedPreferencesManager.saveExternalId(view.context, "")
            reteno.setUserAttributes("")
            binding!!.etExternalId.setText("")
            refreshUi()
        }
        binding!!.tilDeviceId.setStartIconOnClickListener {
            val deviceId = binding!!.etDeviceId.getText().toString()
            AppSharedPreferencesManager.saveDeviceId(view.context, deviceId)
            refreshUi()
        }
        binding!!.tilDeviceId.setEndIconOnClickListener {
            AppSharedPreferencesManager.saveDeviceId(view.context, "")
            binding!!.etDeviceId.setText("")
            refreshUi()
        }
        binding!!.tilDeviceIdFetchDelay.setStartIconOnClickListener {
            val deviceIdMillis = binding!!.etDeviceIdMillis.getText().toString()
            AppSharedPreferencesManager.saveDeviceIdDelay(view.context, deviceIdMillis.toIntOrNull() ?: 0)
            refreshUi()
        }
        binding!!.tilDeviceIdFetchDelay.setEndIconOnClickListener {
            AppSharedPreferencesManager.saveDeviceIdDelay(view.context, 0)
            refreshUi()
        }
    }
}
