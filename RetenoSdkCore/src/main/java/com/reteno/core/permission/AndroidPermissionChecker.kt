package com.reteno.core.permission

import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

internal class LauncherContext<Input, Output>(
    activity: ComponentActivity,
    contract: ActivityResultContract<Input, Output>,
    private val unanswered: (Input) -> Output
) {
    private var pending: Pair<Input, CancellableContinuation<Output>>? = null
    private val launcher = activity.registerForActivityResult(contract) { answer(it) }

    suspend fun launch(input: Input): Output = suspendCancellableCoroutine { waiting ->
        pending = input to waiting
        waiting.invokeOnCancellation { pending = null }
        try {
            launcher.launch(input)
        } catch (failure: Exception) {
            pending = null
            throw failure
        }
    }

    fun abandon() {
        val (input, _) = pending ?: return
        answer(unanswered(input))
    }

    private fun answer(result: Output) {
        val (_, waiting) = pending ?: return
        pending = null
        waiting.resume(result)
    }
}

open class AndroidPermissionChecker : AndroidActivityAware() {

    private class Launchers(
        val single: LauncherContext<String, Boolean>,
        val multiple: LauncherContext<Array<String>, Map<String, Boolean>>
    )

    private val requests = Mutex()
    private var launchers: Launchers? = null

    override fun attachActivity(activity: ComponentActivity) {
        super.attachActivity(activity)
        launchers = Launchers(
            single = LauncherContext(activity, RequestPermission()) { false },
            multiple = LauncherContext(activity, RequestMultiplePermissions()) { permissions ->
                permissions.associateWith { false }
            }
        )
    }

    override fun onDestroy(owner: LifecycleOwner) {
        launchers?.let {
            it.single.abandon()
            it.multiple.abandon()
        }
        launchers = null
        super.onDestroy(owner)
    }

    suspend fun check(permission: String): Boolean = requests.withLock {
        val attached = launchers ?: return@withLock false
        attached.single.launch(permission)
    }

    suspend fun check(vararg permissions: String): Map<String, Boolean> = requests.withLock {
        val attached = launchers ?: return@withLock permissions.associateWith { false }
        attached.multiple.launch(arrayOf(*permissions))
    }
}
