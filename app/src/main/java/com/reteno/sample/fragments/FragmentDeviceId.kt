package com.reteno.sample.fragments

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.reteno.core.RetenoInternalImpl
import com.reteno.core._interop.DeviceIdInternal.getExternalIdInternal
import com.reteno.core._interop.DeviceIdInternal.getIdInternal
import com.reteno.core._interop.DeviceIdInternal.getModeInternal
import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.di.ServiceLocator
import com.reteno.sample.BaseFragment
import com.reteno.sample.databinding.FragmentDeviceIdBinding
import com.reteno.sample.util.AppSharedPreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentDeviceId : BaseFragment() {

    private var binding: FragmentDeviceIdBinding? = null
    private var serviceLocator: ServiceLocator? = null
    private var configRepository: ConfigRepository? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val field = RetenoInternalImpl::class.java.getDeclaredField("serviceLocator")
            field.isAccessible = true
            serviceLocator = field[reteno] as ServiceLocator
            field.isAccessible = false
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
        configRepository = serviceLocator!!.configRepositoryProvider.get()
    }

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

    private fun initExternalDeviceId(view: View) {
        val externalSavedId = AppSharedPreferencesManager.getExternalId(view.context)
        if (!TextUtils.isEmpty(externalSavedId)) {
            reteno.setUserAttributes(externalSavedId)
        }
    }

    private fun refreshUi() {
        val id = configRepository!!.getDeviceId().getIdInternal()
        val externalId = configRepository!!.getDeviceId().getExternalIdInternal()
        val mode = configRepository!!.getDeviceId().getModeInternal()
        binding!!.tvCurrentDeviceIdMode.text = mode.toString()
        binding!!.tvCurrentDeviceId.text = id
        binding!!.etDeviceId.setText(AppSharedPreferencesManager.getDeviceId(requireContext()))
        binding!!.etDeviceIdMillis.setText(
            AppSharedPreferencesManager.getDeviceIdDelay(
                requireContext()
            ).toString()
        )
        binding!!.tvExternalId.text = externalId
        lifecycleScope.launch {
            val token = withContext(Dispatchers.IO) {
                configRepository!!.getFcmToken()
            }
            binding!!.etFcmToken.setText(token)
        }
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
            AppSharedPreferencesManager.saveDeviceIdDelay(view.context, deviceIdMillis.toInt())
            refreshUi()
        }
        binding!!.tilDeviceIdFetchDelay.setEndIconOnClickListener {
            AppSharedPreferencesManager.saveDeviceIdDelay(view.context, 0)
            binding!!.etDeviceId.setText("")
            refreshUi()
        }
    }
}