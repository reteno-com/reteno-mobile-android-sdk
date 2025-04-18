package com.reteno.sample.fragments.database

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerDevice
import com.reteno.core.data.local.model.device.DeviceDb
import com.reteno.sample.R
import com.reteno.sample.databinding.ItemDbDeviceBinding
import com.reteno.sample.fragments.database.DeviceReadDialogFragment.DeviceAdapter
import com.reteno.sample.fragments.database.DeviceReadDialogFragment.DeviceViewHolder
import java.util.Locale

internal class DeviceReadDialogFragment :
    BaseReadDialogFragment<DeviceDb, ItemDbDeviceBinding, DeviceViewHolder, DeviceAdapter>() {

    private var databaseManager: RetenoDatabaseManagerDevice? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        databaseManager = serviceLocator.retenoDatabaseManagerDeviceProvider.get()
    }

    override fun onDestroy() {
        super.onDestroy()
        databaseManager = null
    }

    override fun initAdapter() {
        adapter = DeviceAdapter {
            TransitionManager.beginDelayedTransition(
                bindingMain!!.rvList, AutoTransition()
            )
        }
    }

    override fun initCount() {
        val deviceEventsCount = databaseManager!!.getUnSyncedDeviceCount()
        bindingMain!!.tvCount.text = String.format(Locale.US, "Count: %d", deviceEventsCount)
    }

    override fun deleteItems(count: Int) {
        val devices = databaseManager!!.getDevices(count)
        for (device in devices) {
            databaseManager!!.deleteDevice(device)
        }
    }

    override fun initItems() {
        val newItems = databaseManager!!.getDevices(null)
        adapter!!.setItems(newItems)
    }

    //==============================================================================================
    internal class DeviceAdapter(listener: ViewHolderListener) :
        BaseReadAdapter<DeviceDb, ItemDbDeviceBinding, DeviceViewHolder>(listener) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
            val binding =
                ItemDbDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return DeviceViewHolder(binding)
        }

        override fun initListeners(binding: ItemDbDeviceBinding) {
            binding.ivExpand.setOnClickListener {
                if (binding.llContent.visibility == View.VISIBLE) {
                    onExpandCollapseClickListener.onExpandCollapse()
                    binding.llContent.visibility = View.GONE
                    binding.ivExpand.setImageResource(R.drawable.ic_expand_more)
                } else {
                    onExpandCollapseClickListener.onExpandCollapse()
                    binding.llContent.visibility = View.VISIBLE
                    binding.ivExpand.setImageResource(R.drawable.ic_expand_less)
                }
            }
        }
    }

    //==============================================================================================
    internal class DeviceViewHolder(binding: ItemDbDeviceBinding) :
        BaseReadViewHolder<DeviceDb, ItemDbDeviceBinding>(binding) {
         override fun bind(model: DeviceDb) {
            bindingHolder.tvDeviceId.setTextOrHide(model.deviceId)
            bindingHolder.tvExternalUserId.setTextOrHide(model.externalUserId)
            bindingHolder.tvPushToken.setTextOrHide(model.pushToken)
            var pushSubscribed: String? = null
            if (model.pushSubscribed != null) {
                pushSubscribed = model.pushSubscribed.toString()
            }
            bindingHolder.tvPushSubscribed.setTextOrHide(pushSubscribed)
            bindingHolder.tvCategory.setTextOrHide(model.category.toString())
            bindingHolder.tvOsType.setTextOrHide(model.osType.toString())
            bindingHolder.tvOsVersion.setTextOrHide(model.osVersion)
            bindingHolder.tvDeviceModel.setTextOrHide(model.deviceModel)
            bindingHolder.tvAppVersion.setTextOrHide(model.appVersion)
            bindingHolder.tvLanguageCode.setTextOrHide(model.languageCode)
            bindingHolder.tvTimeZone.setTextOrHide(model.timeZone)
            bindingHolder.tvAdvertisingId.setTextOrHide(model.advertisingId)
        }
    }
}
