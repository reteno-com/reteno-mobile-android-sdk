package com.reteno.sample;


import android.app.Application;

import androidx.annotation.NonNull;

import com.reteno.core.Reteno;
import com.reteno.core.RetenoApplication;
import com.reteno.core.RetenoImpl;
import com.reteno.core.lifecycle.ScreenTrackingConfig;

import java.util.ArrayList;

public class SampleApp extends Application implements RetenoApplication {

    private Reteno retenoInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        retenoInstance = new RetenoImpl(this, BuildConfig.API_ACCESS_KEY);
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
