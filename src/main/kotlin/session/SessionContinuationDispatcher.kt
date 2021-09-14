package com.github.asforest.mshell.session

import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext

class SessionContinuationDispatcher :
    ContinuationInterceptor,
    AbstractCoroutineContextElement(ContinuationInterceptor)
{
    var threadIdx = 0
    val executor: ThreadPoolExecutor = Executors.newCachedThreadPool() {
        Thread(it, SessionContinuationDispatcher::class.simpleName+" #$threadIdx").apply { threadIdx += 1 }
    } as ThreadPoolExecutor

    override fun <T> interceptContinuation(origin: Continuation<T>): Continuation<T>
    {
        return object: Continuation<T>, Runnable {
            var result: Result<T>? = null
            override val context: CoroutineContext get() = origin.context
            override fun resumeWith(result: Result<T>) { this.result = result; executor.submit(this) }
            override fun run() { origin.resumeWith(result!!) }
        }
    }
}
