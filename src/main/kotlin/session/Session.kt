package com.github.asforest.mshell.session

import kotlinx.coroutines.*
import com.github.asforest.mshell.configuration.MainConfig
import com.github.asforest.mshell.event.EventDelegate
import com.github.asforest.mshell.session.SessionManager.sendMessage2
import com.github.asforest.mshell.type.USER
import java.io.PrintStream
import java.nio.charset.Charset
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class Session(
    val process: Process,
    val manager: SessionManager
) {
    val onProcessExit = EventDelegate<Session, suspend () -> Unit>()
    val onStdoutMessage = EventDelegate<Session, suspend (message: String) -> Unit>()
    val onStdPipelineClose = EventDelegate<Session, suspend () -> Unit>()
    val onUserConnect = EventDelegate<Session, suspend (user: USER) -> Unit>()
    val onUserDisconnect = EventDelegate<Session, suspend (user: USER) -> Unit>()

    val stdin = PrintStream(process.outputStream, true)
    var stdoutBuffer = mutableListOf<String>()
    val isAlive: Boolean get() = !process.isAlive

    var job_main: Job? = null
    var job_stdoutGatheringMonitor: Job? = null
    var job_stdoutPrinter: Job? = null

    private var continuation_stdoutPrinter: Continuation<Unit>? = null

    init {
        // 子进程存活监控协程
        job_main = startCoroutine {
            // 启动消息
            stdoutBuffer += "Process created pid($pid)\n"

            // 启动其它协程
            job_stdoutGatheringMonitor?.start()
            job_stdoutPrinter?.start()

            // 监控进程存活
            suspendCoroutine<Unit> {
                process.waitFor()
                it.resume(Unit)
            }

            // 退出消息
            usersConnected.forEach { it.sendMessage2("Process exited pid(${pid})") }

            // 触发事件
            onProcessExit(this@Session) { it() }

            // 断开所有用户并从列表里移除
            manager.disconnectAllUsers(this@Session)
            manager.sessions.remove(this@Session)

            // 停止其它协程
            job_stdoutPrinter?.cancel()
            job_stdoutGatheringMonitor?.cancel()
        }

        // stdout收集协程
        job_stdoutGatheringMonitor = startCoroutine {
            var len = 0
            val buffer = ByteArray(4 * 1024)
            while (len != -1 && process.isAlive) {
                suspendCoroutine<Unit> { continuation ->
                    process.inputStream.read(buffer).also { len = it } != -1
                    continuation.resume(Unit)
                }

                if(len != -1 && process.isAlive)
                {
                    synchronized(stdoutBuffer) {
                        stdoutBuffer += String(buffer, 0, len, Charset.forName("gbk"))
                    }
                    continuation_stdoutPrinter?.resume(Unit)
                    continuation_stdoutPrinter = null
                }
            }

            onStdPipelineClose(this@Session) { it() }
        }

        // stdout输出协程
        job_stdoutPrinter = startCoroutine {
            while (true)
            {
                if(stdoutBuffer.isEmpty())
                    suspendCoroutine<Unit> { continuation_stdoutPrinter = it }

                val truncation = MainConfig.stdoutputTruncationThreshold
                val batchingTimeout = MainConfig.stdoutputBatchingTimeoutInMs.toLong()
                val buffered = StringBuffer()

                while (buffered.length < truncation && stdoutBuffer.isNotEmpty())
                {
                    val temp: MutableList<String>
                    synchronized(stdoutBuffer) {
                        temp = stdoutBuffer
                        stdoutBuffer = mutableListOf()
                    }

                    for(segment in temp)
                        buffered.append(segment)

                    delay(batchingTimeout)
                }

                val text = buffered.toString()

                // 分发事件
                onStdoutMessage(this@Session) { it(text) }

                // 发送消息
                usersConnected.forEach { it.sendMessage2(text) }
            }
        }
    }

    private fun startCoroutine(block: suspend CoroutineScope.() -> Unit): Job
    {
        return GlobalScope.launch(manager.scd, CoroutineStart.LAZY, block)
    }

    fun start(): Session
    {
        job_main?.start()
        return this
    }

    fun kill(): Session
    {
        process.destroy()
        return this
    }

    suspend fun connect(user: USER): Session
    {
        manager.connectToSession(user, this)
        return this
    }

    suspend fun disconnect(user: USER): Session
    {
        manager.disconnectFromSession(user)
        return this
    }

    suspend fun disconnectAll(): Session
    {
        manager.disconnectAllUsers(this)
        return this
    }

    fun isUserConnected(user: USER): Boolean = manager.getSessionByUserConnected(user) == this

    val isConnected: Boolean get() = usersConnected.isNotEmpty()

    val usersConnected: List<USER> get() = manager.getUsersConnectedToSession(this)

    val pid: Long get() = process.pid()
}