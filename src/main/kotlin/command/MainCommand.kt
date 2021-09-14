
package com.github.asforest.mshell.command

import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import com.github.asforest.mshell.MShell
import com.github.asforest.mshell.configuration.EnvPresets
import com.github.asforest.mshell.exception.*
import com.github.asforest.mshell.session.Session
import com.github.asforest.mshell.session.SessionManager
import com.github.asforest.mshell.type.USER

object MainCommand : CompositeCommand(
    MShell,
    primaryName = "mshell",
    description = "MShell插件主指令",
    secondaryNames = arrayOf("ms")
) {
    @SubCommand @Description("开启一个会话并将当前用户连接到这个会话")
    suspend fun CommandSender.open(
        @Name("preset") preset: String? = null
    ) {
        withCatch {
            SessionManager.openSessionAutomatically(user, preset)
        }
    }

    @SubCommand @Description("向当前连接的会话stdin里输出内容")
    suspend fun CommandSender.write(
        @Name("text") vararg text: String
    ) {
        for (session in SessionManager.sessions)
            session.stdin.println(text.joinToString(" "))
    }

    @SubCommand @Description("强制断开一个会话的所有连接")
    suspend fun CommandSender.disconnect(
        @Name("pid") pid: Long
    ) {
        withCatch {
            getSessionByPidWithThrow(pid).disconnectAll()
        }
    }

    @SubCommand @Description("强制结束一个会话")
    suspend fun CommandSender.kill(
        @Name("pid") pid: Long
    ) {
        withCatch {
            getSessionByPidWithThrow(pid).kill()
        }
    }

    @SubCommand @Description("连接到一个会话")
    suspend fun CommandSender.connect(
        @Name("pid") pid: Long
    ) {
        withCatch {
            SessionManager.connectToSession(user, pid)
        }
    }

    @SubCommand @Description("断开当前会话")
    suspend fun CommandSender.disconnect()
    {
        withCatch {
            SessionManager.disconnectFromSession(user)
        }
    }

    @SubCommand @Description("显示所有会话")
    suspend fun CommandSender.list()
    {
        var output = ""

        for ((index, session) in SessionManager.sessions.withIndex())
        {
            val pid = session.pid
            val usersConnected = session.usersConnected.map { u -> u.name}
            output += "[$index] $pid: $usersConnected\n"
        }
        sendMessage(output.ifEmpty { " " })
    }

    @SubCommand @Description("显示资源消耗情况")
    suspend fun CommandSender.status()
    {
        var output = " "

        val executor = SessionManager.scd.executor
        val active = executor.activeCount
        val queue = executor.queue.size
        val poolSize = executor.poolSize

        output += "Active: $active\n"
        output += "Queue: $queue\n"
        output += "PoolSize: $poolSize\n"

        sendMessage(output.ifEmpty { " " })
    }

    fun getPresetWithThrow(presetName: String): EnvPresets.Preset
    {
        return MShell.ep.ins.presets[presetName]
            ?: throw PresetNotFoundException("The preset '$presetName' was not found")
    }

    fun getSessionByPidWithThrow(pid: Long): Session
    {
        return SessionManager.getSessionByPid(pid)
            ?: throw SessionNotFoundException("The session of pid($pid) was not be found")
    }

    suspend inline fun CommandSender.withCatch(block: CommandSender.() -> Unit)
    {
        try { block() } catch (e: BaseException) { sendMessage(e.message ?: e.stackTraceToString()) }
    }

    val USER.name: String get() = if(this != null) "$nick($id)" else "<Console>"
}