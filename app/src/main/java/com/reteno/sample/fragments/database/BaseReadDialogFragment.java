package com.reteno.sample.fragments.database;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

public abstract class BaseReadDialogFragment<Model, ViewHolderBinding extends ViewBinding, ViewHolder extends BaseReadViewHolder<Model, ViewHolderBinding>, Adapter extends BaseReadAdapter<Model, ViewHolderBinding, ViewHolder>> extends BaseDatabaseDialogFragment {

    protected com.reteno.sample.databinding.DialogDbReadBinding bindingMain;
    protected Adapter adapter;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        bindingMain = com.reteno.sample.databinding.DialogDbReadBinding.inflate(getLayoutInflater());
        return new AlertDialog.Builder(requireActivity())
                .setView(bindingMain.getRoot())
                .create();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bindingMain = null;
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
