package com.reteno.sample.fragments.database

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInteraction
import com.reteno.core.data.local.model.interaction.InteractionDb
import com.reteno.core.data.local.model.interaction.InteractionStatusDb.Companion.fromString
import com.reteno.core.util.allElementsNotNull
import com.reteno.sample.databinding.DialogDbWriteInteractionBinding
import com.reteno.sample.util.Util

class InteractionWriteDialogFragment : BaseDatabaseDialogFragment() {
    private var binding: DialogDbWriteInteractionBinding? = null
    private var databaseManager: RetenoDatabaseManagerInteraction? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        databaseManager = serviceLocator.retenoDatabaseManagerInteractionProvider.get()
    }

    override fun onDestroy() {
        super.onDestroy()
        databaseManager = serviceLocator.retenoDatabaseManagerInteractionProvider.get()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogDbWriteInteractionBinding.inflate(
            layoutInflater
        )
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
            val interactionId = Util.getTextOrNull(binding!!.etInteractionId)
            val status = Util.getTextOrNull(binding!!.etStatus)
            val time = Util.getTextOrNull(binding!!.etTime)
            val token = Util.getTextOrNull(binding!!.etToken)
            val action = Util.getTextOrNull(binding!!.etAction)
            if (allElementsNotNull(
                    interactionId,
                    status,
                    time
                ) && (token != null || action != null)
            ) {
                val interaction = InteractionDb(
                    null,
                    interactionId.orEmpty(),
                    fromString(status),
                    time.orEmpty(),
                    token,
                    action
                )
                databaseManager!!.insertInteraction(interaction)
                Toast.makeText(this.context, "Sent", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
