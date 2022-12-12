package com.reteno.sample.fragments.ecomevents;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reteno.core.domain.model.ecom.EcomEvent;
import com.reteno.core.domain.model.ecom.ProductInCart;
import com.reteno.sample.databinding.FragmentEcomEventsCartUpdatedBinding;
import com.reteno.sample.util.Util;

import java.util.ArrayList;
import java.util.List;

public class FragmentEcomEventsCartUpdated extends BaseEcomEventsFragment {

    private FragmentEcomEventsCartUpdatedBinding binding;


    public FragmentEcomEventsCartUpdated() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentEcomEventsCartUpdatedBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initListeners();
    }

    private void initListeners() {
        binding.btnSubmit.setOnClickListener(v -> {
            String cartId = Util.getTextOrNull(binding.etCartId);
            String productId = Util.getTextOrNull(binding.etProductId);

            String quantityString = Util.getTextOrNull(binding.etQuantity);
            Integer quantity = null;
            if (quantityString != null) {
                quantity = Integer.valueOf(quantityString);
            }

            String priceString = Util.getTextOrNull(binding.etPrice);
            Double price = null;
            if (priceString != null) {
                price = Double.valueOf(priceString);
            }

            String discountString = Util.getTextOrNull(binding.etDiscount);
            Double discount = null;
            if (discountString != null) {
                discount = Double.valueOf(discountString);
            }

            if (cartId == null
                    || productId == null
                    || quantity == null
                    || price == null) {
                Toast.makeText(requireContext(), "ERROR. Required fields are empty or null", Toast.LENGTH_SHORT).show();
                return;
            }

            ProductInCart productInCart = new ProductInCart(
                    productId,
                    quantity,
                    price,
                    discount,
                    Util.getTextOrNull(binding.etName),
                    Util.getTextOrNull(binding.etCategory),
                    getCustomAttributes(binding.llCustomAttributes));
            List<ProductInCart> productInCartList = new ArrayList<>();
            productInCartList.add(productInCart);

            EcomEvent ecomEvent = new EcomEvent.CartUpdated(cartId, productInCartList, Util.getTextOrNull(binding.etCurrencyCode));
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