package com.reteno.tests;

import com.reteno.push.channel.RetenoNotificationChannel;

import org.powermock.reflect.Whitebox;

class RetenoNotificationChannelProxy {

    static String FALLBACK_DEFAULT_CHANNEL_NAME;
    static String FALLBACK_DEFAULT_CHANNEL_DESCRIPTION;

    static {
        try {
            FALLBACK_DEFAULT_CHANNEL_NAME = (String) Whitebox.getField(
                    RetenoNotificationChannel.class,
                    "FALLBACK_DEFAULT_CHANNEL_NAME"
            ).get(RetenoNotificationChannel.class);

            FALLBACK_DEFAULT_CHANNEL_DESCRIPTION = (String) Whitebox.getField(
                    RetenoNotificationChannel.class,
                    "FALLBACK_DEFAULT_CHANNEL_DESCRIPTION"
            ).get(RetenoNotificationChannel.class);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    static String getDefaultChannelId() {
        return RetenoNotificationChannel.INSTANCE.getDEFAULT_CHANNEL_ID();
    }

    static void createDefaultChannel() {
        RetenoNotificationChannel.createDefaultChannel$RetenoSdkPush_debug();
    }

    static void configureDefaultNotificationChannel(String channel) {
        RetenoNotificationChannel.configureDefaultNotificationChannel$RetenoSdkPush_debug(channel);
    }
}
