package com.reteno.sample.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavDirections
import androidx.navigation.fragment.NavHostFragment
import com.reteno.core.features.iam.InAppPauseBehaviour
import com.reteno.sample.BaseFragment
import com.reteno.sample.R
import com.reteno.sample.SampleApp
import com.reteno.sample.databinding.FragmentStartBinding
import com.reteno.sample.testscreens.ScreenAdapter
import com.reteno.sample.testscreens.ScreenAdapter.ScreenItemClick
import com.reteno.sample.testscreens.ScreenItem
import com.reteno.sample.util.AppSharedPreferencesManager
import com.reteno.sample.util.SdkInAppBaseUrl
import com.reteno.sample.util.SdkSessionInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FragmentStart : BaseFragment() {

    private var binding: FragmentStartBinding? = null

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
        initInitProgress()
        initIamBaseUrl()
        initSessionDurationChange()
        initSessionTime()
    }

    override fun onDestroyView() {
        binding!!.recycler.adapter = null
        binding!!.spinnerPauseBehaviour.onItemSelectedListener = null
        binding!!.spinnerPushPauseBehaviour.onItemSelectedListener = null
        binding = null
        super.onDestroyView()
    }

    private fun getScreenList(): List<ScreenItem> {
        return listOf(
            ScreenItem("Device Id", direction =  FragmentStartDirections.startToDeviceId()),
            ScreenItem("Error reporting", direction = FragmentStartDirections.startToErrorReporting()),
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

    private fun initSessionTime() {
        binding!!.tvSessionTimeTitle.visibility = if (SdkSessionInfo.isAvailable) View.VISIBLE else View.GONE
        binding!!.tvSessionTime.visibility = binding!!.tvSessionTimeTitle.visibility
        if (!SdkSessionInfo.isAvailable) return
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    val sessionStart = SdkSessionInfo.sessionStartTimestamp(requireContext())
                    binding!!.tvSessionTime.text = formatSessionTime(sessionStart)
                    delay(1000L)
                }
            }
        }
    }

    private fun formatSessionTime(sessionStart: Long): String {
        if (sessionStart == 0L) return "not started"
        val timeSeconds = (System.currentTimeMillis() - sessionStart) / 1000L
        val timeMinutes = timeSeconds / 60L
        val timeHours = timeMinutes / 60L
        return when {
            timeHours > 0 -> "${timeHours}h ${timeMinutes % 60L}m ${timeSeconds % 60L}s"
            timeMinutes > 0 -> "${timeMinutes}m ${timeSeconds % 60L}s"
            else -> "${timeSeconds}s"
        }
    }

    private fun initIamBaseUrl() {
        binding!!.etBaseInAppURL.visibility = if (SdkInAppBaseUrl.isAvailable) View.VISIBLE else View.GONE
        binding!!.saveInAppBase.visibility = binding!!.etBaseInAppURL.visibility
        if (!SdkInAppBaseUrl.isAvailable) return
        binding!!.etBaseInAppURL.setText(SdkInAppBaseUrl.baseUrl(requireContext()))
        binding!!.saveInAppBase.setOnClickListener {
            val url = binding!!.etBaseInAppURL.text?.toString()?.trim().orEmpty()
            val message = when {
                url.isEmpty() -> {
                    SdkInAppBaseUrl.setBaseUrl(requireContext(), null)
                    "Base URL reset to the SDK default"
                }
                isHttpsUrl(url) -> {
                    SdkInAppBaseUrl.setBaseUrl(requireContext(), url)
                    "Base URL saved, used the next time the SDK loads the base html"
                }
                else -> "Base URL must be an https:// address"
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

    private fun isHttpsUrl(url: String): Boolean {
        val uri = Uri.parse(url)
        return uri.scheme == "https" && !uri.host.isNullOrBlank()
    }

    private fun initSessionDurationChange() {
        binding!!.saveSessionDuration.setOnClickListener {
            try {
                val durationStr = binding!!.etSessionDuration.text?.toString().orEmpty()
                if (durationStr.isNotBlank()) {
                    val duration = durationStr.filter { it.isDigit() }.toInt() * 1000L
                    SdkSessionInfo.setSessionDuration(duration)
                    val applied = if (SdkSessionInfo.isAvailable) {
                        "Session duration changed"
                    } else {
                        "Applied on next launch"
                    }
                    Toast.makeText(requireContext(), applied, Toast.LENGTH_SHORT).show()
                    binding!!.etSessionDuration.text?.clear()
                    AppSharedPreferencesManager.saveSessionDuration(requireContext(), duration)
                }
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

    private fun initInitProgress() {
        val app = requireActivity().application as SampleApp
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                app.isRetenoInitialized.collect { initialized ->
                    binding!!.progressBar.visibility = if (initialized) View.GONE else View.VISIBLE
                    binding!!.cbDelayNextLaunch.visibility = if (initialized) View.VISIBLE else View.GONE
                }
            }
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