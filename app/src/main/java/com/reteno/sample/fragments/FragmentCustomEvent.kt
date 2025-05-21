package com.reteno.sample.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.navigation.fragment.NavHostFragment
import com.reteno.core.domain.model.event.Event
import com.reteno.core.domain.model.event.Parameter
import com.reteno.sample.BaseFragment
import com.reteno.sample.R
import com.reteno.sample.databinding.FragmentCustomEventBinding
import com.reteno.sample.util.Util
import java.time.ZonedDateTime

class FragmentCustomEvent : BaseFragment() {

    private var binding: FragmentCustomEventBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCustomEventBinding.inflate(inflater, container, false)
        return binding!!.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()
    }

    private fun setListeners() {
        binding!!.btnSend.setOnClickListener { v: View? ->
            try {
                sendCustomEvent()
                Toast.makeText(this.context, "Sent", Toast.LENGTH_SHORT).show()
                NavHostFragment.findNavController(this).popBackStack()
            } catch (e: Exception) {
                Toast.makeText(this.context, e.message, Toast.LENGTH_LONG).show()
                Log.e(TAG, "FragmentCustomEvent", e)
            }
        }
        binding!!.btnCustomFieldsAdd.setOnClickListener {
            val view = createNewFields()
            binding!!.llCustomData.addView(view)
        }
        binding!!.btnCustomFieldsMinus.setOnClickListener {
            val countView = binding!!.llCustomData.childCount
            if (countView > 0) {
                binding!!.llCustomData.removeViewAt(countView - 1)
            }
        }
    }

    private fun sendCustomEvent() {
        val eventTypeKey = Util.getTextOrNull(binding!!.etEventType)
        if (eventTypeKey == null) {
            Toast.makeText(this.context, "Event type key must be not null", Toast.LENGTH_LONG)
                .show()
            return
        }
        reteno.logEvent(Event.Custom(eventTypeKey, ZonedDateTime.now(), getUserCustomData()))
    }

    private fun getUserCustomData(): List<Parameter>? {
        val countView = binding!!.llCustomData.childCount
        if (countView == 0) return null
        val list: MutableList<Parameter> = ArrayList()
        for (i in 0 until countView) {
            val parent = binding!!.llCustomData.getChildAt(i) as LinearLayout
            val etKey = parent.getChildAt(0) as EditText
            val etValue = parent.getChildAt(1) as EditText
            val key = Util.getTextOrNull(etKey)
            val value = Util.getTextOrNull(etValue)
            if (key != null && value != null) {
                list.add(Parameter(key, value))
            }
        }
        return list
    }

    private fun createNewFields(): View {
        return LayoutInflater
            .from(binding!!.getRoot().context)
            .inflate(R.layout.view_user_custom_fields_horizontal, binding!!.llCustomData, false)
    }

    companion object {
        private val TAG = FragmentCustomEvent::class.java.getSimpleName()
    }
}