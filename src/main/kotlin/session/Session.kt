package com.github.asforest.mshell.session

import com.github.asforest.mshell.configuration.MainConfig
import com.github.asforest.mshell.event.Event
import kotlinx.coroutines.*
import okhttp3.internal.wait
import java.io.File
import java.io.PrintStream
import java.nio.charset.Charset
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class Session(
    val manager: SessionManager,
    userToConnect: SessionUser?,
    command: String,
    workdir: String? =null,
    env: Map<String, String>? =null
) {
    val onProcessExit = Event<Session, suspend () -> Unit>(this)
    val onStdoutMessage = Event<Session, suspend (message: String) -> Unit>(this)
    val onStdPipelineClose = Event<Session, suspend () -> Unit>(this)
    val onUserConnect = Event<Session, suspend (user: SessionUser) -> Unit>(this)
    val onUserDisconnect = Event<Session, suspend (user: SessionUser) -> Unit>(this)

    val process: Process
    val stdin: PrintStream
    var stdoutBuffer = mutableListOf<String>()
    val pid: Long get() = process.pid()

    var job_main: Job? = null
    var job_stdoutGatheringMonitor: Job? = null
    var job_stdoutPrinter: Job? = null

    private var continuation_stdoutPrinter: Continuation<Unit>? = null

    init {
        // 启动子进程
        val _workdir = File(if(workdir!=null && workdir!= "") workdir else System.getProperty("user.dir"))
        val _env = env ?: mapOf()
        process = ProcessBuilder()
            .command(command)
            .directory(_workdir)
            .also { it.environment().putAll(_env) }
            .redirectErrorStream(true)
            .redirectInput(ProcessBuilder.Redirect.PIPE)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .start()
        stdin = PrintStream(process.outputStream, true)

        // 发送消息
        runBlocking {
            userToConnect?.sendMessage("Process created with pid($pid)")
        }

        // 子进程存活监控协程
        job_main = startCoroutine {
            // 启动其它协程
            job_stdoutGatheringMonitor?.start()
            job_stdoutPrinter?.start()

            // 监控进程存活
            suspendCoroutine<Unit> {
                process.waitFor()
                it.resume(Unit)
            }

            // 触发事件
            onProcessExit { it() }

            // 等待stdpipeline关闭
            job_stdoutGatheringMonitor?.join()

            // 退出消息
            usersConnected.forEach { it.sendMessage("Process exited with pid(${pid})") }

            // 断开所有用户并从列表里移除
            manager.disconnectAll(this@Session)
            manager.sessions.remove(this@Session)

            delay(300)

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

            // 让缓冲区里的消息发送完毕
            job_stdoutPrinter?.join()

            // 分发事件
            onStdPipelineClose { it() }
        }

        // stdout输出协程
        job_stdoutPrinter = startCoroutine {
            while (process.isAlive)
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
                onStdoutMessage { it(text) }

                // 发送消息
                usersConnected.forEach { it.sendMessage(text) }
            }
        }

        // 注册Session
        SessionManager.sessions += this

        // 设置连接
        if(userToConnect != null)
            runBlocking { connect(userToConnect) }

        // 启动主协程
        job_main?.start()
    }

    private fun startCoroutine(block: suspend CoroutineScope.() -> Unit): Job
    {
        return GlobalScope.launch(manager.scd, CoroutineStart.LAZY, block)
    }

    fun kill(): Session
    {
        process.destroy()
        return this
    }

    suspend fun connect(user: SessionUser): Session
    {
        manager.connect(user, this)
        return this
    }

    suspend fun disconnect(user: SessionUser): Session
    {
        manager.disconnect(user)
        return this
    }

    suspend fun disconnectAll(): Session
    {
        manager.disconnectAll(this)
        return this
    }

    fun isUserConnected(user: SessionUser): Boolean = manager.getSessionByUserConnected(user) == this

    val isConnected: Boolean get() = usersConnected.isNotEmpty()

    val usersConnected: List<SessionUser> get() = manager.getUsersConnectedToSession(this)
}