package com.reteno.sample.fragments.ecomevents;

import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import com.reteno.core.domain.model.ecom.EcomEvent;
import com.reteno.core.domain.model.ecom.Order;
import com.reteno.core.domain.model.ecom.OrderItem;
import com.reteno.core.domain.model.ecom.OrderStatus;
import com.reteno.sample.R;
import com.reteno.sample.databinding.FragmentEcomEventsOrderUpsertBinding;
import com.reteno.sample.util.Util;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class FragmentEcomEventsOrderCreated extends BaseEcomEventsFragment {

    protected FragmentEcomEventsOrderUpsertBinding binding;

    public FragmentEcomEventsOrderCreated() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentEcomEventsOrderUpsertBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.spnStatus.setAdapter(new ArrayAdapter<>(view.getContext(), android.R.layout.simple_spinner_item, OrderStatus.values()));
        binding.spnStatus.setSelection(0);
        binding.etDate.setText(ZonedDateTime.now().toString());
        initListeners();
    }

    private void initListeners() {
        binding.btnSubmit.setOnClickListener(v -> {
            String externalOrderId = Util.getTextOrNull(binding.etExternalOrderId);
            String totalCost = Util.getTextOrNull(binding.etTotalCost);
            OrderStatus orderStatus = OrderStatus.valueOf(binding.spnStatus.getSelectedItem().toString());
            String date = Util.getTextOrNull(binding.etDate);

            if (externalOrderId == null
                    || totalCost == null
                    || orderStatus == null
                    || date == null) {
                Toast.makeText(requireContext(), "ERROR. Required fields are empty or null", Toast.LENGTH_SHORT).show();
                return;
            }

            String shippingString = Util.getTextOrNull(binding.etShipping);
            Double shipping = null;
            if (shippingString != null) {
                shipping = Double.valueOf(shippingString);
            }

            String discountString = Util.getTextOrNull(binding.etDiscount);
            Double discount = null;
            if (discountString != null) {
                discount = Double.valueOf(discountString);
            }

            String taxesString = Util.getTextOrNull(binding.etTaxes);
            Double taxes = null;
            if (taxesString != null) {
                taxes = Double.valueOf(taxesString);
            }

            List<OrderItem> orderItems = getOrderItems(binding.llOrderItems);
            Order.Builder orderBuilder = new Order.Builder(
                    externalOrderId,
                    Util.getTextOrNull(binding.etExternalCustomerId),
                    Double.valueOf(totalCost),
                    orderStatus,
                    ZonedDateTime.parse(date)
            );
            orderBuilder.setCartId(Util.getTextOrNull(binding.etCartId));
            orderBuilder.setEmail(Util.getTextOrNull(binding.etEmail));
            orderBuilder.setPhone(Util.getTextOrNull(binding.etPhone));
            orderBuilder.setFirstName(Util.getTextOrNull(binding.etFirstName));
            orderBuilder.setLastName(Util.getTextOrNull(binding.etLastName));
            orderBuilder.setShipping(shipping);
            orderBuilder.setDiscount(discount);
            orderBuilder.setTaxes(taxes);
            orderBuilder.setRestoreUrl(Util.getTextOrNull(binding.etRestoreUrl));
            orderBuilder.setStatusDescription(Util.getTextOrNull(binding.etStatusDescription));
            orderBuilder.setStoreId(Util.getTextOrNull(binding.etStoreId));
            orderBuilder.setSource(Util.getTextOrNull(binding.etSource));
            orderBuilder.setDeliveryMethod(Util.getTextOrNull(binding.etDeliveryMethod));
            orderBuilder.setPaymentMethod(Util.getTextOrNull(binding.etPaymentMethod));
            orderBuilder.setDeliveryAddress(Util.getTextOrNull(binding.etDeliveryAddress));
            orderBuilder.setItems(orderItems);
            orderBuilder.setAttributes(getOrderCustomAttributes(binding.llCustomAttributes));

            logEvent(orderBuilder.build());
            Toast.makeText(requireContext(), "Done", Toast.LENGTH_SHORT).show();
        });

        binding.btnCustomAttributePlus.setOnClickListener(v2 -> {
            View view = createNewFields(binding, binding.llCustomAttributes);
            binding.llCustomAttributes.addView(view);
        });

        binding.btnCustomAttributeMinus.setOnClickListener(v2 -> {
            int countView = binding.llCustomAttributes.getChildCount();
            if (countView > 0) {
                binding.llCustomAttributes.removeViewAt(countView - 1);
            }
        });

        binding.btnOrderItemsPlus.setOnClickListener(v -> {
            View view = createNewOrderItem(binding, binding.llOrderItems);
            binding.llOrderItems.addView(view);
        });
        binding.btnOrderItemsMinus.setOnClickListener(v -> {
            int countView = binding.llOrderItems.getChildCount();
            if (countView > 0) {
                binding.llOrderItems.removeViewAt(countView - 1);
            }
        });
    }

    protected void logEvent(Order order) {
        EcomEvent ecomEvent = new EcomEvent.OrderCreated(order, Util.getTextOrNull(binding.etCurrencyCode));
        getReteno().logEcommerceEvent(ecomEvent);
    }

    private View createNewOrderItem(@NonNull ViewBinding viewBinding, LinearLayout container) {
        LayoutInflater layoutInflater = LayoutInflater.from(viewBinding.getRoot().getContext());
        return layoutInflater.inflate(R.layout.view_ecom_events_order_items, container, false);
    }

    private List<OrderItem> getOrderItems(LinearLayout llOrderItems) {
        int countView = llOrderItems.getChildCount();
        if (countView == 0) return null;

        List<OrderItem> list = new ArrayList<>();
        for (int i = 0; i < countView; i++) {
            LinearLayout parent = (LinearLayout) llOrderItems.getChildAt(i);

            EditText etExternalItemId = (EditText) parent.getChildAt(0);
            EditText etName = (EditText) parent.getChildAt(1);
            EditText etCategory = (EditText) parent.getChildAt(2);
            EditText etQuantity = (EditText) parent.getChildAt(3);
            EditText etCost = (EditText) parent.getChildAt(4);
            EditText etUrl = (EditText) parent.getChildAt(5);
            EditText etImageUrl = (EditText) parent.getChildAt(6);
            EditText etDescription = (EditText) parent.getChildAt(7);

            String externalItemId = Util.getTextOrNull(etExternalItemId);
            String name = Util.getTextOrNull(etName);
            String category = Util.getTextOrNull(etCategory);
            String quantity = Util.getTextOrNull(etQuantity);
            String cost = Util.getTextOrNull(etCost);
            String url = Util.getTextOrNull(etUrl);
            String imageUrl = Util.getTextOrNull(etImageUrl);
            String description = Util.getTextOrNull(etDescription);

            if (externalItemId != null
                    && name != null
                    && category != null
                    && quantity != null
                    && cost != null
                    && url != null) {
                list.add(new OrderItem(
                                externalItemId,
                                name,
                                category,
                                Double.valueOf(quantity),
                                Double.valueOf(cost),
                                url,
                                imageUrl,
                                description
                        )
                );
            }
        }
        return list;
    }

    private List<Pair<String, String>> getOrderCustomAttributes(LinearLayout llCustomAttributes) {
        int countView = llCustomAttributes.getChildCount();
        if (countView == 0) return null;

        List<Pair<String, String>> list = new ArrayList<>();
        for (int i = 0; i < countView; i++) {
            LinearLayout parent = (LinearLayout) llCustomAttributes.getChildAt(i);

            EditText etKey = (EditText) parent.getChildAt(0);
            EditText etValue = (EditText) parent.getChildAt(1);

            String key = Util.getTextOrNull(etKey);
            String value = Util.getTextOrNull(etValue);

            if (key != null && value != null) {
                list.add(new Pair<String, String>(key, value));
            }
        }
        return list;
    }
}