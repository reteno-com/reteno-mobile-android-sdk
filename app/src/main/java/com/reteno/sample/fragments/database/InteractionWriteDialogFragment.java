package com.reteno.sample.fragments.database;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInteraction;
import com.reteno.core.data.local.model.interaction.InteractionDb;
import com.reteno.core.data.local.model.interaction.InteractionStatusDb;
import com.reteno.core.util.UtilKt;
import com.reteno.sample.databinding.DialogDbWriteInteractionBinding;
import com.reteno.sample.util.Util;

public class InteractionWriteDialogFragment extends BaseDatabaseDialogFragment {

    private DialogDbWriteInteractionBinding binding;
    private RetenoDatabaseManagerInteraction databaseManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        databaseManager = serviceLocator.getRetenoDatabaseManagerInteractionProvider().get();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        databaseManager = serviceLocator.getRetenoDatabaseManagerInteractionProvider().get();
    }

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
                InteractionDb interaction = new InteractionDb(
                        interactionId,
                        InteractionStatusDb.fromString(status),
                        time,
                        token
                );
                databaseManager.insertInteraction(interaction);
                Toast.makeText(this.getContext(), "Sent", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
