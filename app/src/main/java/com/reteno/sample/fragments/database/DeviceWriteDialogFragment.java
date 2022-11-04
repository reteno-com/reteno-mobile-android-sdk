package com.reteno.sample.fragments.database;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.reteno.core.Reteno;
import com.reteno.core.RetenoImpl;
import com.reteno.core._interop.DeviceIdInternal;
import com.reteno.core.data.local.config.DeviceId;
import com.reteno.core.data.local.database.RetenoDatabaseManagerImpl;
import com.reteno.core.model.device.Device;
import com.reteno.core.model.device.DeviceCategory;
import com.reteno.core.model.device.DeviceOS;
import com.reteno.sample.SampleApp;
import com.reteno.sample.databinding.DialogDbWriteDeviceBinding;
import com.reteno.sample.util.Util;

public class DeviceWriteDialogFragment extends DialogFragment {

    private DialogDbWriteDeviceBinding binding;
    private RetenoDatabaseManagerImpl databaseManager;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = DialogDbWriteDeviceBinding.inflate(getLayoutInflater());
        return new AlertDialog.Builder(requireActivity())
                .setView(binding.getRoot())
                .create();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        databaseManager = new RetenoDatabaseManagerImpl();
        return super.onCreateView(inflater, container, savedInstanceState);
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
            Device device = new Device(
                    binding.etDeviceId.getText().toString(),
                    Util.getTextOrNull(binding.etExternalUserId),
                    Util.getTextOrNull(binding.etPushToken),
                    DeviceCategory.Companion.fromString(Util.getTextOrNull(binding.etCategory)),
                    DeviceOS.Companion.fromString(Util.getTextOrNull(binding.etOsType)),
                    Util.getTextOrNull(binding.etOsVersion),
                    Util.getTextOrNull(binding.etDeviceModel),
                    Util.getTextOrNull(binding.etAppVersion),
                    Util.getTextOrNull(binding.etLanguageCode),
                    Util.getTextOrNull(binding.etTimeZone),
                    Util.getTextOrNull(binding.etAdvertisingId)
            );

            databaseManager.insertDevice(device);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        databaseManager = null;
    }
}
