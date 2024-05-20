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
import com.reteno.sample.util.AppSharedPreferencesManager;
import com.reteno.sample.util.FragmentStartSessionListener;
import com.reteno.sample.util.RetenoInitListener;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import kotlin.Unit;

public class FragmentStart extends BaseFragment {

    private FragmentStartBinding binding;
    private RetenoSessionHandler sessionHandler;
    private FragmentStartSessionListener sessionListener = new FragmentStartSessionListener();


    public FragmentStart() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        awaitInit();
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
        initDelayedInitCheckbox();
    }

    @Override
    public void onPause() {
        super.onPause();
        sessionListener.stop();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sessionHandler != null) {
            sessionListener.start(sessionHandler, binding.tvSessionTime);
        }
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

    private void awaitInit() {
        RetenoImpl impl = (RetenoImpl) getReteno();
        if (impl.isInitialized()) {
            initSessionHandler();
            binding.progressBar.setVisibility(View.GONE);
            binding.cbDelayNextLaunch.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.cbDelayNextLaunch.setVisibility(View.GONE);
            new RetenoInitListener(impl, () -> {
                if (isResumed()) {
                    initSessionHandler();
                    binding.progressBar.setVisibility(View.GONE);
                    binding.cbDelayNextLaunch.setVisibility(View.VISIBLE);
                }
                return Unit.INSTANCE;
            });
        }
    }

    private void initSessionHandler() {
        try {
            Field field = RetenoImpl.class.getDeclaredField("serviceLocator");
            field.setAccessible(true);
            ServiceLocator serviceLocator = (ServiceLocator) field.get(getReteno());
            field.setAccessible(false);
            sessionHandler = serviceLocator.getRetenoSessionHandlerProvider().get();
            sessionListener.start(sessionHandler, binding.tvSessionTime);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void initDelayedInitCheckbox() {
        binding.cbDelayNextLaunch.setChecked(AppSharedPreferencesManager.getShouldDelayLaunch(requireContext()));
        binding.cbDelayNextLaunch.setOnCheckedChangeListener((compoundButton, b) -> AppSharedPreferencesManager.setDelayLaunch(compoundButton.getContext(), b));
    }
}