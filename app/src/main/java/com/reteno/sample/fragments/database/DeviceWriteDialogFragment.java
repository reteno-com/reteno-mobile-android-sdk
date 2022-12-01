package com.reteno.sample.fragments.database;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reteno.core.Reteno;
import com.reteno.core.RetenoImpl;
import com.reteno.core._interop.DeviceIdInternal;
import com.reteno.core.data.local.config.DeviceId;
import com.reteno.core.data.local.model.BooleanDb;
import com.reteno.core.data.local.model.device.DeviceCategoryDb;
import com.reteno.core.data.local.model.device.DeviceDb;
import com.reteno.core.data.local.model.device.DeviceOsDb;
import com.reteno.core.domain.model.device.Device;
import com.reteno.sample.SampleApp;
import com.reteno.sample.databinding.DialogDbWriteDeviceBinding;
import com.reteno.sample.util.Util;

public class DeviceWriteDialogFragment extends BaseDatabaseDialogFragment {

    private DialogDbWriteDeviceBinding binding;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = DialogDbWriteDeviceBinding.inflate(getLayoutInflater());
        return new AlertDialog.Builder(requireActivity())
                .setView(binding.getRoot())
                .create();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        initUi();
        initListeners();
    }

    private void initUi() {
        Reteno reteno = ((SampleApp) requireActivity().getApplication()).getRetenoInstance();
        RetenoImpl retenoImpl = ((RetenoImpl) reteno);
        DeviceId deviceId = retenoImpl.getServiceLocator().getConfigRepositoryProvider().get().getDeviceId();
        String pushToken = retenoImpl.getServiceLocator().getConfigRepositoryProvider().get().getFcmToken();
        Device device = Device.createDevice(DeviceIdInternal.INSTANCE.getIdInternal(deviceId),
                DeviceIdInternal.INSTANCE.getExternalIdInternal(deviceId),
                pushToken,
                null,
                null
        );

        binding.etDeviceId.setText(device.getDeviceId());
        binding.etExternalUserId.setText(device.getExternalUserId());
        binding.etPushToken.setText(device.getPushToken());
        binding.etCategory.setText(device.getCategory().toString());
        binding.etOsType.setText(device.getOsType().toString());
        binding.etOsVersion.setText(device.getOsVersion());
        binding.etDeviceModel.setText(device.getDeviceModel());
        binding.etAppVersion.setText(device.getAppVersion());
        binding.etLanguageCode.setText(device.getLanguageCode());
        binding.etTimeZone.setText(device.getTimeZone());
        binding.etAdvertisingId.setText(device.getAdvertisingId());
    }

    private void initListeners() {
        binding.btnSubmit.setOnClickListener(v -> {
            String pushSubscribedString = binding.etPushSubscribed.getText().toString();
            BooleanDb pushSubscribed = null;
            switch (pushSubscribedString) {
                case "TRUE":
                case "true":
                    pushSubscribed = BooleanDb.TRUE;
                    break;
                case "FALSE":
                case "false":
                    pushSubscribed = BooleanDb.FALSE;
                    break;
            }
            BooleanDb finalPushSubscribed = pushSubscribed;

            DeviceDb device = new DeviceDb(
                    binding.etDeviceId.getText().toString(),
                    Util.getTextOrNull(binding.etExternalUserId),
                    Util.getTextOrNull(binding.etPushToken),
                    finalPushSubscribed,
                    DeviceCategoryDb.Companion.fromString(Util.getTextOrNull(binding.etCategory)),
                    DeviceOsDb.Companion.fromString(Util.getTextOrNull(binding.etOsType)),
                    Util.getTextOrNull(binding.etOsVersion),
                    Util.getTextOrNull(binding.etDeviceModel),
                    Util.getTextOrNull(binding.etAppVersion),
                    Util.getTextOrNull(binding.etLanguageCode),
                    Util.getTextOrNull(binding.etTimeZone),
                    Util.getTextOrNull(binding.etAdvertisingId)
            );

            databaseManager.insertDevice(device);
            Toast.makeText(this.getContext(), "Sent", Toast.LENGTH_SHORT).show();
        });
    }
}
