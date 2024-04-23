package com.reteno.sample;


import android.app.Application;

import androidx.annotation.NonNull;

import com.reteno.core.Reteno;
import com.reteno.core.RetenoApplication;
import com.reteno.core.RetenoConfig;
import com.reteno.core.RetenoImpl;
import com.reteno.core.lifecycle.ScreenTrackingConfig;

import java.util.ArrayList;

public class SampleApp extends Application implements RetenoApplication {

    private Reteno retenoInstance;
    private int tries = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        retenoInstance = new RetenoImpl(this, BuildConfig.API_ACCESS_KEY, new RetenoConfig(false, () -> {
            if (tries == 5) {
                return "custom_id";
            } else {
                tries++;
                return null;
            }
        }));
        ArrayList<String> excludeScreensFromTracking = new ArrayList<String>();
        excludeScreensFromTracking.add("NavHostFragment");
        retenoInstance.autoScreenTracking(new ScreenTrackingConfig(false, excludeScreensFromTracking));
    }


    @NonNull
    @Override
    public Reteno getRetenoInstance() {
        return retenoInstance;
    }
}
