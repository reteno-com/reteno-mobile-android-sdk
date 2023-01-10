package com.reteno.sample.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;

import com.reteno.core.domain.model.event.Event;
import com.reteno.core.domain.model.event.Parameter;
import com.reteno.core.util.Logger;
import com.reteno.sample.BaseFragment;
import com.reteno.sample.R;
import com.reteno.sample.databinding.FragmentCustomEventBinding;
import com.reteno.sample.util.Util;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class FragmentCustomEvent extends BaseFragment {

    protected FragmentCustomEventBinding binding;

    public FragmentCustomEvent() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCustomEventBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setListeners();
    }

    private void setListeners() {
        binding.btnSend.setOnClickListener(v -> {
            try {
                sendCustomEvent();
                Toast.makeText(this.getContext(), "Sent", Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(this).popBackStack();
            } catch (Exception e) {
                Toast.makeText(this.getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                Logger.e("FragmentCustomEvent", e.getMessage());
            }
        });

        binding.btnCustomFieldsAdd.setOnClickListener(v -> {
            View view = createNewFields();
            binding.llCustomData.addView(view);
        });

        binding.btnCustomFieldsMinus.setOnClickListener(v -> {
            int countView = binding.llCustomData.getChildCount();
            if (countView > 0) {
                binding.llCustomData.removeViewAt(countView - 1);
            }
        });
    }

    private void sendCustomEvent() {
        String eventTypeKey = Util.getTextOrNull(binding.etEventType);

        if (eventTypeKey == null) {
            Toast.makeText(this.getContext(), "Event type key must be not null", Toast.LENGTH_LONG).show();
            return;
        }
        List<Parameter> params = getUserCustomData();

        getReteno().logEvent(new Event.Custom(eventTypeKey, ZonedDateTime.now(), params));
    }

    private List<Parameter> getUserCustomData() {
        int countView = binding.llCustomData.getChildCount();
        if (countView == 0) return null;

        List<Parameter> list = new ArrayList<>();
        for (int i = 0; i < countView; i++) {
            LinearLayout parent = (LinearLayout) binding.llCustomData.getChildAt(i);

            EditText etKey = (EditText) parent.getChildAt(0);
            EditText etValue = (EditText) parent.getChildAt(1);

            String key = Util.getTextOrNull(etKey);
            String value = Util.getTextOrNull(etValue);

            if (key != null) {
                list.add(new Parameter(key, value));
            }
        }
        return list;
    }

    private View createNewFields() {
        return LayoutInflater
                .from(binding.getRoot().getContext())
                .inflate(R.layout.view_user_custom_fields_horizontal, binding.llCustomData, false);
    }

}