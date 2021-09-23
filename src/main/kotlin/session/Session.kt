package com.github.asforest.mshell.session

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
    workdir: File,
    env: Map<String, String>,
    charset: Charset
) {
    val process: Process
    val stdin: PrintWriter
    val pid: Long get() = process.pid()
    val isAlive: Boolean get() = stdoutOpeningFlag && process.isAlive

    private var coStdoutCollector: Job
    private var stdoutOpeningFlag = true

    init {
        // 启动子进程
        process = ProcessBuilder()
            .command(command)
            .directory(workdir)
            .also { it.environment().putAll(env) }
            .redirectErrorStream(true)
            .redirectInput(ProcessBuilder.Redirect.PIPE)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .start()
        stdin = PrintWriter(process.outputStream, true, charset)

        userToConnect?.sendMessageBatchly("会话已创建(pid: $pid)")

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
                    usersConnected.forEach { it.sendRawMessageBatchly(String(buffer, 0, len, charset)) }
                } else {
                    process.inputStream.close()
                    stdoutOpeningFlag = false
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
            sendMessageBatchly("会话已结束(pid: $pid)")

            // 断开所有用户并从列表里移除
            manager.disconnectAll(this@Session)
        }
    }

    fun kill()
    {
        process.destroy()
    }

    fun connect(user: SessionUser)
    {
        manager.connect(user, this)
    }

    /**
     * 断开某个用户与当前会话的连接
     * @param user 要断开的用户
     */
    fun disconnect(user: SessionUser)
    {
        manager.disconnect(user)
    }

    /**
     * 断开连接到当前会话上的所有用户
     */
    fun disconnectAll()
    {
        manager.disconnectAll(this)
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