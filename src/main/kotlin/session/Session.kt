package com.github.asforest.mshell.session

import com.github.asforest.mshell.event.Event
import com.github.asforest.mshell.exception.SessionNotRegisteredException
import com.github.asforest.mshell.exception.external.PresetIsIncompeleteException
import com.github.asforest.mshell.exception.external.TerminalColumnRowsOutOfRangeException
import com.github.asforest.mshell.exception.external.UnsupportedCharsetException
import com.github.asforest.mshell.model.EnvironmentalPreset
import com.pty4j.PtyProcess
import com.pty4j.PtyProcessBuilder
import kotlinx.coroutines.*
import java.io.File
import java.io.PrintWriter
import java.nio.charset.Charset
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class Session(
    val manager: SessionManager,
    val preset: EnvironmentalPreset,
    val lastwillMessageLines: Int
) {
    val process: PtyProcess
    val stdin: PrintWriter
    val pid: Long get() = process.pid()
    val lwm = SessionLastwillMessage(lastwillMessageLines)
    var isLive = true

    val onProcessEnd = Event<Session, Session.() -> Unit>(this)

    private var coStdoutCollector: Job
    private var stdoutOpeningFlag = true

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

        stdin = PrintWriter(process.outputStream, true, charset)

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

                if(len != -1) { // 有消息时
                    val message = String(buffer, 0, len, charset);
                    // 发送给所有连接上的用户
                    usersConnected.forEach { it.sendMessage(message) }

                    // 保留遗言
                    lwm.append(message)
                } else {
                    process.inputStream.close()
                    stdoutOpeningFlag = false
                    break
                }
            }
        }
    }

    fun start()
    {
        // 启动stdout收集协程
        coStdoutCollector.start()

        // 当进程退出
        process.onExit().thenRun {
            onProcessEnd { it() }
            isLive = false
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

    /**
     * 向所有连接到当前Session上的用户广播消息
     * @param message 要发送的消息
     */
    fun broadcastMessageBatchly(message: String)
    {
        usersConnected.forEach { it.sendMessage(message) }
    }

    /**
     * 强制打断消息合并，拆分成2条消息发送
     */
    fun broadcaseMessageTruncation()
    {
        usersConnected.forEach { it.sendTruncation() }
    }

    fun isUserConnected(user: SessionUser): Boolean = manager.getSessionByUserConnected(user) == this

    val isConnected: Boolean get() = usersConnected.isNotEmpty()

    val usersConnected: Collection<SessionUser> get() = manager.getUsersConnectedTo(this)

    val connections: Collection<Connection> get() = manager.getConnectionManager(this)?.getConnections(false) ?: throw SessionNotRegisteredException(this)

    private fun validatePreset(preset: EnvironmentalPreset)
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