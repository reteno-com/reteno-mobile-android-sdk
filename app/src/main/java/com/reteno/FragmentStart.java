package com.reteno;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;

import com.reteno.sample.R;
import com.reteno.sample.databinding.FragmentStartBinding;
import com.reteno.testscreens.ScreenAdapter;
import com.reteno.testscreens.ScreenItem;

import java.util.ArrayList;
import java.util.List;

public class FragmentStart extends BaseFragment {

    private FragmentStartBinding binding;

    public FragmentStart() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStartBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ScreenAdapter adapter = new ScreenAdapter(getScreenList(), new ScreenAdapter.ScreenItemClick() {
            @Override
            public void NavigateById(int fragmentId) {
                NavHostFragment.findNavController(FragmentStart.this).navigate(fragmentId);
            }

            @Override
            public void navigateByDirections(NavDirections navDirections) {
                NavHostFragment.findNavController(FragmentStart.this).navigate(navDirections);
            }
        });

        binding.recycler.setAdapter(adapter);
    }

    private List<ScreenItem> getScreenList() {
        List<ScreenItem> screens = new ArrayList<>();

        screens.add(new ScreenItem("Device Id", FragmentStartDirections.startToDeviceId()));

        return screens;
    }
}