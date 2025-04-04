package com.reteno.sample.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.reteno.push.RetenoNotifications
import com.reteno.sample.BaseFragment
import com.reteno.sample.databinding.FragmentNotificationsBinding

class FragmentNotifications: BaseFragment() {

    private var binding: FragmentNotificationsBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding!!.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()
    }

    private fun setListeners() = with(requireNotNull(binding)) {
        btnApply.setOnClickListener {
            RetenoNotifications.updateDefaultNotificationChannel(
                name = etName.text?.toString(),
                description = etDescription.text?.toString()
            )
            etName.text?.clear()
            etDescription.text?.clear()
            Toast.makeText(requireContext(), "Updated", Toast.LENGTH_SHORT).show()
        }
    }
}