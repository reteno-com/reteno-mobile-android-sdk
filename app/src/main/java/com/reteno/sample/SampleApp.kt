package com.reteno.sample;


import android.app.Application;
import android.os.Handler;

import androidx.annotation.NonNull;

import com.reteno.core.Reteno;
import com.reteno.core.RetenoApplication;
import com.reteno.core.RetenoConfig;
import com.reteno.core.RetenoImpl;
import com.reteno.core.domain.model.event.LifecycleTrackingOptions;
import com.reteno.core.identification.DeviceIdProvider;
import com.reteno.core.lifecycle.ScreenTrackingConfig;
import com.reteno.sample.util.AppSharedPreferencesManager;

import java.util.ArrayList;

public class SampleApp extends Application implements RetenoApplication {

    private Reteno retenoInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        if (AppSharedPreferencesManager.getShouldDelayLaunch(this)) {
            AppSharedPreferencesManager.setDelayLaunch(this, false);
            retenoInstance = new RetenoImpl(this);
            Handler handler = new Handler();
            handler.postDelayed(() -> {
                retenoInstance.initWith(new RetenoConfig(false, createProvider(), LifecycleTrackingOptions.Companion.getALL(), BuildConfig.API_ACCESS_KEY));
            }, 3000L);
        } else {
            retenoInstance = new RetenoImpl(this, BuildConfig.API_ACCESS_KEY, new RetenoConfig(false, createProvider()));
        }
        ArrayList<String> excludeScreensFromTracking = new ArrayList<String>();
        excludeScreensFromTracking.add("NavHostFragment");
        retenoInstance.autoScreenTracking(new ScreenTrackingConfig(false, excludeScreensFromTracking));
    }

    private DeviceIdProvider createProvider() {
        DeviceIdProvider provider = null;
        int deviceIdDelay = AppSharedPreferencesManager.getDeviceIdDelay(this);
        String deviceId = AppSharedPreferencesManager.getDeviceId(this);
        if (!deviceId.isEmpty()) {
            long startTime = System.currentTimeMillis();
            provider = () -> {
                if (System.currentTimeMillis() - startTime > deviceIdDelay) {
                    return deviceId;
                } else {
                    return null;
                }
            };
        }
        return provider;
    }


    @NonNull
    @Override
    public Reteno getRetenoInstance() {
        return retenoInstance;
    }
}
