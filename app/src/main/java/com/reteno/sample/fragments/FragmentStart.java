package com.reteno.sample.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;

import com.reteno.core.RetenoImpl;
import com.reteno.core.di.ServiceLocator;
import com.reteno.core.features.iam.InAppPauseBehaviour;
import com.reteno.core.lifecycle.RetenoSessionHandler;
import com.reteno.sample.BaseFragment;
import com.reteno.sample.R;
import com.reteno.sample.databinding.FragmentStartBinding;
import com.reteno.sample.testscreens.ScreenAdapter;
import com.reteno.sample.testscreens.ScreenItem;
import com.reteno.sample.util.FragmentStartSessionListener;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class FragmentStart extends BaseFragment {

    private FragmentStartBinding binding;
    private ServiceLocator serviceLocator;
    private RetenoSessionHandler sessionHandler;
    private FragmentStartSessionListener sessionListener = new FragmentStartSessionListener();


    public FragmentStart() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Field field = RetenoImpl.class.getDeclaredField("serviceLocator");
            field.setAccessible(true);
            serviceLocator = (ServiceLocator) field.get(getReteno());
            field.setAccessible(false);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        sessionHandler = serviceLocator.getRetenoSessionHandlerProvider().get();
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
        initInAppPausingSwitcher();
        initPauseBehaviourSwitcher();
    }

    @Override
    public void onPause() {
        super.onPause();
        sessionListener.stop();
    }

    @Override
    public void onResume() {
        super.onResume();
        sessionListener.start(sessionHandler, binding.tvSessionTime);
    }

    private List<ScreenItem> getScreenList() {
        List<ScreenItem> screens = new ArrayList<>();

        screens.add(new ScreenItem("Device Id", FragmentStartDirections.startToDeviceId()));
        screens.add(new ScreenItem("Sentry", FragmentStartDirections.startToSentry()));
        screens.add(new ScreenItem("Second Activity", FragmentStartDirections.startToActivitySecond()));
        screens.add(new ScreenItem("User data", FragmentStartDirections.startToUserData()));
        screens.add(new ScreenItem("User Anonymous data", FragmentStartDirections.startToUserAnonymousData()));
        screens.add(new ScreenItem("Custom Data", R.id.start_to_custom_data, getArguments()));
        screens.add(new ScreenItem("Database", FragmentStartDirections.startToDatabase()));
        screens.add(new ScreenItem("Custom event", FragmentStartDirections.startToCustomEvent()));
        screens.add(new ScreenItem("App lifecycle events", FragmentStartDirections.startToAppLifecycleEvents()));
        screens.add(new ScreenItem("Force push", FragmentStartDirections.startToForcePush()));
        screens.add(new ScreenItem("Screen tracking", FragmentStartDirections.startToScreenTracking()));
        screens.add(new ScreenItem("App Inbox", FragmentStartDirections.startToAppInbox()));
        screens.add(new ScreenItem("Recommendations GET", FragmentStartDirections.startToRecommendationsGet()));
        screens.add(new ScreenItem("Recommendations POST", FragmentStartDirections.startToRecommendationsPost()));
        screens.add(new ScreenItem("Ecom Events", FragmentStartDirections.startToEcomEvents()));

        return screens;
    }

    private void initPauseBehaviourSwitcher() {
        binding.spinnerPauseBehaviour.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, InAppPauseBehaviour.values()));
        binding.spinnerPauseBehaviour.setSelection(InAppPauseBehaviour.POSTPONE_IN_APPS.ordinal());
        binding.spinnerPauseBehaviour.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                getReteno().setInAppMessagesPauseBehaviour(InAppPauseBehaviour.values()[i]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    private void initInAppPausingSwitcher() {
        binding.swInAppsPause.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                getReteno().pauseInAppMessages(isChecked);
            }
        });
    }
}