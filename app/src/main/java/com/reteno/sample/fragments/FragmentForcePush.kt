package com.reteno.sample.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.reteno.sample.BaseFragment
import com.reteno.sample.databinding.FragmentForcePushBinding

class FragmentForcePush : BaseFragment() {

    private var binding: FragmentForcePushBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentForcePushBinding.inflate(layoutInflater, container, false)
        return binding!!.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListeners()
    }

    private fun initListeners() {
        binding!!.btnForcePush.setOnClickListener { reteno.forcePushData() }
    }
}
