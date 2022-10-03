package com.reteno;

import android.app.Application;

import com.reteno.config.DeviceIdMode;

public class SampleApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Reteno.INSTANCE.init(this, DeviceIdMode.CUSTOM_ID);
    }
}
