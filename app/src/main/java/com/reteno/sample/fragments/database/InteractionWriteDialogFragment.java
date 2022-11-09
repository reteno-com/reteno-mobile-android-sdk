package com.reteno.sample.fragments.database;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reteno.core.data.local.model.InteractionModelDb;
import com.reteno.core.model.interaction.InteractionStatus;
import com.reteno.core.util.UtilKt;
import com.reteno.sample.databinding.DialogDbWriteInteractionBinding;
import com.reteno.sample.util.Util;

public class InteractionWriteDialogFragment extends BaseDatabaseDialogFragment {

    private DialogDbWriteInteractionBinding binding;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = DialogDbWriteInteractionBinding.inflate(getLayoutInflater());
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
            String interactionId = Util.getTextOrNull(binding.etInteractionId);
            String status = Util.getTextOrNull(binding.etStatus);
            String time = Util.getTextOrNull(binding.etTime);
            String token = Util.getTextOrNull(binding.etToken);

            if (UtilKt.allElementsNotNull(interactionId, status, time, token)) {
                InteractionModelDb interaction = new InteractionModelDb(
                        interactionId,
                        InteractionStatus.fromString(status),
                        time,
                        token
                );
                databaseManager.insertInteraction(interaction);
                Toast.makeText(this.getContext(), "Sent", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
