package com.reteno.sample.fragments.database

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.reteno.core.RetenoInternalImpl
import com.reteno.core._interop.DeviceIdInternal.getExternalIdInternal
import com.reteno.core._interop.DeviceIdInternal.getIdInternal
import com.reteno.core.data.local.config.DeviceId
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerDevice
import com.reteno.core.data.local.model.BooleanDb
import com.reteno.core.data.local.model.device.DeviceCategoryDb
import com.reteno.core.data.local.model.device.DeviceDb
import com.reteno.core.data.local.model.device.DeviceOsDb
import com.reteno.core.domain.model.device.Device.Companion.createDevice
import com.reteno.sample.databinding.DialogDbWriteDeviceBinding
import com.reteno.sample.util.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DeviceWriteDialogFragment : BaseDatabaseDialogFragment() {

    private var binding: DialogDbWriteDeviceBinding? = null
    private var databaseManager: RetenoDatabaseManagerDevice? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        databaseManager = serviceLocator.retenoDatabaseManagerDeviceProvider.get()
    }

    override fun onDestroy() {
        super.onDestroy()
        databaseManager = null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogDbWriteDeviceBinding.inflate(layoutInflater)
        return AlertDialog.Builder(requireActivity())
            .setView(binding!!.getRoot())
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onStart() {
        super.onStart()
        initUi()
        initListeners()
    }

    private fun initUi() {
        val retenoImpl = RetenoInternalImpl.instance
        val deviceId = retenoImpl.serviceLocator.configRepositoryProvider.get().getDeviceId()
        lifecycleScope.launch {
            val token = withContext(Dispatchers.IO) {
                retenoImpl.serviceLocator.configRepositoryProvider.get().getFcmToken()
            }
            initUiFromToken(deviceId, token)
        }
    }

    private fun initUiFromToken(
        deviceId: DeviceId,
        token: String?
    ) {
        val device = createDevice(
            deviceId.getIdInternal(),
            deviceId.getExternalIdInternal(),
            token,
            null,
            null,
            null,
            null
        )
        binding!!.etDeviceId.setText(device.deviceId)
        binding!!.etExternalUserId.setText(device.externalUserId)
        binding!!.etPushToken.setText(device.pushToken)
        binding!!.etCategory.setText(device.category.toString())
        binding!!.etOsType.setText(device.osType.toString())
        binding!!.etOsVersion.setText(device.osVersion)
        binding!!.etDeviceModel.setText(device.deviceModel)
        binding!!.etAppVersion.setText(device.appVersion)
        binding!!.etLanguageCode.setText(device.languageCode)
        binding!!.etTimeZone.setText(device.timeZone)
        binding!!.etAdvertisingId.setText(device.advertisingId)
        binding!!.etPhone.setText(device.phone)
        binding!!.etEmail.setText(device.email)
    }

    private fun initListeners() {
        binding!!.btnSubmit.setOnClickListener { v: View? ->
            val pushSubscribedString = binding!!.etPushSubscribed.getText().toString()
            var pushSubscribed: BooleanDb? = null
            when (pushSubscribedString) {
                "TRUE", "true" -> pushSubscribed = BooleanDb.TRUE
                "FALSE", "false" -> pushSubscribed = BooleanDb.FALSE
            }
            val finalPushSubscribed = pushSubscribed
            val device = DeviceDb(
                null,
                0L,
                binding!!.etDeviceId.getText().toString(),
                Util.getTextOrNull(binding!!.etExternalUserId),
                Util.getTextOrNull(binding!!.etPushToken),
                finalPushSubscribed,
                DeviceCategoryDb.fromString(Util.getTextOrNull(binding!!.etCategory)),
                DeviceOsDb.fromString(Util.getTextOrNull(binding!!.etOsType)),
                Util.getTextOrNull(binding!!.etOsVersion),
                Util.getTextOrNull(binding!!.etDeviceModel),
                Util.getTextOrNull(binding!!.etAppVersion),
                Util.getTextOrNull(binding!!.etLanguageCode),
                Util.getTextOrNull(binding!!.etTimeZone),
                Util.getTextOrNull(binding!!.etAdvertisingId),
                null,
                Util.getTextOrNull(binding!!.etEmail),
                Util.getTextOrNull(binding!!.etPhone)
            )
            databaseManager!!.insertDevice(device)
            Toast.makeText(this.context, "Sent", Toast.LENGTH_SHORT).show()
        }
    }
}
