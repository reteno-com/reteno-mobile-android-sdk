package com.reteno.sample.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.reteno.core.RetenoInternalImpl
import com.reteno.core.di.ServiceLocator
import com.reteno.core.domain.controller.AppLifecycleController
import com.reteno.core.domain.model.event.LifecycleEventType
import com.reteno.core.domain.model.event.LifecycleTrackingOptions
import com.reteno.sample.BaseFragment
import com.reteno.sample.databinding.FragmentAppLifecycleEventsBinding

class FragmentAppLifecycleEvents : BaseFragment() {

    private var binding: FragmentAppLifecycleEventsBinding? = null
    private var appLifecycleController: AppLifecycleController? = null
    private var config = LifecycleTrackingOptions.ALL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val field = RetenoInternalImpl::class.java.getDeclaredField("serviceLocator")
            field.isAccessible = true
            val serviceLocator = field[reteno] as ServiceLocator
            appLifecycleController = serviceLocator.appLifecycleControllerProvider.get()
            field.isAccessible = false
            val configField = AppLifecycleController::class.java.getDeclaredField("lifecycleEventConfig")
            configField.isAccessible = true
            val configMap = configField[appLifecycleController] as Map<LifecycleEventType, Boolean>
            config = LifecycleTrackingOptions(
                appLifecycleEnabled = configMap.getOrDefault(LifecycleEventType.APP_LIFECYCLE, false),
                pushSubscriptionEnabled = configMap.getOrDefault(LifecycleEventType.PUSH, false),
                sessionEventsEnabled = configMap.getOrDefault(LifecycleEventType.SESSION, false)
            )
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentAppLifecycleEventsBinding.inflate(inflater, container, false).run {
            binding = this
            root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.apply {
            itemAppLifecycle.tvTitle.text = "AppLifecycle"
            itemAppLifecycle.cbEnabled.isChecked = config.appLifecycleEnabled
            itemAppLifecycle.cbEnabled.setOnCheckedChangeListener { _, b ->
                config = config.copy(appLifecycleEnabled = b)
            }
            itemPush.tvTitle.text = "PushSubsription"
            itemPush.cbEnabled.isChecked = config.pushSubscriptionEnabled
            itemPush.cbEnabled.setOnCheckedChangeListener { _, b ->
                config = config.copy(pushSubscriptionEnabled = b)
            }
            itemSession.tvTitle.text = "Sessions"
            itemSession.cbEnabled.isChecked = config.sessionEventsEnabled
            itemSession.cbEnabled.setOnCheckedChangeListener { _, b ->
                config = config.copy(sessionEventsEnabled = b)
            }
            btnSave.setOnClickListener {
                appLifecycleController?.setLifecycleEventConfig(config)
                Toast.makeText(requireContext(), "SAVED", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        appLifecycleController = null
        super.onDestroy()
    }
}