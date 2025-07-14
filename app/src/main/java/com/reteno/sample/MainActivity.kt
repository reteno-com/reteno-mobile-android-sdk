package com.reteno.sample

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.reteno.core.RetenoInternalImpl
import com.reteno.core.view.iam.callback.InAppCloseData
import com.reteno.core.view.iam.callback.InAppData
import com.reteno.core.view.iam.callback.InAppErrorData
import com.reteno.core.view.iam.callback.InAppLifecycleCallback
import com.reteno.push.RetenoNotifications
import com.reteno.sample.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private var binding: ActivityMainBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.getRoot())
        checkPermissions()
        checkDeepLink(intent)
        setNavigation(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        checkDeepLink(intent)
        setNavigation(intent)
    }

    private fun createInAppLifecycleListener() {
        val context: Context = this
        RetenoInternalImpl.instance
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
        lifecycleScope.launch {
            val isGranted = RetenoNotifications.requestNotificationPermission()
            if (isGranted) {
                Toast.makeText(this@MainActivity, "Permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, "Permission not granted", Toast.LENGTH_SHORT).show()
            }
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