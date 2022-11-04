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
import androidx.viewbinding.ViewBinding;

import com.reteno.core.data.local.database.RetenoDatabaseManagerImpl;

public abstract class BaseReadDialogFragment<Model, ViewHolderBinding extends ViewBinding, ViewHolder extends BaseReadViewHolder<Model, ViewHolderBinding>, Adapter extends BaseReadAdapter<Model, ViewHolderBinding, ViewHolder>> extends DialogFragment {

    protected com.reteno.sample.databinding.DialogDbReadBinding bindingMain;
    protected RetenoDatabaseManagerImpl databaseManager;
    protected Adapter adapter;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        bindingMain = com.reteno.sample.databinding.DialogDbReadBinding.inflate(getLayoutInflater());
        return new AlertDialog.Builder(requireActivity())
                .setView(bindingMain.getRoot())
                .create();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        databaseManager = new RetenoDatabaseManagerImpl();
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bindingMain = null;
        databaseManager = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        initUi();
    }

    private void initUi() {
        initAdapter();
        initRecycler();
        initItems();
        initCount();
        initRemoveSection();
    }

    protected abstract void initAdapter();

    protected void initRecycler() {
        bindingMain.rvList.setAdapter(adapter);
    }

    protected abstract void initItems();

    protected abstract void initCount();

    private void initRemoveSection() {
        bindingMain.npRemoveEntries.setWrapSelectorWheel(false);
        bindingMain.npRemoveEntries.setMinValue(1);
        bindingMain.npRemoveEntries.setMaxValue(Integer.MAX_VALUE);
        bindingMain.btnRemoveEntries.setOnClickListener(v -> {
            int count = bindingMain.npRemoveEntries.getValue();
            deleteItems(count);

            initCount();
            initItems();
        });
    }

    protected abstract void deleteItems(int count);
}
