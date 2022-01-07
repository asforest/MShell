package com.github.asforest.mshell.command

import com.github.asforest.mshell.MShellPlugin
import com.github.asforest.mshell.authentication.Authentication
import com.github.asforest.mshell.exception.external.*
import com.github.asforest.mshell.permission.MShellPermissions
import com.github.asforest.mshell.permission.PresetGrants
import com.github.asforest.mshell.session.Session
import com.github.asforest.mshell.session.SessionManager
import com.github.asforest.mshell.session.user.FriendUser
import com.github.asforest.mshell.util.MShellUtils
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.util.ConsoleExperimentalApi

@ConsoleExperimentalApi
object UserCommand : CompositeCommand(
    MShellPlugin,
    primaryName = "mshellu",
    description = "MShell插件预设授权用户普通指令",
    secondaryNames = arrayOf("msu", "mu"),
    parentPermission = MShellPermissions.use
) {
    @SubCommand @Description("开启一个会话并连接到这个会话")
    suspend fun CommandSender.open(
        @Name("preset") preset: String? = null
    ) {
        withCatch {
            val user = getUserWithCheck("/mshellu open")

            SessionManager.getSessionByUserConnected(user)?.also {
                throw SessionUserAlreadyConnectedException(it.pid)
            }

            val _preset = Authentication.useDefaultPreset(preset, user.user.id)
            SessionManager.createSession(_preset.name, user)
        }
    }

    @SubCommand @Description("向当前连接的会话stdin里输出内容但不换行")
    suspend fun CommandSender.write(
        @Name("end-with-newline") newline: Boolean,
        @Name("text") vararg text: String
    ) {
        withCatch {
            val user = getUserWithCheck("/mshellu write")

            val session = SessionManager.getSessionByUserConnected(MShellUtils.getSessionUser(this))
                ?: throw throw UserDidNotConnectedYetException()
            session.stdin.print(text.joinToString(" ") + (if(newline) "\n" else ""))
        }
    }

    @SubCommand @Description("强制结束一个会话")
    suspend fun CommandSender.kill(
        @Name("pid") pid: Long
    ) {
        withCatch {
            val session = getSessionByPidWithThrow(pid)
            val user = getUserWithCheck("/mshellu kill")

            if(!PresetGrants.testGrant(session.preset.name, user.user.id))
                throw NoPermissionToKillSessionException(session.pid)

            getSessionByPidWithThrow(pid).kill()
            sendMessage("进程已终止($pid)")
        }
    }

    @SubCommand @Description("连接到一个会话")
    suspend fun CommandSender.connect(
        @Name("pid") pid: Long
    ) {
        withCatch {
            val session = getSessionByPidWithThrow(pid)
            val user = getUserWithCheck("/mshellu connect")

            if(!PresetGrants.testGrant(session.preset.name, user.user.id))
                throw NoPermissionToConnectToASessionException(session.pid)

            SessionManager.connect(MShellUtils.getSessionUser(this), pid)
        }
    }

    @SubCommand @Description("显示所有会话")
    suspend fun CommandSender.list()
    {
        withCatch {
            val user = getUserWithCheck("/mshellu list")

            val sessions = SessionManager.getAllSessions()
                .filter { PresetGrants.testGrant(it.preset.name, user.user.id) }

            var output = ""
            for ((index, session) in sessions.withIndex()) {
                val pid = session.pid
                val usersConnected = session.usersConnected

                output += "[$index] pid: $pid: $usersConnected\n"
            }

            sendMessage(output.ifEmpty { "当前没有运行中的会话" })
        }
    }

    @SubCommand @Description("列出所有可用的环境预设")
    suspend fun CommandSender.presets()
    {
        withCatch {
            val user = getUserWithCheck("/mshellu list")
            val availablePresets = Authentication.getAvailablePresets(user.user.id)

            var ouput = ""
            for ((index, preset) in availablePresets.withIndex())
                ouput += "$index. ${preset.name}"

            sendMessage(ouput.ifEmpty { "没有可用的环境预设" })
        }
    }

    private fun CommandSender.getUserWithCheck(commandName: String): FriendUser
    {
        val user = MShellUtils.getSessionUser(this)

        if(user !is FriendUser)
            throw OnlyUsingInPrivateSessionException(commandName)

        return user
    }

    private fun getSessionByPidWithThrow(pid: Long): Session
    {
        return SessionManager.getSessionByPid(pid) ?: throw NoSuchSessionException(pid)
    }

    private suspend inline fun CommandSender.withCatch(block: CommandSender.() -> Unit)
    {
        try { block() } catch (e: BaseExternalException) { sendMessage(e.message ?: e.stackTraceToString()) }
    }
}