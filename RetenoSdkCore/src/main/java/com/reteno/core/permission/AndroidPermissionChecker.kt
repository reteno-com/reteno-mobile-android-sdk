package com.reteno.core.permission

import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal class LauncherContext<Input, Output>(
    activity: ComponentActivity,
    contract: ActivityResultContract<Input, Output>
) {
    private val launcher = activity.registerForActivityResult(contract) {
        continuation?.resume(it)
        continuation = null
    }
    private var continuation: CancellableContinuation<Output>? = null

    fun launch(input: Input, continuation: CancellableContinuation<Output>) {
        this.continuation = continuation
        launcher.launch(input)
    }
}

open class AndroidPermissionChecker: AndroidActivityAware() {

    private var singleChecker: LauncherContext<String, Boolean>? = null
    private var multipleChecker: LauncherContext<Array<String>, Map<String, Boolean>>? = null

    override fun attachActivity(activity: ComponentActivity) {
        super.attachActivity(activity)
        singleChecker = LauncherContext(activity, RequestPermission())
        multipleChecker = LauncherContext(activity, RequestMultiplePermissions())
    }

    override fun onDestroy(owner: LifecycleOwner) {
        singleChecker = null
        multipleChecker = null
        super.onDestroy(owner)
    }

    suspend fun check(permission: String): Boolean = suspendCancellableCoroutine {
        singleChecker?.launch(permission, it)
    }

    suspend fun check(vararg permissions: String) = suspendCancellableCoroutine {
        multipleChecker?.launch(arrayOf(*permissions), it)
    }
}