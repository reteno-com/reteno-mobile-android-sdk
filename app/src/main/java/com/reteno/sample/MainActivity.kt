package com.reteno.sample

import android.Manifest.permission
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.snackbar.Snackbar
import com.reteno.core.RetenoImpl
import com.reteno.core.view.iam.callback.InAppCloseData
import com.reteno.core.view.iam.callback.InAppData
import com.reteno.core.view.iam.callback.InAppErrorData
import com.reteno.core.view.iam.callback.InAppLifecycleCallback
import com.reteno.sample.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private var binding: ActivityMainBinding? = null
    private val requestPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult<String, Boolean>(
            ActivityResultContracts.RequestPermission(),
            ActivityResultCallback<Boolean> { isGranted: Boolean ->
                if (isGranted) {
                    RetenoImpl.instance
                        .updatePushPermissionStatus()
                    Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show()
                }
            })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.getRoot())
        checkPermissions()
        checkDeepLink(intent)
        setNavigation(intent)
        //createInAppLifecycleListener(); this is an example of in-app lifecycle callbacks
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        checkDeepLink(intent)
        setNavigation(intent)
    }

    private fun createInAppLifecycleListener() {
        val context: Context = this
        RetenoImpl.instance
            .setInAppLifecycleCallback(object : InAppLifecycleCallback {
                override fun beforeDisplay(inAppData: InAppData) {
                    Toast.makeText(
                        context,
                        "beforeDisplay: " + inAppData.id + ", " + inAppData.source.toString(),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onDisplay(inAppData: InAppData) {
                    Toast.makeText(
                        context,
                        "onDisplay: " + inAppData.id + ", " + inAppData.source.toString(),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun beforeClose(closeData: InAppCloseData) {
                    Toast.makeText(
                        context,
                        "beforeClose: " + closeData.id + ", " + closeData.closeAction.toString(),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun afterClose(closeData: InAppCloseData) {
                    Toast.makeText(
                        context,
                        "afterClose: " + closeData.id + ", " + closeData.closeAction.toString(),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onError(errorData: InAppErrorData) {
                    Toast.makeText(
                        context,
                        "onError: " + errorData.id + ", " + errorData.errorMessage,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return
        } else if (shouldShowRequestPermissionRationale(permission.POST_NOTIFICATIONS)) {
            Snackbar.make(window.decorView, "Notification blocked", Snackbar.LENGTH_LONG)
                .setAction("Settings") { v: View? ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.setData(uri)
                    startActivity(intent)
                }.show()
        } else {
            requestPermissionLauncher.launch(permission.POST_NOTIFICATIONS)
        }
    }

    private fun checkDeepLink(intent: Intent?) {
        if (intent != null) {
            val uri = intent.data
            if (uri != null) {
                binding!!.tvDeepLinkData.text = "Intent data: " + intent.data.toString()
            }
        }
    }

    private fun setNavigation(intent: Intent) {
        val bundle = intent.extras
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment?
        navHostFragment!!.navController.setGraph(R.navigation.nav_graph_main, bundle)
    }
}