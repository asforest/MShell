package com.github.asforest.mshell.session

import com.github.asforest.mshell.configuration.MainConfig
import com.github.asforest.mshell.event.Event
import com.github.asforest.mshell.exception.UnsupportedCharsetException
import kotlinx.coroutines.*
import java.io.File
import java.io.PrintStream
import java.nio.charset.Charset
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class Session(
    val manager: SessionManager,
    userToConnect: SessionUser?,
    command: String,
    workdir: String? =null,
    env: Map<String, String>? =null,
    charset: String
) {
    val process: Process
    val stdin: PrintStream
    var stdoutBuffer = mutableListOf<String>()
    val pid: Long get() = process.pid()

    var co_main: Job? = null
    var co_stdoutGathering: Job? = null
    var co_stdoutForwarding: Job? = null

    private var stdoutOpened = true
    private val gatheringAndForwardingLock = Object()
    private var onStdoutForwardResume = Event<Unit, () -> Unit>(Unit)

    init {
        // 启动子进程
        val _workdir = File(if(workdir!=null && workdir!= "") workdir else System.getProperty("user.dir"))
        val _env = env ?: mapOf()
        val _charset = if(Charset.isSupported(charset)) charset
                       else throw UnsupportedCharsetException("The charset '$charset' is unsupported")

        process = startProcess(command, _workdir, _env)
        stdin = PrintStream(process.outputStream, true, _charset)

        // 发送消息
        runBlocking {
            userToConnect?.sendMessage("Process created with pid($pid)")
        }

        // 主协程
        co_main = coMain()

        // stdout收集协程
        co_stdoutGathering = coStdoutGathering(_charset)

        // stdout输出协程
        co_stdoutForwarding = coStdoutForwarder()

        // 注册Session
        SessionManager.sessions += this

        // 设置连接
        if(userToConnect != null)
            runBlocking { connect(userToConnect) }

        // 启动主协程
        co_main?.start()
    }

    fun coMain(): Job
    {
        return startCoroutine {
            // 启动其它协程
            co_stdoutGathering?.start()
            co_stdoutForwarding?.start()

            // 监控进程存活
            suspendCoroutine<Unit> {
                process.waitFor() // blocking
                it.resume(Unit)
            }

            // 等待收集协程关闭
            co_stdoutGathering?.join()

            // 退出消息
            usersConnected.forEach { it.sendMessage("Process exited with pid(${pid})") }

            // 断开所有用户并从列表里移除
            manager.disconnectAll(this@Session)
            manager.sessions.remove(this@Session)
        }
    }

    fun coStdoutGathering(charset: String): Job
    {
        return startCoroutine {
            val __charset = Charset.forName(charset)
            var len = 0
            val buffer = ByteArray(4 * 1024)

            while (true)
            {
                suspendCoroutine<Unit> {
                    process.inputStream.read(buffer).apply { len = this } // blocking
                    it.resume(Unit)
                }

                if(len != -1)
                {
                    synchronized(gatheringAndForwardingLock) {
                        stdoutBuffer += String(buffer, 0, len, __charset)
                    }
                    onStdoutForwardResume { it() }
                } else {
                    stdoutOpened = false
                    onStdoutForwardResume { it() }
                    break
                }
            }

            // 让缓冲区里的消息发送完毕
            co_stdoutForwarding?.join()
        }
    }

    fun coStdoutForwarder(): Job
    {
        return startCoroutine {
            val truncation = MainConfig.stdoutputTruncationThreshold
            val batchingTimeout = MainConfig.stdoutputBatchingTimeoutInMs.toLong()

            while (stdoutOpened)
            {
                if(stdoutBuffer.isEmpty())
                    suspendCoroutine<Unit> { onStdoutForwardResume.once { it.resume(Unit) } }

                val buffered = StringBuffer()

                while (buffered.length < truncation && stdoutBuffer.isNotEmpty())
                {
                    val temp: MutableList<String>
                    synchronized(gatheringAndForwardingLock) {
                        temp = stdoutBuffer
                        stdoutBuffer = mutableListOf()
                    }

                    for(segment in temp)
                        buffered.append(segment)

                    delay(batchingTimeout)
                }

                // 发送消息
                usersConnected.forEach { it.sendMessage(buffered.toString()) }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun startCoroutine(block: suspend CoroutineScope.() -> Unit): Job
    {
        return GlobalScope.launch(manager.scd, CoroutineStart.LAZY, block)
    }

    private fun startProcess(command: String, workdir: File, env: Map<String, String>): Process
    {
        return ProcessBuilder()
            .command(command)
            .directory(workdir)
            .also { it.environment().putAll(env) }
            .redirectErrorStream(true)
            .redirectInput(ProcessBuilder.Redirect.PIPE)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .start()
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