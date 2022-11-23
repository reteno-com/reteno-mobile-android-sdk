package com.reteno.sample;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.reteno.core.util.Logger;

public class CustomPushReceiver extends BroadcastReceiver {

    public static final String TAG = CustomPushReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        Toast.makeText(context, "Received custom push. Bundle.size = " + extras.size(), Toast.LENGTH_SHORT).show();
        /*@formatter:off*/ Logger.i(TAG, "onReceive(): ", "context = [" , context , "], intent = [", intent, "]");
        /*@formatter:on*/
    }

}
