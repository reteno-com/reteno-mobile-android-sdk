package com.reteno.sample.fragments.database

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.reteno.core.RetenoInternalImpl
import com.reteno.core._interop.DeviceIdInternal.getIdInternal
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerAppInbox
import com.reteno.core.data.local.model.appinbox.AppInboxMessageDb
import com.reteno.core.data.local.model.appinbox.AppInboxMessageStatusDb
import com.reteno.core.util.Util.getCurrentTimeStamp
import com.reteno.sample.databinding.DialogDbWriteInboxBinding
import com.reteno.sample.util.Util

class InboxWriteDialogFragment : BaseDatabaseDialogFragment() {
    private var binding: DialogDbWriteInboxBinding? = null
    private var databaseManager: RetenoDatabaseManagerAppInbox? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        databaseManager = serviceLocator.retenoDatabaseManagerAppInboxProvider.get()
    }

    override fun onDestroy() {
        super.onDestroy()
        databaseManager = null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogDbWriteInboxBinding.inflate(layoutInflater)
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
        binding!!.btnSubmit.setOnClickListener { v: View? ->
            val messageId = Util.getTextOrNull(
                binding!!.etInteractionId
            )
            val retenoImpl = RetenoInternalImpl.instance
            val deviceIdModel =
                retenoImpl.serviceLocator.configRepositoryProvider.get().getDeviceId()
            val deviceId = deviceIdModel.getIdInternal()
            if (messageId != null) {
                val inbox = AppInboxMessageDb(
                    messageId,
                    deviceId,
                    getCurrentTimeStamp(),
                    AppInboxMessageStatusDb.OPENED
                )
                databaseManager!!.insertAppInboxMessage(inbox)
                Toast.makeText(this.context, "Save", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
