package com.github.asforest.mshell.session

import com.github.asforest.mshell.exception.UnsupportedCharsetException
import kotlinx.coroutines.*
import java.io.File
import java.io.PrintWriter
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
    val stdin: PrintWriter
    val pid: Long get() = process.pid()
    val isAlive: Boolean get() = stdoutOpened && process.isAlive

    private var coStdoutCollector: Job
    private var stdoutOpened = true

    init {
        // 启动子进程
        val _workdir = File(if(workdir!=null && workdir!= "") workdir else System.getProperty("user.dir"))
        val _env = env ?: mapOf()
        val _charset = if(Charset.isSupported(charset)) Charset.forName(charset)
                       else throw UnsupportedCharsetException("The charset '$charset' is unsupported")

        process = startProcess(command, _workdir, _env)
        stdin = PrintWriter(process.outputStream, true, _charset)

        userToConnect?.sendMessageBatchly("Process created with pid($pid)")

        // stdout收集协程
        coStdoutCollector = GlobalScope.launch(Dispatchers.IO, CoroutineStart.LAZY) {
            var len = 0
            val buffer = ByteArray(4 * 1024)
            while (true)
            {
                suspendCoroutine<Unit> {
                    process.inputStream.read(buffer).apply { len = this } // blocking
                    it.resume(Unit)
                }

                if(len != -1) {
                    usersConnected.forEach { it.sendRawMessageBatchly(String(buffer, 0, len, _charset)) }
                } else {
                    process.inputStream.close()
                    stdoutOpened = false
                    break
                }
            }
        }

        // 注册Session
        SessionManager.sessions += this

        // 设置连接
        if(userToConnect != null)
            connect(userToConnect)

        // 手动消息分批，用于将提示信息和下面的子程序输出显式分开来（拆分成两条消息）
        userToConnect?.sendMessageTruncation()

        // 启动stdout收集协程
        coStdoutCollector.start()

        // 当进程退出
        process.onExit().thenRun {
            // 取消注册
            manager.sessions.remove(this@Session)

            // 退出消息
            sendMessageTruncation()
            sendMessageBatchly("Process exited with pid(${pid})")

            // 断开所有用户并从列表里移除
            manager.disconnectAll(this@Session)
        }
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

    fun connect(user: SessionUser): Session
    {
        manager.connect(user, this)
        return this
    }

    fun disconnect(user: SessionUser): Session
    {
        manager.disconnect(user)
        return this
    }

    fun disconnectAll(): Session
    {
        manager.disconnectAll(this)
        return this
    }

    fun sendMessageBatchly(msg: String, truncation: Boolean =false)
    {
        usersConnected.forEach { it.sendMessageBatchly(msg, truncation) }
    }

    fun sendMessageTruncation()
    {
        usersConnected.forEach { it.sendMessageTruncation() }
    }

    fun isUserConnected(user: SessionUser): Boolean = manager.getSessionByUserConnected(user) == this

    val isConnected: Boolean get() = usersConnected.isNotEmpty()

    val usersConnected: Collection<SessionUser> get() = manager.getUsersConnectedToSession(this)

    val connections: Collection<Connection> get() = manager.getConnections(this)
}