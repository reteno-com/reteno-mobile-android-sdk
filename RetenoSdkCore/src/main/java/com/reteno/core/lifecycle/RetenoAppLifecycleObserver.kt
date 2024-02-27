package com.reteno.core.lifecycle

import android.util.Log
import com.reteno.core.di.ServiceLocator
import com.reteno.core.domain.controller.IamController

class RetenoAppLifecycleObserver(val serviceLocator: ServiceLocator) /*: DefaultLifecycleObserver*/ {
/*
    private val sessionHandler = serviceLocator.retenoSessionHandlerProvider.get()
    private val iamController = serviceLocator.iamControllerProvider.get()

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        Log.e("ololo","lifecycle onCreate")
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Log.e("ololo","lifecycle onStart")
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        Log.e("ololo","lifecycle onResume")
        sessionHandler.start()
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        Log.e("ololo","lifecycle onPause")
        sessionHandler.stop()
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        Log.e("ololo","lifecycle onStop")
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        Log.e("ololo","lifecycle onDestroy")
    }

 */
}