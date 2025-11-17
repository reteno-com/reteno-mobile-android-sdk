package com.reteno.sample;

import android.app.Application;
import android.graphics.Color;

import androidx.core.app.NotificationManagerCompat;

import com.reteno.core.Reteno;
import com.reteno.core.RetenoConfig;

public class SampleJavaApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Reteno.initWithConfig(
                new RetenoConfig.Builder()
                        .accessKey(BuildConfig.API_ACCESS_KEY)
                        .setDebug(BuildConfig.DEBUG)
                        .defaultNotificationChannelConfig(builder -> {
                            builder
                                    .setName("Default Channel")
                                    .setDescription("General notifications")
                                    .setImportance(NotificationManagerCompat.IMPORTANCE_HIGH)
                                    .setLightColor(Color.BLUE)
                                    .setLightsEnabled(true)
                                    .setVibrationEnabled(true)
                                    .setShowBadge(true);
                            return null;
                        })
                        .build()
        );
    }
}