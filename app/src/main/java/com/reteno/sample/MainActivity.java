package com.reteno.sample;

import static android.Manifest.permission.POST_NOTIFICATIONS;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.snackbar.Snackbar;
import com.reteno.core.Reteno;
import com.reteno.core.RetenoApplication;
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
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        checkDeepLink(intent);
        setNavigation(intent);
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