package com.reteno.sample.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;

import com.reteno.sample.BaseFragment;
import com.reteno.sample.R;
import com.reteno.sample.databinding.FragmentStartBinding;
import com.reteno.sample.testscreens.ScreenAdapter;
import com.reteno.sample.testscreens.ScreenItem;

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
            public void navigateById(int fragmentId) {
                NavHostFragment.findNavController(FragmentStart.this).navigate(fragmentId);
            }

            @Override
            public void navigateById(int fragmentId, Bundle bundle) {
                NavHostFragment.findNavController(FragmentStart.this).navigate(fragmentId, bundle);
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
        screens.add(new ScreenItem("Sentry", FragmentStartDirections.startToSentry()));
        screens.add(new ScreenItem("Second Activity", FragmentStartDirections.startToActivitySecond()));
        screens.add(new ScreenItem("User data", FragmentStartDirections.startToUserData()));
        screens.add(new ScreenItem("Custom Data", R.id.start_to_custom_data, getArguments()));
        screens.add(new ScreenItem("Database", FragmentStartDirections.startToDatabase()));
        screens.add(new ScreenItem("Custom event", FragmentStartDirections.startToCustomEvent()));
        screens.add(new ScreenItem("Force push", FragmentStartDirections.startToForcePush()));
        screens.add(new ScreenItem("Screen tracking", FragmentStartDirections.startToScreenTracking()));
        screens.add(new ScreenItem("App Inbox", FragmentStartDirections.startToAppInbox()));

        return screens;
    }
}