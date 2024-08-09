package com.reteno.sample;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.reteno.core.RetenoImpl;

public class SecondActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        RetenoImpl.Companion.getInstance().pausePushInAppMessages(true);
        new Handler().postDelayed(() -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }, 2000L);
    }

    @Override
    protected void onStop() {
        RetenoImpl.Companion.getInstance().pausePushInAppMessages(false);
        super.onStop();
    }
}