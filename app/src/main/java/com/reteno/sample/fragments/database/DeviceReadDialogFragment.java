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
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import com.reteno.core.data.local.database.RetenoDatabaseManagerImpl;
import com.reteno.core.model.device.Device;
import com.reteno.sample.R;
import com.reteno.sample.databinding.DialogDbReadBinding;
import com.reteno.sample.databinding.ItemDbDeviceBinding;

import java.util.List;
import java.util.Locale;

import kotlin.Pair;


public class DeviceReadDialogFragment extends DialogFragment {

    private DialogDbReadBinding bindingMain;
    private RetenoDatabaseManagerImpl databaseManager;
    private DeviceAdapter adapter;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        bindingMain = DialogDbReadBinding.inflate(getLayoutInflater());
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
    public void onStart() {
        super.onStart();
        initUi();
    }

    private void initUi() {
        initRecycler();
        initRemoveSection();
        initCount();
        initDeviceItems();
    }

    private void initRecycler() {
        adapter = new DeviceAdapter(() ->
                TransitionManager.beginDelayedTransition(bindingMain.rvList, new AutoTransition())
        );
        bindingMain.rvList.setAdapter(adapter);
    }

    private void initRemoveSection() {
        bindingMain.npRemoveEntries.setWrapSelectorWheel(false);
        bindingMain.npRemoveEntries.setMinValue(1);
        bindingMain.npRemoveEntries.setMaxValue(Integer.MAX_VALUE);
        bindingMain.btnRemoveEntries.setOnClickListener(v -> {
            int count = bindingMain.npRemoveEntries.getValue();
            databaseManager.deleteDeviceEvents(count, true);

            initCount();
            initDeviceItems();
        });
    }

    private void initCount() {
        long deviceEventsCount = databaseManager.getDeviceEventsCount();
        bindingMain.tvCount.setText(String.format(Locale.US, "Count: %d", deviceEventsCount));
    }

    private void initDeviceItems() {
        List<Pair<String, Device>> newItems = databaseManager.getDeviceEvents(null);
        adapter.setItems(newItems);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bindingMain = null;
        databaseManager = null;
    }

    //==============================================================================================
    private static class DeviceAdapter extends RecyclerView.Adapter<DeviceViewHolder> {

        private List<Pair<String, Device>> items;
        private final ViewHolderListener onExpandCollapseClickListener;

        private DeviceAdapter(ViewHolderListener listener) {
            this.onExpandCollapseClickListener = listener;
        }

        private void setItems(List<Pair<String, Device>> newItems) {
            this.items = newItems;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemDbDeviceBinding binding = ItemDbDeviceBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new DeviceViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
            Pair<String, Device> item = items.get(position);
            initListeners(holder.bindingHolder);
            holder.bind(item.component1(), item.component2());
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        private void initListeners(ItemDbDeviceBinding binding) {
            binding.ivExpand.setOnClickListener(v -> {
                if (binding.llContent.getVisibility() == View.VISIBLE) {
                    onExpandCollapseClickListener.onExpandCollapse();
                    binding.llContent.setVisibility(View.GONE);
                    binding.ivExpand.setImageResource(R.drawable.ic_expand_more);
                } else {
                    onExpandCollapseClickListener.onExpandCollapse();
                    binding.llContent.setVisibility(View.VISIBLE);
                    binding.ivExpand.setImageResource(R.drawable.ic_expand_less);
                }
            });
        }
    }

    private static class DeviceViewHolder extends RecyclerView.ViewHolder {
        private final ItemDbDeviceBinding bindingHolder;

        private DeviceViewHolder(ItemDbDeviceBinding binding) {
            super(binding.getRoot());
            this.bindingHolder = binding;
        }

        private void bind(String timestamp, Device device) {
            bindingHolder.tvValueTimestamp.setText(timestamp);

            bindingHolder.tvDeviceId.setTextOrHide(device.getDeviceId());
            bindingHolder.tvExternalUserId.setTextOrHide(device.getExternalUserId());
            bindingHolder.tvPushToken.setTextOrHide(device.getPushToken());
            bindingHolder.tvCategory.setTextOrHide(device.getCategory().toString());
            bindingHolder.tvOsType.setTextOrHide(device.getOsType().toString());
            bindingHolder.tvOsVersion.setTextOrHide(device.getOsVersion());
            bindingHolder.tvDeviceModel.setTextOrHide(device.getDeviceModel());
            bindingHolder.tvAppVersion.setTextOrHide(device.getAppVersion());
            bindingHolder.tvLanguageCode.setTextOrHide(device.getLanguageCode());
            bindingHolder.tvTimeZone.setTextOrHide(device.getTimeZone());
            bindingHolder.tvAdvertisingId.setTextOrHide(device.getAdvertisingId());
        }
    }

    private interface ViewHolderListener {
        void onExpandCollapse();
    }
}
