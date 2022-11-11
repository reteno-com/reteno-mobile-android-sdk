package com.reteno.sample.fragments.database;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import com.reteno.core.domain.model.device.Device;
import com.reteno.sample.R;
import com.reteno.sample.databinding.ItemDbDeviceBinding;

import java.util.List;
import java.util.Locale;

import kotlin.Pair;


public class DeviceReadDialogFragment extends BaseReadDialogFragment<Device, ItemDbDeviceBinding, DeviceReadDialogFragment.DeviceViewHolder, DeviceReadDialogFragment.DeviceAdapter> {

    @Override
    protected void initAdapter() {
        adapter = new DeviceAdapter(() ->
                TransitionManager.beginDelayedTransition(bindingMain.rvList, new AutoTransition())
        );
    }

    @Override
    protected void initCount() {
        long deviceEventsCount = databaseManager.getDeviceCount();
        bindingMain.tvCount.setText(String.format(Locale.US, "Count: %d", deviceEventsCount));
    }

    @Override
    protected void deleteItems(int count) {
        databaseManager.deleteDevices(count, true);
    }

    @Override
    protected void initItems() {
        List<Device> newItems = databaseManager.getDevices(null);
        adapter.setItems(newItems);
    }

    //==============================================================================================
    static class DeviceAdapter extends BaseReadAdapter<Device, ItemDbDeviceBinding, DeviceViewHolder> {

        DeviceAdapter(ViewHolderListener listener) {
            super(listener);
        }

        @NonNull
        @Override
        public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemDbDeviceBinding binding = ItemDbDeviceBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new DeviceViewHolder(binding);
        }

        @Override
        protected void initListeners(ItemDbDeviceBinding binding) {
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

    //==============================================================================================
    static class DeviceViewHolder extends BaseReadViewHolder<Device, ItemDbDeviceBinding> {

        DeviceViewHolder(ItemDbDeviceBinding binding) {
            super(binding);
        }

        @Override
        protected void bind(Device device) {
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
}
