package com.github.asforest.mshell.session

import com.github.asforest.mshell.event.AsyncEvent
import com.github.asforest.mshell.exception.business.*
import com.github.asforest.mshell.model.Preset
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
class Session(
    val manager: SessionManager,
    val preset: Preset,
    userAutoConnect: SessionUser? = null
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

//                    println("BBB: $isLive")
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
            connect(userAutoConnect)

        // 启动stdout收集协程
        coStdoutCollector.start()

        // 当进程退出
        process.onExit().thenRun {
            isLive = false

//            println("AAA: $stdoutOpen")

            if (!stdoutOpen)
                GlobalScope.launch { cleanup() }
        }
    }

    /**
     * 子进程退出时的清理方法
     */
    private suspend fun cleanup()
    {
        // 发送退出消息
        broadcaseMessageTruncation()
        broadcastMessageBatchly("已从会话断开 $identity 因为会话已结束\n")
//        println("开始刷新缓冲区")

        // 关掉连接管理器
        connectionManager.closeAndWait()

//        println("cleanup")
        onProcessExit.invoke()
    }

    fun kill()
    {
        process.destroy()
    }

    /**
     * 使一个用户连接到当前会话上
     * @param user 要连接的用户
     * @return Connection 对象
     */
    fun connect(user: SessionUser): Connection
    {
        if(manager.hasUserConnectedToAnySession(user))
            throw SessionUserAlreadyConnectedException(pid)

        val whenOnlineChanged = connectionManager.getConnection(user, true)?.whenOnlineChanged ?: -1
        val (conn, isReconnection) = connectionManager.openConnection(user)

        if (isReconnection)
        {
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
                val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(last)
                sb.append("\n========== 最后输出（$ts）==========\n")

                conn.sendTruncation()
                conn.sendMessage(sb.toString())
                conn.sendTruncation()
            }
        }

        conn.sendMessage(if(isReconnection) "已重连到会话 $identity\n" else "会话已创建且已连接 $identity\n")
        conn.sendTruncation()

        return conn
    }

    /**
     * 断开某个用户与当前会话的连接
     * @param user 要断开的用户
     */
    fun disconnect(user: SessionUser): Connection
    {
        val connection = getConnection(user)
            ?: if(user is SessionUser.GroupUser)
                throw UserNotConnectedException(user)
            else
                throw UserNotConnectedException()

        // 发送消息
        connection.sendTruncation()
        connection.sendMessage("已从会话断开 $identity\n")
        connection.sendTruncation()
        connection.close()

        return connection
    }

    /**
     * 断开连接到当前会话上的所有用户
     */
    fun disconnectAll()
    {
        for (user in users)
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

    /**
     * 获取一个用户的连接
     */
    fun getConnection(user: SessionUser): Connection? = connectionManager.getConnection(user, includeOffline = false)

    /**
     * 判断用户是否连接到了当前会话上
     */
    fun isUserConnected(user: SessionUser): Boolean = getConnection(user) != null

    /**
     * 所有的在线用户
     */
    val users: Collection<SessionUser> get() = connectionManager.getConnections(includeOffline = false).map { it.user }

    /**
     * 所有在线的连接
     */
    val connections: Collection<Connection> get() = connectionManager.getConnections(includeOffline = false)

    /**
     * 会话的标识字符串
     */
    val identity: String get() = "$pid（${preset.name}）"

    /**
     * 验证预设的有效性
     */
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