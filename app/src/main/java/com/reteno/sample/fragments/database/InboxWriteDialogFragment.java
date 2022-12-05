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
import com.reteno.core.data.local.model.appinbox.AppInboxMessageDb;
import com.reteno.core.data.local.model.appinbox.AppInboxMessageStatusDb;
import com.reteno.sample.SampleApp;
import com.reteno.sample.databinding.DialogDbWriteInboxBinding;
import com.reteno.sample.util.Util;

public class InboxWriteDialogFragment extends BaseDatabaseDialogFragment {

    private DialogDbWriteInboxBinding binding;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = DialogDbWriteInboxBinding.inflate(getLayoutInflater());
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
        initListeners();
    }

    private void initListeners() {
        binding.btnSubmit.setOnClickListener(v -> {
            String messageId = Util.getTextOrNull(binding.etInteractionId);

            Reteno reteno = ((SampleApp) requireActivity().getApplication()).getRetenoInstance();
            RetenoImpl retenoImpl = ((RetenoImpl) reteno);
            DeviceId deviceIdModel = retenoImpl.getServiceLocator().getConfigRepositoryProvider().get().getDeviceId();
            String deviceId = DeviceIdInternal.INSTANCE.getIdInternal(deviceIdModel);

            if (messageId != null) {
                AppInboxMessageDb inbox = new AppInboxMessageDb(
                        messageId,
                        deviceId,
                        com.reteno.core.util.Util.getCurrentTimeStamp(),
                        AppInboxMessageStatusDb.OPENED
                );

                databaseManager.insertAppInboxMessage(inbox);
                Toast.makeText(this.getContext(), "Save", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
