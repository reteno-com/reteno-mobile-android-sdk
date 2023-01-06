package com.reteno.sample.fragments.ecomevents;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reteno.core.domain.model.ecom.EcomEvent;
import com.reteno.core.domain.model.ecom.ProductCategoryView;
import com.reteno.sample.databinding.FragmentEcomEventsProductCategoryViewedBinding;
import com.reteno.sample.util.Util;

public class FragmentEcomEventsProductCategoryViewed extends BaseEcomEventsFragment {

    private FragmentEcomEventsProductCategoryViewedBinding binding;


    public FragmentEcomEventsProductCategoryViewed() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEcomEventsProductCategoryViewedBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initListeners();
    }

    private void initListeners() {
        binding.btnSubmit.setOnClickListener(v -> {
            String productCategoryId = Util.getTextOrNull(binding.etProductCategoryId);
            if (productCategoryId == null) {
                Toast.makeText(requireContext(), "ERROR. Required fields are empty or null", Toast.LENGTH_SHORT).show();
                return;
            }

            ProductCategoryView productCategoryView = new ProductCategoryView(
                    productCategoryId,
                    getCustomAttributes(binding.llCustomAttributes));
            EcomEvent ecomEvent = new EcomEvent.ProductCategoryViewed(productCategoryView);
            getReteno().logEcommerceEvent(ecomEvent);

            Toast.makeText(requireContext(), "Done", Toast.LENGTH_SHORT).show();
        });

        binding.btnCustomAttributePlus.setOnClickListener(v -> {
            View view = createNewFields(binding, binding.llCustomAttributes);
            binding.llCustomAttributes.addView(view);
        });

        binding.btnCustomAttributeMinus.setOnClickListener(v -> {
            int countView = binding.llCustomAttributes.getChildCount();
            if (countView > 0) {
                binding.llCustomAttributes.removeViewAt(countView - 1);
            }
        });
    }
}