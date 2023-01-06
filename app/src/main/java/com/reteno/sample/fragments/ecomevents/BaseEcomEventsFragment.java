package com.reteno.sample.fragments.ecomevents;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.viewbinding.ViewBinding;

import com.reteno.core.domain.model.ecom.Attributes;
import com.reteno.sample.BaseFragment;
import com.reteno.sample.R;
import com.reteno.sample.util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class BaseEcomEventsFragment extends BaseFragment {

    protected View createNewFields(ViewBinding viewBinding, LinearLayout container) {
        return LayoutInflater
                .from(viewBinding.getRoot().getContext())
                .inflate(R.layout.view_user_custom_fields_horizontal, container, false);
    }

    protected List<Attributes> getCustomAttributes(LinearLayout llCustomAttributes) {
        int countView = llCustomAttributes.getChildCount();
        if (countView == 0) return null;

        List<Attributes> list = new ArrayList<>();
        for (int i = 0; i < countView; i++) {
            LinearLayout parent = (LinearLayout) llCustomAttributes.getChildAt(i);

            EditText etKey = (EditText) parent.getChildAt(0);
            EditText etValue = (EditText) parent.getChildAt(1);

            String key = Util.getTextOrNull(etKey);
            String value = Util.getTextOrNull(etValue);

            if (key != null && value != null) {
                List<String> valueList = Arrays.asList(value.split(","));
                list.add(new Attributes(key, valueList));
            }
        }
        return list;
    }
}
