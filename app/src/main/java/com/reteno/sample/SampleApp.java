package com.reteno.sample;


import android.app.Application;

import androidx.annotation.NonNull;

import com.reteno.Reteno;
import com.reteno.RetenoApplication;
import com.reteno.RetenoImpl;
import com.reteno.data.local.config.DeviceIdMode;
import com.reteno.sample.util.AppSharedPreferencesManager;

public class SampleApp extends Application implements RetenoApplication {

    private Reteno retenoInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        retenoInstance = new RetenoImpl(this);
        DeviceIdMode deviceIdMode = AppSharedPreferencesManager.getDeviceIdMode(this);
        retenoInstance.changeDeviceIdMode(deviceIdMode);
    }


    @NonNull
    @Override
    public Reteno getRetenoInstance() {
        return retenoInstance;
    }
}
