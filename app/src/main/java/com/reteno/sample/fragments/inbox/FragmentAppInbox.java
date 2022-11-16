package com.reteno.sample.fragments.inbox;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reteno.core.domain.callback.appinbox.AppInboxMessagesCallback;
import com.reteno.core.domain.callback.appinbox.AppInboxMessagesCountCallback;
import com.reteno.core.domain.model.appinbox.AppInboxMessage;
import com.reteno.sample.BaseFragment;
import com.reteno.sample.databinding.FragmentAppInboxBinding;
import com.reteno.sample.util.Util;

import java.util.List;

public class FragmentAppInbox extends BaseFragment {

    protected FragmentAppInboxBinding binding;
    private InboxMessageAdapter adapter = null;

    public FragmentAppInbox() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAppInboxBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setUpView();
        setListeners();
        loadMessagesCount();
    }

    private void setUpView() {
        adapter = new InboxMessageAdapter(new InboxMessageAdapter.InboxItemClick() {
            @Override
            public void onOpenedClicked(String messageId) {
                getReteno().getAppInbox().markAsOpened(messageId);
            }

            @Override
            public void onMessageClicked(String url) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            }
        });
        binding.rvInboxMessages.setAdapter(adapter);
    }

    private void setListeners() {
        binding.btnGetMessage.setOnClickListener(v -> {
            loadMessage();
        });

        binding.btnMarkAllAsOpened.setOnClickListener(v -> {
            getReteno().getAppInbox().markAllMessagesAsOpened();
        });

        binding.btnObserveCount.setOnClickListener(v -> {
            observeMessageCount();
        });

    }

    private void loadMessage() {
        Integer page = Util.saveParseInt(Util.getTextOrNull(binding.etPage));
        Integer pageSize = Util.saveParseInt(Util.getTextOrNull(binding.etPageSize));
        getReteno().getAppInbox().getAppInboxMessages(page, pageSize, new AppInboxMessagesCallback() {
            @Override
            public void onSuccess(@NonNull List<AppInboxMessage> messages, int totalPages) {
                adapter.submitList(messages);
            }

            @Override
            public void onFailure(@Nullable Integer statusCode, @Nullable String response, @Nullable Throwable throwable) {
                String msg = "Error " + statusCode + " " + response + " " + throwable.getMessage();
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMessagesCount() {
        getReteno().getAppInbox().getAppInboxMessagesCount(new AppInboxMessagesCountCallback() {
            @Override
            public void onSuccess(int count) {
                binding.textView.setText("AppInbox messages count: " + count);
            }

            @Override
            public void onFailure(@Nullable Integer statusCode, @Nullable String response, @Nullable Throwable throwable) {
                String msg = "Error " + statusCode + " " + response + " " + throwable.getMessage();
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void observeMessageCount() {
        getReteno().getAppInbox().observeAppInboxMessagesCount(new AppInboxMessagesCountCallback() {
            @Override
            public void onSuccess(int count) {
                binding.textView.setText("AppInbox messages count: " + count);
            }

            @Override
            public void onFailure(@Nullable Integer statusCode, @Nullable String response, @Nullable Throwable throwable) {
                String msg = "Error " + statusCode + " " + response + " " + throwable.getMessage();
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

}