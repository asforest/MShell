package com.github.asforest.mshell.session

import com.github.asforest.mshell.event.AsyncEvent
import com.github.asforest.mshell.exception.business.*
import com.github.asforest.mshell.model.Preset
import com.github.asforest.mshell.session.user.AbstractSessionUser
import com.github.asforest.mshell.session.user.GroupUser
import com.pty4j.PtyProcess
import com.pty4j.PtyProcessBuilder
import kotlinx.coroutines.*
import java.io.File
import java.io.PrintWriter
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Session 代表一个会话对象，会话对象就是对一个子进程进行封装和管理的对象，
 * 可以粗略地认为一个会话就是一个子进程
 */
@DelicateCoroutinesApi
class Session(
    val manager1: SessionManager,
    val preset: Preset,
    userAutoConnect: AbstractSessionUser? = null
) {
    val process: PtyProcess
    val stdin: PrintWriter
    var pid: Long = -1
    val lwm = LastwillMessage(preset.lastwillCapacity)
    var isLive = true
    val connectionManager = ConnectionManager(this)

    val onProcessExit = AsyncEvent()

    private var coStdoutCollector: Job
    private var stdoutOpen = true

    init {
        validatePreset(preset)

        val charset = Charset.forName(preset.charset)

        // 启动子进程
        process = PtyProcessBuilder()
            .setCommand(preset.command.split(" ").toTypedArray())
            .setDirectory(File(preset.workdir.ifEmpty { System.getProperty("user.dir") }).absolutePath)
            .setEnvironment(System.getenv() + preset.env)
            .setRedirectErrorStream(true)
            .setWindowsAnsiColorEnabled(false)
            .setInitialColumns(preset.columns)
            .setInitialRows(preset.rows)
            .start()

        pid = process.pid()
        stdin = PrintWriter(process.outputStream, true, charset)

        // stdout收集协程
        coStdoutCollector = GlobalScope.launch(Dispatchers.IO, CoroutineStart.LAZY) {
            var len: Int
            val buffer = ByteArray(4 * 1024)
            while (true)
            {
                suspendCoroutine<Unit> {
                    process.inputStream.read(buffer).apply { len = this } // blocking
                    it.resume(Unit)
                }

                if(len != -1) { // 有消息时
                    val message = String(buffer, 0, len, charset)

                    // 发送给所有连接上的用户
                    connections.forEach { it.sendMessage(message) }

                    // 再作为遗言保留一份，以便发送给暂时离线的用户
                    lwm.append(message)
                } else {
                    process.inputStream.close()
                    stdoutOpen = false

                    println("BBB: $isLive")
//
                    if(!isLive)
                        cleanup()

                    break
                }
            }
        }

        // 向子进程的stdin输入input选项里的内容
        if(preset.input.isNotEmpty())
            stdin.println(preset.input)

        // 用户自动连接
        if(userAutoConnect != null)
        {
            val conn = connect(userAutoConnect)
            conn.sendMessage("会话已创建(pid: $pid)，环境预设(${preset.name})\n")
        }
    }

    fun start()
    {
        // 启动stdout收集协程
        coStdoutCollector.start()

        // 当进程退出
        process.onExit().thenRun {
            isLive = false

            println("AAA: $stdoutOpen")

            if (!stdoutOpen)
                GlobalScope.launch { cleanup() }
        }
    }

    private suspend fun cleanup()
    {
        // 发送退出消息
        broadcaseMessageTruncation()
        broadcastMessageBatchly("会话已结束(pid: $pid)，环境预设(${preset.name})\n")
//        println("开始刷新缓冲区")

        // 关掉所有连接
        disconnectAll()

        connectionManager.closeAndWait()

//        println("cleanup")
        onProcessExit.invoke()
    }

    fun kill()
    {
        process.destroy()
    }

    fun connect(user: AbstractSessionUser): Connection
    {
        if(manager1.hasUserConnectedToAnySession(user))
            throw SessionUserAlreadyConnectedException(pid)

        val whenOnlineChanged = connectionManager.getConnection(user, true)?.whenOnlineChanged ?: -1
        val (conn, isReconnection) = connectionManager.openConnection(user)

        // 发送遗愿消息
        if(whenOnlineChanged != -1L && lwm.hasMessage(whenOnlineChanged))
        {
            var last: Long = 0
            val sb = StringBuffer()
            for (msg in lwm.getAllLines(whenOnlineChanged))
            {
                sb.append(msg.message)
                last = msg.time
            }
            sb.append("\n==========最后输出(${SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(last)})==========\n")

            conn.sendTruncation()
            conn.sendMessage(sb.toString())
            conn.sendTruncation()
        }

        conn.sendMessage((if(isReconnection) "已重连" else "已连接")+"到会话(pid: $pid)，环境预设(${preset.name})\n")
        conn.sendTruncation()

        return conn
    }

    /**
     * 断开某个用户与当前会话的连接
     * @param user 要断开的用户
     */
    fun disconnect(user: AbstractSessionUser): Connection
    {
        val connection = getConnection(user)
            ?: if(user is GroupUser)
                throw UserNotConnectedException(user)
            else
                throw UserNotConnectedException()

        // 发送消息
        connection.sendTruncation()
        connection.sendMessage("已从会话断开(pid: $pid)，环境预设(${preset.name})\n")
        connection.close()

        return connection
    }

    /**
     * 断开连接到当前会话上的所有用户
     */
    fun disconnectAll()
    {
        for (user in usersOnline)
            disconnect(user)
    }

    /**
     * 向所有连接到当前Session上的用户广播消息
     * @param message 要发送的消息
     */
    fun broadcastMessageBatchly(message: String)
    {
        connections.forEach { it.sendMessage(message) }
    }

    /**
     * 强制打断消息合并，拆分成2条消息发送
     */
    fun broadcaseMessageTruncation()
    {
        connections.forEach { it.sendTruncation() }
    }

    fun getConnection(user: AbstractSessionUser): Connection? = connectionManager.getConnection(user, includeOffline = false)

    fun isUserConnected(user: AbstractSessionUser): Boolean = getConnection(user) != null

    val usersOnline: Collection<AbstractSessionUser> get() = connectionManager.getConnections(includeOffline = false).map { it.user }

    val connections: Collection<Connection> get() = connectionManager.getConnections(includeOffline = false)

    @Suppress("NOTHING_TO_INLINE")
    private inline fun validatePreset(preset: Preset)
    {
        if (preset.command.isEmpty())
            throw PresetIsIncompeleteException(preset)

        if (preset.charset.isEmpty() || !Charset.isSupported(preset.charset))
            throw UnsupportedCharsetException(preset.charset)

        if (preset.columns < 0 || preset.rows < 0 || preset.columns > 100000 || preset.rows > 100000)
            throw TerminalColumnRowsOutOfRangeException(preset.columns, preset.rows)
    }

    override fun toString(): String
    {
        return "Session[pid=$pid, presetName=${preset.name}, preset=$preset, isLive=$isLive]"
    }

    override fun equals(other: Any?): Boolean
    {
        if (other == null || other !is Session)
            return false

        return hashCode() == other.hashCode()
    }

    override fun hashCode(): Int
    {
        val i = pid.toInt() xor (pid shr 32).toInt()
        return (i shl 5) - i
    }
}