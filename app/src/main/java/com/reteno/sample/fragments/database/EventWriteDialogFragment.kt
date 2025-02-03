package com.reteno.sample.fragments.database

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import com.reteno.core.RetenoImpl
import com.reteno.core._interop.DeviceIdInternal.getIdInternal
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerEvents
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.local.model.event.EventsDb
import com.reteno.core.domain.model.event.Event
import com.reteno.core.domain.model.event.Events
import com.reteno.core.domain.model.event.Parameter
import com.reteno.sample.R
import com.reteno.sample.SampleApp
import com.reteno.sample.databinding.DialogDbWriteEventBinding
import com.reteno.sample.databinding.ViewEventWriteBinding
import com.reteno.sample.databinding.ViewUserCustomFieldsHorizontalBinding
import com.reteno.sample.util.Util
import java.time.ZonedDateTime

class EventWriteDialogFragment : BaseDatabaseDialogFragment() {
    private var binding: DialogDbWriteEventBinding? = null
    private var databaseManager: RetenoDatabaseManagerEvents? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        databaseManager = serviceLocator.retenoDatabaseManagerEventsProvider.get()
    }

    override fun onDestroy() {
        super.onDestroy()
        databaseManager = null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogDbWriteEventBinding.inflate(layoutInflater)
        return AlertDialog.Builder(requireActivity())
            .setView(binding!!.getRoot())
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onStart() {
        super.onStart()
        initListeners()
    }

    private fun initListeners() {
        binding!!.btnSubmit.setOnClickListener {
            val events = getEventData()
            val eventsDb: EventsDb = events.toDb()
            databaseManager!!.insertEvents(eventsDb)
            Toast.makeText(this.context, "Sent", Toast.LENGTH_SHORT).show()
        }
        binding!!.btnEventPlus.setOnClickListener {
            val view = createEvent()
            binding!!.llEvents.addView(view)
        }
        binding!!.btnEventMinus.setOnClickListener {
            val countView = binding!!.llEvents.childCount
            if (countView > 0) {
                binding!!.llEvents.removeViewAt(countView - 1)
            }
        }
    }

    private fun getEventData():Events {
        val retenoImpl = RetenoImpl.instance
        val deviceIdModel = retenoImpl.serviceLocator.configRepositoryProvider.get().getDeviceId()
        val deviceId = deviceIdModel.getIdInternal()
        val externalUserId = Util.getTextOrNull(binding!!.etExternalUserId)
        val eventList = getEventList()
        return Events(deviceId, externalUserId, eventList)
    }

    private fun getEventList(): List<Event> {
        val list: MutableList<Event> = ArrayList()
        val countView = binding!!.llEvents.childCount
        if (countView == 0) return list
        for (i in 0 until countView) {
            val parent = binding!!.llEvents.getChildAt(i) as LinearLayout
            val etEventTypeKey = parent.getChildAt(0) as EditText
            val eventTypeKey = Util.getTextOrNull(etEventTypeKey)
            val occurred = ZonedDateTime.now()
            if (eventTypeKey != null) {
                val params = getParamsList(parent)
                list.add(Event.Custom(eventTypeKey, occurred, params))
            }
        }
        return list
    }

    private fun getParamsList(container: ViewGroup): List<Parameter> {
        val list: MutableList<Parameter> = ArrayList()
        val paramsView = container.findViewById<LinearLayout>(R.id.llParams)
        val countView = paramsView.childCount
        if (countView == 0) return list
        for (i in 0 until countView) {
            val parent = paramsView.getChildAt(i) as LinearLayout
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

    private fun createEvent(): View {
        val viewEventBinding = ViewEventWriteBinding.inflate(
            layoutInflater, binding!!.llEvents, false
        )
        viewEventBinding.btnParamPlus.setOnClickListener {
            val view = createParam(viewEventBinding)
            viewEventBinding.llParams.addView(view)
        }
        viewEventBinding.btnParamMinus.setOnClickListener {
            val countView = viewEventBinding.llParams.childCount
            if (countView > 0) {
                viewEventBinding.llParams.removeViewAt(countView - 1)
            }
        }
        return viewEventBinding.getRoot()
    }

    private fun createParam(viewEventBinding: ViewEventWriteBinding): View {
        val viewParamsBinding = ViewUserCustomFieldsHorizontalBinding.inflate(
            layoutInflater, viewEventBinding.llParams, false
        )
        return viewParamsBinding.getRoot()
    }
}
