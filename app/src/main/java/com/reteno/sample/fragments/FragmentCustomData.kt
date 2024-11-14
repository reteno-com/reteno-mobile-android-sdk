package com.reteno.sample.fragments

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.reteno.core.util.Logger
import com.reteno.core.util.toStringVerbose
import com.reteno.sample.databinding.FragmentCustomDataBinding
import com.reteno.sample.databinding.ItemCustomDataKeyBinding

class FragmentCustomData : Fragment() {
    private var binding: FragmentCustomDataBinding? = null
    private var bundle: Bundle? = null
    private var adapter: KeyAdapter? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCustomDataBinding.inflate(inflater, container, false)
        return binding!!.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (checkBundle(view)) {
            fillKeySet()
        }
    }

    private fun checkBundle(view: View): Boolean {
        bundle = arguments
        return if (bundle == null) {
            Toast.makeText(context, "No custom data found", Toast.LENGTH_SHORT).show()
            findNavController(view).navigateUp()
            false
        } else {
            true
        }
    }

    private fun fillKeySet() {
        Logger.i(TAG, "Custom data = " + bundle.toStringVerbose())
        val keySet = bundle!!.keySet()
        adapter = KeyAdapter(keySet) { key: String? ->
            if (!TextUtils.isEmpty(key)) {
                val value = bundle!!.getString(key)
                binding!!.tvCustomDataValue.setTextOrHide(value)
            }
        }
        binding!!.rvKeySet.adapter = adapter
    }

    //==============================================================================================
    private class KeyAdapter(keySet: Set<String?>, keySelectListener: KeySelectListener) :
        RecyclerView.Adapter<KeyViewHolder>() {
        private val keySet: MutableList<KeyItem>
        private val keySelectListener: KeySelectListener

        init {
            this.keySet = ArrayList()
            for (key in keySet) {
                this.keySet.add(KeyItem(key))
            }
            this.keySelectListener = keySelectListener
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KeyViewHolder {
            return KeyViewHolder(
                ItemCustomDataKeyBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(holder: KeyViewHolder, position: Int) {
            holder.bind(keySet[position])
            holder.itemView.setOnClickListener {
                for (keyItem in keySet) {
                    keyItem.isSelected = false
                }
                val selectedKey = keySet[position]
                selectedKey.isSelected = true
                notifyDataSetChanged()
                keySelectListener.onKeySelected(selectedKey.text)
            }
        }

        override fun getItemCount(): Int {
            return keySet.size
        }
    }

    private class KeyViewHolder(private val binding: ItemCustomDataKeyBinding) :
        RecyclerView.ViewHolder(binding.getRoot()) {
        fun bind(keyItem: KeyItem) {
            binding.rbItem.text = keyItem.text
            binding.rbItem.setChecked(keyItem.isSelected)
        }
    }

    private class KeyItem(
        val text: String?,
        var isSelected: Boolean = false
    )

    fun interface KeySelectListener {
        fun onKeySelected(key: String?)
    }

    companion object {
        private val TAG = FragmentCustomData::class.java.getSimpleName()
    }
}