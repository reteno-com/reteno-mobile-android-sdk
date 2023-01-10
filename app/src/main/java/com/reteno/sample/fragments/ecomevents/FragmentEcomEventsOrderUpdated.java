package com.reteno.sample.fragments.ecomevents;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.reteno.core.domain.model.ecom.EcomEvent;
import com.reteno.core.domain.model.ecom.Order;
import com.reteno.sample.databinding.FragmentEcomEventsOrderUpsertBinding;
import com.reteno.sample.util.Util;

public class FragmentEcomEventsOrderUpdated extends FragmentEcomEventsOrderCreated {

    public FragmentEcomEventsOrderUpdated() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentEcomEventsOrderUpsertBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    protected void logEvent(Order order) {
        EcomEvent ecomEvent = new EcomEvent.OrderUpdated(order, Util.getTextOrNull(binding.etCurrencyCode));
        getReteno().logEcommerceEvent(ecomEvent);
    }
}