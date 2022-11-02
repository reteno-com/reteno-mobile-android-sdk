package com.reteno.sample.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.reteno.sample.databinding.FragmentCustomDataBinding;
import com.reteno.sample.databinding.ItemCustomDataKeyBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FragmentCustomData extends Fragment {

    private FragmentCustomDataBinding binding;

    private Bundle bundle;
    private KeyAdapter adapter;

    public FragmentCustomData() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCustomDataBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (checkBundle(view)) {
            fillKeySet();
        }
    }

    private boolean checkBundle(View view) {
        bundle = getArguments();
        if (bundle == null) {
            Toast.makeText(getContext(), "No custom data found", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).navigateUp();
            return false;
        } else {
            return true;
        }
    }

    private void fillKeySet() {
        Set<String> keySet = bundle.keySet();
        adapter = new KeyAdapter(keySet, key -> {
            if (!TextUtils.isEmpty(key)) {
                String value = bundle.getString(key);
                binding.tvCustomDataValue.setTextOrHide(value);
            }
        });
        binding.rvKeySet.setAdapter(adapter);
    }

    //==============================================================================================

    private static class KeyAdapter extends RecyclerView.Adapter<KeyViewHolder> {

        private List<KeyItem> keySet;
        private KeySelectListener keySelectListener;

        public KeyAdapter(Set<String> keySet, KeySelectListener keySelectListener) {
            this.keySet = new ArrayList<>();
            for (String key : keySet) {
                this.keySet.add(new KeyItem(key));
            }
            this.keySelectListener = keySelectListener;
        }

        @NonNull
        @Override
        public KeyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new KeyViewHolder(ItemCustomDataKeyBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull KeyViewHolder holder, int position) {
            holder.bind(keySet.get(position));
            holder.itemView.setOnClickListener(v -> {
                for (KeyItem keyItem : keySet) {
                    keyItem.setSelected(false);
                }
                KeyItem selectedKey = keySet.get(position);
                selectedKey.setSelected(true);
                notifyDataSetChanged();

                keySelectListener.onKeySelected(selectedKey.text);
            });
        }

        @Override
        public int getItemCount() {
            return keySet.size();
        }

        String getSelectedKey() {
            for (KeyItem keyItem : keySet) {
                if (keyItem.isSelected) {
                    return keyItem.text;
                }
            }
            return "";
        }
    }

    private static class KeyViewHolder extends RecyclerView.ViewHolder {
        private final ItemCustomDataKeyBinding binding;

        KeyViewHolder(ItemCustomDataKeyBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(KeyItem keyItem) {
            binding.rbItem.setText(keyItem.text);
            binding.rbItem.setChecked(keyItem.isSelected);
        }
    }

    private static class KeyItem {
        private String text;
        private boolean isSelected;

        KeyItem(String text) {
            this.text = text;
        }

        public boolean isSelected() {
            return isSelected;
        }

        public void setSelected(boolean selected) {
            isSelected = selected;
        }
    }

    private interface KeySelectListener {
        void onKeySelected(String key);
    }
}