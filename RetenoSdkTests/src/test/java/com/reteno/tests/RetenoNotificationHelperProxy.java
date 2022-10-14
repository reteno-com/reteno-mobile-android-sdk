package com.reteno.tests;

import android.os.Bundle;

import com.reteno.push.RetenoNotificationHelper;

import org.powermock.reflect.Whitebox;

public class RetenoNotificationHelperProxy {

    static int NOTIFICATION_ID_DEFAULT;

    static {
        try {
            NOTIFICATION_ID_DEFAULT = (int) Whitebox.getField(
                    RetenoNotificationHelper.class,
                    "NOTIFICATION_ID_DEFAULT"
            ).get(RetenoNotificationHelper.class);
        } catch (IllegalAccessException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    static int getNotificationId(Bundle bundle) {
        return RetenoNotificationHelper.getNotificationId$RetenoSdkPush_debug(bundle);
    }
}
