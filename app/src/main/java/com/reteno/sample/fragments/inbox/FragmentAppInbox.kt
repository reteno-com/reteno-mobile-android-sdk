package com.reteno.sample.fragments.inbox

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import com.reteno.core.domain.callback.appinbox.RetenoResultCallback
import com.reteno.core.domain.model.appinbox.AppInboxMessages
import com.reteno.core.features.appinbox.AppInboxStatus
import com.reteno.core.features.iam.InAppPauseBehaviour
import com.reteno.sample.BaseFragment
import com.reteno.sample.databinding.FragmentAppInboxBinding
import com.reteno.sample.fragments.inbox.InboxMessageAdapter.InboxItemClick
import com.reteno.sample.util.Util

class FragmentAppInbox : BaseFragment() {

    private var binding: FragmentAppInboxBinding? = null
    private var adapter: InboxMessageAdapter? = null
    private var selectedStatus: AppInboxStatus? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAppInboxBinding.inflate(inflater, container, false)
        return binding!!.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpView()
        setListeners()
        loadMessagesCount()
    }

    private fun setUpView() {
        adapter = InboxMessageAdapter(object : InboxItemClick {
            override fun onOpenedClicked(messageId: String) {
                reteno.appInbox.markAsOpened(messageId)
            }

            override fun onMessageClicked(url: String) {
                val i = Intent(Intent.ACTION_VIEW)
                i.setData(Uri.parse(url))
                startActivity(i)
            }
        })
        binding!!.rvInboxMessages.adapter = adapter

        val items = listOf(
            null,
            *AppInboxStatus.values()
        )
        binding!!.spinnerStatus.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            items
        )
        binding!!.spinnerStatus.setSelection(0)
        binding!!.spinnerStatus.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View?,
                    i: Int,
                    l: Long
                ) {
                    selectedStatus = items[i]
                }

                override fun onNothingSelected(adapterView: AdapterView<*>?) {}
            }
    }

    private fun setListeners() {
        binding!!.btnGetMessage.setOnClickListener { loadMessage() }
        binding!!.btnMarkAllAsOpened.setOnClickListener {
            reteno.appInbox.markAllMessagesAsOpened(object : RetenoResultCallback<Unit> {
                override fun onSuccess(result: Unit) {
                    Toast.makeText(
                        requireContext(),
                        "Mark All Messages As Opened",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
                    val msg = "Error " + statusCode + " " + response + " " + throwable!!.message
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            })
        }
        binding!!.btnObserveCount.setOnClickListener { v: View? -> observeMessageCount() }
    }

    private fun loadMessage() {
        val page = Util.saveParseInt(
            Util.getTextOrNull(
                binding!!.etPage
            )
        )
        val pageSize = Util.saveParseInt(
            Util.getTextOrNull(
                binding!!.etPageSize
            )
        )
        reteno.appInbox.getAppInboxMessages(
            page,
            pageSize,
            selectedStatus,
            object : RetenoResultCallback<AppInboxMessages> {
                override fun onSuccess(result: AppInboxMessages) {
                    adapter!!.submitList(result.messages)
                }

                override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
                    val msg = "Error " + statusCode + " " + response + " " + throwable!!.message
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun loadMessagesCount() {
        reteno.appInbox.getAppInboxMessagesCount(object : RetenoResultCallback<Int> {
            override fun onSuccess(result: Int) {
                binding!!.textView.text = "AppInbox messages count: $result"
            }

            override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
                val msg = "Error " + statusCode + " " + response + " " + throwable!!.message
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun observeMessageCount() {
        reteno.appInbox.subscribeOnMessagesCountChanged(object : RetenoResultCallback<Int> {
            override fun onSuccess(result: Int) {
                binding!!.textView.text = "AppInbox messages count: $result"
            }

            override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
                val msg = "Error " + statusCode + " " + response + " " + throwable!!.message
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding!!.rvInboxMessages.adapter = null
        reteno.appInbox.unsubscribeAllMessagesCountChanged()
    }
}