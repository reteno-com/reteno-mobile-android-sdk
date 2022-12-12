package com.reteno.sample.fragments.ecomevents;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reteno.core.domain.model.ecom.EcomEvent;
import com.reteno.sample.databinding.FragmentEcomEventsOrderCancelledBinding;
import com.reteno.sample.util.Util;

public class FragmentEcomEventsOrderCancelled extends BaseEcomEventsFragment {

    private FragmentEcomEventsOrderCancelledBinding binding;


    public FragmentEcomEventsOrderCancelled() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentEcomEventsOrderCancelledBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initListeners();
    }

    private void initListeners() {
        binding.btnSubmit.setOnClickListener(v -> {
            String externalOrderId = Util.getTextOrNull(binding.etExternalOrderId);

            if (externalOrderId == null) {
                Toast.makeText(requireContext(), "ERROR. Required fields are empty or null", Toast.LENGTH_SHORT).show();
                return;
            }

            EcomEvent ecomEvent = new EcomEvent.OrderCancelled(externalOrderId);
            getReteno().logEcommerceEvent(ecomEvent);

            Toast.makeText(requireContext(), "Done", Toast.LENGTH_SHORT).show();
        });
    }
}