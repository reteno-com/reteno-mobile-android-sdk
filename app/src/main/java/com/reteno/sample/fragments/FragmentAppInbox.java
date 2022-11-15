package com.reteno.sample.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.reteno.core.domain.callback.appinbox.AppInboxMessagesCallback;
import com.reteno.core.domain.callback.appinbox.AppInboxMessagesCountCallback;
import com.reteno.core.domain.model.appinbox.AppInboxMessage;
import com.reteno.sample.BaseFragment;
import com.reteno.sample.databinding.FragmentAppInboxBinding;
import com.reteno.sample.util.Util;

import java.util.List;

public class FragmentAppInbox extends BaseFragment {

    protected FragmentAppInboxBinding binding;
    private String messageId = null;

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

        setListeners();
        loadMessagesCount();
    }

    private void setListeners() {
        binding.btnGetMessage.setOnClickListener(v -> {
            loadMessage();
        });

        binding.btnMarkAsOpened.setOnClickListener(v -> {
            if (messageId != null) {
                getReteno().getAppInboxMessaging().markAsOpened(messageId);
            }
        });

        binding.btnMarkAllAsOpened.setOnClickListener(v -> {
            getReteno().getAppInboxMessaging().markAllMessagesAsOpened();
        });

        binding.btnObserveCount.setOnClickListener(v -> {
            observeMessageCount();
        });

    }

    private void loadMessage() {
        Integer page = Util.saveParseInt(Util.getTextOrNull(binding.etPage));
        Integer pageSize = Util.saveParseInt(Util.getTextOrNull(binding.etPageSize));
        getReteno().getAppInboxMessaging().getAppInboxMessages(page, pageSize, new AppInboxMessagesCallback() {
            @Override
            public void onSuccess(@NonNull List<AppInboxMessage> messages, int totalPages) {
                if (!messages.isEmpty()) {
                    setMessageData(messages.get(0));
                }
                binding.btnMarkAsOpened.setEnabled(!messages.isEmpty());
            }

            @Override
            public void onFailure(@Nullable Integer statusCode, @Nullable String response, @Nullable Throwable throwable) {
                String msg = "Error " + statusCode + " " + response + " " + throwable.getMessage();
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMessagesCount() {
        getReteno().getAppInboxMessaging().getAppInboxMessagesCount(new AppInboxMessagesCountCallback() {
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
        getReteno().getAppInboxMessaging().observeAppInboxMessagesCount(new AppInboxMessagesCountCallback() {
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

    private void setMessageData(AppInboxMessage appInboxMessage) {
        messageId = appInboxMessage.getId();
        binding.tvTitle.setText(appInboxMessage.getTitle());
        binding.tvData.setText(appInboxMessage.getCreatedDate());
        binding.tvContent.setText(appInboxMessage.getContent());
        if (appInboxMessage.isNewMessage()) {
            binding.tvNew.setVisibility(View.VISIBLE);
        } else {
            binding.tvNew.setVisibility(View.GONE);
        }

        if (appInboxMessage.getLinkUrl() != null && !appInboxMessage.getLinkUrl().isEmpty()) {
            binding.cardView.setOnClickListener(v -> {
                String url = appInboxMessage.getLinkUrl();
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            });
        }

        if (appInboxMessage.getImageUrl() != null && !appInboxMessage.getImageUrl().isEmpty()) {
            Glide.with(binding.imageView)
                    .load(appInboxMessage.getImageUrl())
                    .into(binding.imageView);
        }
    }


}