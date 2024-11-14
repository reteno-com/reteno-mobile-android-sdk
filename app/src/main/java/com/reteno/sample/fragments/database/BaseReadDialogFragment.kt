package com.reteno.sample.fragments.database

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.viewbinding.ViewBinding
import com.reteno.sample.databinding.DialogDbReadBinding

internal abstract class BaseReadDialogFragment<Model : Any, ViewHolderBinding : ViewBinding, ViewHolder : BaseReadViewHolder<Model, ViewHolderBinding>, Adapter : BaseReadAdapter<Model, ViewHolderBinding, ViewHolder>> :
    BaseDatabaseDialogFragment() {

    @JvmField
    protected var bindingMain: DialogDbReadBinding? = null

    @JvmField
    protected var adapter: Adapter? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        bindingMain = DialogDbReadBinding.inflate(layoutInflater)
        return AlertDialog.Builder(requireActivity())
            .setView(bindingMain!!.getRoot())
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bindingMain = null
    }

    override fun onStart() {
        super.onStart()
        initUi()
    }

    private fun initUi() {
        initAdapter()
        initRecycler()
        initItems()
        initCount()
        initRemoveSection()
    }

    protected abstract fun initAdapter()
    protected fun initRecycler() {
        bindingMain!!.rvList.adapter = adapter
    }

    protected abstract fun initItems()
    protected abstract fun initCount()
    private fun initRemoveSection() {
        bindingMain!!.npRemoveEntries.setWrapSelectorWheel(false)
        bindingMain!!.npRemoveEntries.setMinValue(1)
        bindingMain!!.npRemoveEntries.setMaxValue(Int.MAX_VALUE)
        bindingMain!!.btnRemoveEntries.setOnClickListener { v: View? ->
            val count = bindingMain!!.npRemoveEntries.value
            deleteItems(count)
            initCount()
            initItems()
        }
    }

    protected abstract fun deleteItems(count: Int)
}
