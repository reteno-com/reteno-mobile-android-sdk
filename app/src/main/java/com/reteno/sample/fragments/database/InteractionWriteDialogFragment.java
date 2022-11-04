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

import com.reteno.core.data.local.database.RetenoDatabaseManagerImpl;
import com.reteno.core.data.local.model.InteractionModelDb;
import com.reteno.core.model.interaction.InteractionStatus;
import com.reteno.core.util.UtilKt;
import com.reteno.sample.databinding.DialogDbWriteInteractionBinding;
import com.reteno.sample.util.Util;

public class InteractionWriteDialogFragment extends DialogFragment {

    private DialogDbWriteInteractionBinding binding;
    private RetenoDatabaseManagerImpl databaseManager;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = DialogDbWriteInteractionBinding.inflate(getLayoutInflater());
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
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        databaseManager = null;
    }
}
