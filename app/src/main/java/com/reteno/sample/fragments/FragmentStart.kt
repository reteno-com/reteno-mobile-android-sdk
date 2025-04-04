package com.reteno.sample.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.Toast
import androidx.navigation.NavDirections
import androidx.navigation.fragment.NavHostFragment
import com.reteno.core.RetenoInternalImpl
import com.reteno.core.di.ServiceLocator
import com.reteno.core.features.iam.InAppPauseBehaviour
import com.reteno.core.lifecycle.RetenoSessionHandler
import com.reteno.sample.BaseFragment
import com.reteno.sample.R
import com.reteno.sample.databinding.FragmentStartBinding
import com.reteno.sample.testscreens.ScreenAdapter
import com.reteno.sample.testscreens.ScreenAdapter.ScreenItemClick
import com.reteno.sample.testscreens.ScreenItem
import com.reteno.sample.util.AppSharedPreferencesManager
import com.reteno.sample.util.FragmentStartSessionListener
import com.reteno.sample.util.RetenoInitListener

class FragmentStart : BaseFragment() {

    private var binding: FragmentStartBinding? = null
    private var sessionHandler: RetenoSessionHandler? = null
    private val sessionListener = FragmentStartSessionListener()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentStartBinding.inflate(inflater, container, false)
        return binding!!.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        awaitInit()
        val adapter = ScreenAdapter(getScreenList(), object : ScreenItemClick {
            override fun navigateById(fragmentId: Int) {
                NavHostFragment.findNavController(this@FragmentStart).navigate(fragmentId)
            }

            override fun navigateById(fragmentId: Int, bundle: Bundle) {
                NavHostFragment.findNavController(this@FragmentStart).navigate(fragmentId, bundle)
            }

            override fun navigateByDirections(navDirections: NavDirections) {
                NavHostFragment.findNavController(this@FragmentStart).navigate(navDirections)
            }
        })
        binding!!.recycler.adapter = adapter
        initInAppPausingSwitcher()
        initPauseBehaviourSwitcher()
        initDelayedInitCheckbox()
        initIamBaseUrl()
    }

    override fun onDestroyView() {
        binding!!.recycler.adapter = null
        binding!!.spinnerPauseBehaviour.onItemSelectedListener = null
        binding!!.spinnerPushPauseBehaviour.onItemSelectedListener = null
        binding = null
        super.onDestroyView()
    }

    override fun onPause() {
        super.onPause()
        sessionListener.stop()
    }

    override fun onResume() {
        super.onResume()
        if (sessionHandler != null) {
            sessionListener.start(sessionHandler!!, binding!!.tvSessionTime)
        }
    }

    private fun getScreenList(): List<ScreenItem> {
        return listOf(
            ScreenItem("Device Id", direction =  FragmentStartDirections.startToDeviceId()),
            ScreenItem("Sentry", direction = FragmentStartDirections.startToSentry()),
            ScreenItem(
                "Second Activity",
                direction = FragmentStartDirections.startToActivitySecond()
            ),
            ScreenItem("Notifications", direction = FragmentStartDirections.startToNotifications()),
            ScreenItem("User data",direction =  FragmentStartDirections.startToUserData()),
            ScreenItem(
                "User Anonymous data",
                direction = FragmentStartDirections.startToUserAnonymousData()
            ),
            ScreenItem("Custom Data", navigationId = R.id.start_to_custom_data, bundle = arguments),
            ScreenItem("Database", direction = FragmentStartDirections.startToDatabase()),
            ScreenItem("Custom event", direction = FragmentStartDirections.startToCustomEvent()),
            ScreenItem(
                "App lifecycle events",
                direction = FragmentStartDirections.startToAppLifecycleEvents()
            ),
            ScreenItem("Force push", direction = FragmentStartDirections.startToForcePush()),
            ScreenItem(
                "Screen tracking",
                direction =  FragmentStartDirections.startToScreenTracking()
            ),
            ScreenItem("App Inbox",direction =  FragmentStartDirections.startToAppInbox()),
            ScreenItem(
                "Recommendations GET",
                direction = FragmentStartDirections.startToRecommendationsGet()
            ),
            ScreenItem(
                "Recommendations POST",
                direction = FragmentStartDirections.startToRecommendationsPost()
            ),
            ScreenItem("Ecom Events", direction = FragmentStartDirections.startToEcomEvents())
        )
    }

    private fun initIamBaseUrl() {
        val prefs = requireActivity()
            .getSharedPreferences("reteno_shared_prefs", Context.MODE_PRIVATE)
        val coreUrl = "https://statics.esputnik.com/in-app/base.latest.html"
        val custom = prefs.getString("iam_base_url", null)
        binding!!.etBaseInAppURL.setText(custom ?: coreUrl)
        binding!!.saveInAppBase.setOnClickListener {
            try {
                prefs
                    .edit()
                    .putString("iam_base_url", binding!!.etBaseInAppURL.text?.toString()?.takeIf { it.isNotBlank() })
                    .apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            Toast.makeText(requireContext(), "SAVED", Toast.LENGTH_LONG).show()
        }
    }


    private fun initPauseBehaviourSwitcher() {
        binding!!.spinnerPauseBehaviour.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            InAppPauseBehaviour.values()
        )
        binding!!.spinnerPauseBehaviour.setSelection(InAppPauseBehaviour.POSTPONE_IN_APPS.ordinal)
        binding!!.spinnerPauseBehaviour.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View?,
                    i: Int,
                    l: Long
                ) {
                    reteno.setInAppMessagesPauseBehaviour(InAppPauseBehaviour.values()[i])
                }

                override fun onNothingSelected(adapterView: AdapterView<*>?) {}
            }
        binding!!.spinnerPushPauseBehaviour.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            InAppPauseBehaviour.values()
        )
        binding!!.spinnerPushPauseBehaviour.setSelection(InAppPauseBehaviour.POSTPONE_IN_APPS.ordinal)
        binding!!.spinnerPushPauseBehaviour.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View?,
                    i: Int,
                    l: Long
                ) {
                    reteno.setPushInAppMessagesPauseBehaviour(InAppPauseBehaviour.values()[i])
                }

                override fun onNothingSelected(adapterView: AdapterView<*>?) {}
            }
    }

    private fun initInAppPausingSwitcher() {
        binding!!.swInAppsPause.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            reteno.pauseInAppMessages(
                isChecked
            )
        }
        binding!!.swPushInAppsPause.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            reteno.pausePushInAppMessages(
                isChecked
            )
        }
    }

    private fun awaitInit() {
        val impl = reteno as RetenoInternalImpl
        if (impl.isInitialized) {
            initSessionHandler()
            binding!!.progressBar.visibility = View.GONE
            binding!!.cbDelayNextLaunch.visibility = View.VISIBLE
        } else {
            binding!!.progressBar.visibility = View.VISIBLE
            binding!!.cbDelayNextLaunch.visibility = View.GONE
            RetenoInitListener(impl) {
                if (isResumed) {
                    initSessionHandler()
                    binding!!.progressBar.visibility = View.GONE
                    binding!!.cbDelayNextLaunch.visibility = View.VISIBLE
                }
                Unit
            }
        }
    }

    private fun initSessionHandler() {
        try {
            val field = RetenoInternalImpl::class.java.getDeclaredField("serviceLocator")
            field.isAccessible = true
            val serviceLocator = field[reteno] as ServiceLocator
            field.isAccessible = false
            sessionHandler = serviceLocator.retenoSessionHandlerProvider.get()
            sessionListener.start(sessionHandler!!, binding!!.tvSessionTime)
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
    }

    private fun initDelayedInitCheckbox() {
        binding!!.cbDelayNextLaunch.setChecked(
            AppSharedPreferencesManager.getShouldDelayLaunch(
                requireContext()
            )
        )
        binding!!.cbDelayNextLaunch.setOnCheckedChangeListener { compoundButton: CompoundButton, b: Boolean ->
            AppSharedPreferencesManager.setDelayLaunch(
                compoundButton.context,
                b
            )
        }
    }
}