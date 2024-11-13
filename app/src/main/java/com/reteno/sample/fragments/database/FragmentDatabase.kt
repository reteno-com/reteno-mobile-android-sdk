package com.reteno.sample.fragments.database

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import com.reteno.sample.databinding.FragmentDatabaseBinding

class FragmentDatabase : Fragment() {

    private var binding: FragmentDatabaseBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDatabaseBinding.inflate(layoutInflater, container, false)
        return binding!!.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListeners()
    }

    private fun initListeners() {
        binding!!.btnWriteDevice.setOnClickListener { showDialogWriteDevice() }
        binding!!.btnReadDevice.setOnClickListener { showDialogReadDevice() }
        binding!!.btnWriteUser.setOnClickListener { showDialogWriteUser() }
        binding!!.btnReadUser.setOnClickListener { showDialogReadUser() }
        binding!!.btnWriteInteraction.setOnClickListener { showDialogWriteInteraction() }
        binding!!.btnReadInteraction.setOnClickListener { showDialogReadInteraction() }
        binding!!.btnWriteEvent.setOnClickListener { showDialogWriteEvent() }
        binding!!.btnReadEvent.setOnClickListener { showDialogReadEvent() }
        binding!!.btnWriteInbox.setOnClickListener { showDialogWriteInbox() }
        binding!!.btnReadInbox.setOnClickListener { showDialogReadInbox() }
    }

    private fun showDialogWriteDevice() {
        NavHostFragment.findNavController(this)
            .navigate(FragmentDatabaseDirections.databaseToWriteDevice())
    }

    private fun showDialogReadDevice() {
        NavHostFragment.findNavController(this)
            .navigate(FragmentDatabaseDirections.databaseToReadDevice())
    }

    private fun showDialogWriteUser() {
        NavHostFragment.findNavController(this)
            .navigate(FragmentDatabaseDirections.databaseToWriteUser())
    }

    private fun showDialogReadUser() {
        NavHostFragment.findNavController(this)
            .navigate(FragmentDatabaseDirections.databaseToReadUser())
    }

    private fun showDialogWriteInteraction() {
        NavHostFragment.findNavController(this)
            .navigate(FragmentDatabaseDirections.databaseToWriteInteraction())
    }

    private fun showDialogReadInteraction() {
        NavHostFragment.findNavController(this)
            .navigate(FragmentDatabaseDirections.databaseToReadInteraction())
    }

    private fun showDialogWriteEvent() {
        NavHostFragment.findNavController(this)
            .navigate(FragmentDatabaseDirections.databaseToWriteEvent())
    }

    private fun showDialogReadEvent() {
        NavHostFragment.findNavController(this)
            .navigate(FragmentDatabaseDirections.databaseToReadEvent())
    }

    private fun showDialogWriteInbox() {
        NavHostFragment.findNavController(this)
            .navigate(FragmentDatabaseDirections.databaseToWriteInbox())
    }

    private fun showDialogReadInbox() {
        NavHostFragment.findNavController(this)
            .navigate(FragmentDatabaseDirections.databaseToReadInbox())
    }
}