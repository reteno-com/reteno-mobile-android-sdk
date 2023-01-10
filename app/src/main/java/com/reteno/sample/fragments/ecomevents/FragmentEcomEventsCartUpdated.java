package com.reteno.sample.fragments.ecomevents;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import com.reteno.core.domain.model.ecom.EcomEvent;
import com.reteno.core.domain.model.ecom.ProductInCart;
import com.reteno.sample.databinding.FragmentEcomEventsCartUpdatedBinding;
import com.reteno.sample.databinding.ViewEcomEventsCartItemBinding;
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
        binding.btnProductsPlus.setOnClickListener(v -> {
            ViewEcomEventsCartItemBinding productBinding = createNewProduct(binding, binding.llProducts);
            setupSingleProductView(productBinding);
            binding.llProducts.addView(productBinding.getRoot());
        });

        binding.btnProductsMinus.setOnClickListener(v -> {
            int countView = binding.llProducts.getChildCount();
            if (countView > 0) {
                binding.llProducts.removeViewAt(countView - 1);
            }
        });

        binding.btnSubmit.setOnClickListener(v -> {
            String cartId = Util.getTextOrNull(binding.etCartId);

            try {
                List<ProductInCart> products = getProducts(binding.llProducts);
                if (cartId == null || products == null) {
                    throw new IllegalArgumentException("ERROR. Cart ID should not be empty or null. Nothing done");
                }
                EcomEvent ecomEvent = new EcomEvent.CartUpdated(cartId, products, Util.getTextOrNull(binding.etCurrencyCode));
                getReteno().logEcommerceEvent(ecomEvent);

                Toast.makeText(requireContext(), "Done", Toast.LENGTH_SHORT).show();
            } catch (IllegalArgumentException iae) {
                Toast.makeText(requireContext(), "ERROR. Required fields are empty or null", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private ViewEcomEventsCartItemBinding createNewProduct(ViewBinding viewBinding, LinearLayout container) {
        LayoutInflater layoutInflater = LayoutInflater.from(viewBinding.getRoot().getContext());
        return ViewEcomEventsCartItemBinding.inflate(layoutInflater, container, false);
    }

    private void setupSingleProductView(ViewEcomEventsCartItemBinding productBinding) {
        productBinding.btnCustomAttributePlus.setOnClickListener(v -> {
            View view = createNewFields(binding, productBinding.llCustomAttributes);
            productBinding.llCustomAttributes.addView(view);
        });

        productBinding.btnCustomAttributeMinus.setOnClickListener(v -> {
            int countView = productBinding.llCustomAttributes.getChildCount();
            if (countView > 0) {
                productBinding.llCustomAttributes.removeViewAt(countView - 1);
            }
        });
    }

    private List<ProductInCart> getProducts(LinearLayout llProductLayout) throws IllegalArgumentException {
        int countView = llProductLayout.getChildCount();
        if (countView == 0) return null;

        List<ProductInCart> list = new ArrayList<>();
        for (int i = 0; i < countView; i++) {
            LinearLayout parent = (LinearLayout) llProductLayout.getChildAt(i);

            EditText etProductId = (EditText) parent.getChildAt(0);
            EditText etQuantity = (EditText) parent.getChildAt(1);
            EditText etPrice = (EditText) parent.getChildAt(2);
            EditText etDiscount = (EditText) parent.getChildAt(3);
            EditText etName = (EditText) parent.getChildAt(4);
            EditText etCategory = (EditText) parent.getChildAt(5);
            LinearLayout llCustomAttributes = (LinearLayout) parent.getChildAt(7);

            String productId = Util.getTextOrNull(etProductId);

            String quantityString = Util.getTextOrNull(etQuantity);
            Integer quantity = null;
            if (quantityString != null) {
                quantity = Integer.valueOf(quantityString);
            }

            String priceString = Util.getTextOrNull(etPrice);
            Double price = null;
            if (priceString != null) {
                price = Double.valueOf(priceString);
            }

            String discountString = Util.getTextOrNull(etDiscount);
            Double discount = null;
            if (discountString != null) {
                discount = Double.valueOf(discountString);
            }

            String productName = Util.getTextOrNull(etName);

            String productCategory = Util.getTextOrNull(etCategory);

            if (productId == null || quantity == null || price == null) {
                throw new IllegalArgumentException("ERROR. Required fields are empty or null");
            }

            ProductInCart productInCart = new ProductInCart(
                    productId,
                    quantity,
                    price,
                    discount,
                    productName,
                    productCategory,
                    getCustomAttributes(llCustomAttributes));
            list.add(productInCart);
        }
        return list;
    }
}