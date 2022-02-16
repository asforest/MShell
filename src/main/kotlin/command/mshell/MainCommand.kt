package com.github.asforest.mshell.command.mshell

import com.github.asforest.mshell.MShellPlugin
import com.github.asforest.mshell.command.mshell.MShellCommand.Admin
import com.github.asforest.mshell.command.mshell.MShellCommand.User
import com.github.asforest.mshell.command.resolver.AbstractSmartCommand
import com.github.asforest.mshell.configuration.MShellConfig
import com.github.asforest.mshell.exception.business.NoSuchSessionException
import com.github.asforest.mshell.exception.business.SessionUserAlreadyConnectedException
import com.github.asforest.mshell.session.Session
import com.github.asforest.mshell.session.SessionManager
import com.github.asforest.mshell.util.MShellUtils
import net.mamoe.mirai.console.command.CommandSender

object MainCommand : AbstractSmartCommand()
{
    @NestedCommand("auth")
    val authCommand = AuthCommandAbstract

    @NestedCommand("preset")
    val presetCommand = PresetCommand

    @CommandFunc(desc = "插件帮助信息", permission = User or Admin)
    suspend fun CommandSender.help()
    {
        sendMessage(buildString {
            for ((label, func) in allCommandFunctions)
            {
                append(listOf(
                    "/${MShellCommand.primaryName}",
                    label,
                    *func.parameters.map { p -> p.identity }.toTypedArray(),
                ).joinToString(" "))
                if(func.description.isNotEmpty())
                    append(": ${func.description}")
                append("\n")
            }
        })
    }

    @CommandFunc(desc = "开启一个会话并将当前用户连接到这个会话", permission = Admin)
    suspend fun CommandSender.open(preset: String? = null)
    {
        withCatch {
            val user = MShellUtils.getSessionUser(this)

            SessionManager.getSession(user)?.also {
                throw SessionUserAlreadyConnectedException(it.pid)
            }

            SessionManager.createSession(preset, user)
        }
    }

    @CommandFunc(desc = "向目标会话的stdin里输出内容", permission = Admin)
    suspend fun CommandSender.write(
        pid: Long,
        newline: Boolean,
        vararg text: String
    ) {
        withCatch {
            val session = SessionManager.getSession(pid)
                ?: throw NoSuchSessionException(pid)
            session.stdin.print(text.joinToString(" ") + (if(newline) "\n" else ""))
        }
    }

    @CommandFunc(desc = "强制断开一个会话的所有连接", permission = Admin)
    suspend fun CommandSender.disconnect(
        pid: Long
    ) {
        withCatch {
            getSessionByPidWithThrow(pid).disconnectAll()
        }
    }

    @CommandFunc(desc = "强制结束一个会话", permission = Admin)
    suspend fun CommandSender.kill(
        pid: Long
    ) {
        withCatch {
            getSessionByPidWithThrow(pid).kill()
            sendMessage("进程已终止($pid)")
        }
    }

    @CommandFunc(desc = "连接到一个会话", permission = Admin)
    suspend fun CommandSender.connect(
        pid: Long
    ) {
        withCatch {
            SessionManager.connect(MShellUtils.getSessionUser(this), pid, )
        }
    }

    @CommandFunc(desc = "断开当前会话", permission = Admin)
    suspend fun CommandSender.disconnect()
    {
        withCatch {
            SessionManager.disconnect(MShellUtils.getSessionUser(this))
        }
    }

    @CommandFunc(desc = "显示所有会话", permission = Admin)
    suspend fun CommandSender.list()
    {
        var output = ""
        for ((index, session) in SessionManager.sessions.withIndex())
        {
            val pid = session.pid
            val usersConnected = session.users

            output += "[$index] ${session.preset.name} | $pid: $usersConnected\n"
        }
        sendMessage(output.ifEmpty { "当前没有运行中的会话" })
    }

    @CommandFunc(desc = "模拟戳一戳(窗口抖动)消息", permission = Admin)
    suspend fun CommandSender.poke()
    {
        val user = MShellUtils.getSessionUser(this)
        val session = SessionManager.getSession(user)

        if(session != null)
        {
            session.disconnect(user)
        } else {
            SessionManager.reconnectOrCreate(user)
        }
    }

    @CommandFunc(desc = "重新加载config.yml配置文件", permission = Admin)
    suspend fun CommandSender.reload()
    {
        MShellConfig.read()
        sendMessage("config.yml配置文件重载完成")
    }

    private fun getSessionByPidWithThrow(pid: Long): Session
    {
        return SessionManager.getSession(pid) ?: throw NoSuchSessionException(pid)
    }

    private suspend inline fun CommandSender.withCatch(block: CommandSender.() -> Unit)
    {
        MShellPlugin.catchException(user) { block() }
    }
}