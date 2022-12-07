package com.reteno.core.util

import java.util.concurrent.ThreadFactory

internal class RetenoThreadFactory : ThreadFactory {

    override fun newThread(r: Runnable): Thread {
        return RetenoThread(r)
    }

}