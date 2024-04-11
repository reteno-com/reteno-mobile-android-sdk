package com.reteno.sample;

import static android.Manifest.permission.POST_NOTIFICATIONS;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.snackbar.Snackbar;
import com.reteno.core.RetenoApplication;
import com.reteno.core.view.iam.callback.InAppCloseData;
import com.reteno.core.view.iam.callback.InAppData;
import com.reteno.core.view.iam.callback.InAppErrorData;
import com.reteno.core.view.iam.callback.InAppLifecycleCallback;
import com.reteno.sample.databinding.ActivityMainBinding;


public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
        if (isGranted) {
            ((RetenoApplication) getApplicationContext()).getRetenoInstance().updatePushPermissionStatus();
            Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show();
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        checkPermissions();
        checkDeepLink(getIntent());
        setNavigation(getIntent());
        //createInAppLifecycleListener(); this is an example of in-app lifecycle callbacks
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        checkDeepLink(intent);
        setNavigation(intent);
    }

    private void createInAppLifecycleListener() {
        Context context = this;
        ((RetenoApplication) getApplication()).getRetenoInstance().setInAppLifecycleCallback(new InAppLifecycleCallback() {
            @Override
            public void beforeDisplay(@NonNull InAppData inAppData) {
                Toast.makeText(context, "beforeDisplay: " + inAppData.getId() + ", " + inAppData.getSource().toString(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDisplay(@NonNull InAppData inAppData) {
                Toast.makeText(context, "onDisplay: " + inAppData.getId() + ", " + inAppData.getSource().toString(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void beforeClose(@NonNull InAppCloseData closeData) {
                Toast.makeText(context, "beforeClose: " + closeData.getId() + ", " + closeData.getCloseAction().toString(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void afterClose(@NonNull InAppCloseData closeData) {
                Toast.makeText(context, "afterClose: " + closeData.getId() + ", " + closeData.getCloseAction().toString(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(@NonNull InAppErrorData errorData) {
                Toast.makeText(context, "onError: " + errorData.getId() + ", " + errorData.getErrorMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            return;
        } else if (shouldShowRequestPermissionRationale(POST_NOTIFICATIONS)) {
            Snackbar.make(getWindow().getDecorView(), "Notification blocked", Snackbar.LENGTH_LONG).setAction("Settings", v -> {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }).show();
        } else {
            requestPermissionLauncher.launch(POST_NOTIFICATIONS);
        }
    }

    private void checkDeepLink(Intent intent) {
        if (intent != null) {
            Uri uri = intent.getData();
            if (uri != null) {
                binding.tvDeepLinkData.setText("Intent data: " + intent.getData().toString());
            }
        }
    }

    private void setNavigation(Intent intent) {
        Bundle bundle = intent.getExtras();
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.navHostFragment);
        navHostFragment.getNavController().setGraph(R.navigation.nav_graph_main, bundle);
    }
}