package com.reteno.sample.fragments.ecomevents;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;

import com.reteno.sample.BaseFragment;
import com.reteno.sample.R;
import com.reteno.sample.databinding.FragmentEcomEventsBinding;

public class FragmentEcomEvents extends BaseFragment {

    private FragmentEcomEventsBinding binding;

    public FragmentEcomEvents() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentEcomEventsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initListeners();
    }

    private void initListeners() {
        binding.btnProductViewed.setOnClickListener(v -> {
            navigateTo(R.id.ecomEvents_to_ProductViewed);
        });
        binding.btnProductCategoryViewed.setOnClickListener(v -> {
            navigateTo(R.id.ecomEvents_to_ProductCategoryViewed);
        });
        binding.btnProductAddedToWishlist.setOnClickListener(v -> {
            navigateTo(R.id.ecomEvents_to_ProductAddedToWishlist);
        });
        binding.btnCartUpdated.setOnClickListener(v -> {
            navigateTo(R.id.ecomEvents_to_CartUpdated);
        });
        binding.btnOrderCreated.setOnClickListener(v -> {
            navigateTo(R.id.ecomEvents_to_OrderCreated);
        });
        binding.btnOrderUpdated.setOnClickListener(v -> {
            navigateTo(R.id.ecomEvents_to_OrderUpdated);
        });
        binding.btnOrderDelivered.setOnClickListener(v -> {
            navigateTo(R.id.ecomEvents_to_OrderDelivered);

        });
        binding.btnOrderCancelled.setOnClickListener(v -> {
            navigateTo(R.id.ecomEvents_to_OrderCancelled);
        });
        binding.btnSearchRequest.setOnClickListener(v -> {
            navigateTo(R.id.ecomEvents_to_SearchRequest);
        });
    }

    private void navigateTo(int fragmentId) {
        NavHostFragment.findNavController(this).navigate(fragmentId);
    }
}