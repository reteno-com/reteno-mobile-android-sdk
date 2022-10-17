package com.reteno.sample;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.reteno.util.Logger;
import com.reteno.util.UtilKt;

public class CustomReceiverPushReceived extends BroadcastReceiver {

    private static String TAG = CustomReceiverPushReceived.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Logger.i(TAG, "onReceive(): ", UtilKt.toStringVerbose(intent.getExtras()));
    }
}
