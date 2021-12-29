
package com.github.asforest.mshell.command

import com.github.asforest.mshell.MShellPlugin
import com.github.asforest.mshell.exception.external.BaseExternalException
import com.github.asforest.mshell.exception.external.NoSuchSessionException
import com.github.asforest.mshell.exception.external.UserAlreadyConnectedException
import com.github.asforest.mshell.exception.external.UserDidnotConnectedYetException
import com.github.asforest.mshell.permission.MShellPermissions
import com.github.asforest.mshell.session.Session
import com.github.asforest.mshell.session.SessionManager
import com.github.asforest.mshell.session.SessionUser
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand

object MainCommand : CompositeCommand(
    MShellPlugin,
    primaryName = "mshell",
    description = "MShell插件主指令",
    secondaryNames = arrayOf("ms"),
    parentPermission = MShellPermissions.all
) {
    @SubCommand @Description("开启一个会话并将当前用户连接到这个会话")
    suspend fun CommandSender.open(
        @Name("preset") preset: String? = null
    ) {
        withCatch {
            val user = SessionUser(user)
            if(SessionManager.hasUserConnectedToAnySession(user))
                throw UserAlreadyConnectedException()
            SessionManager.createSession(preset, user)
        }
    }

    @SubCommand @Description("向当前连接的会话stdin里输出内容并换行")
    suspend fun CommandSender.write(
        @Name("text") vararg text: String
    ) {
        withCatch {
            val session = SessionManager.getSessionByUserConnected(SessionUser(user))
                ?: throw throw UserDidnotConnectedYetException()
            session.stdin.println(text.joinToString(" "))
        }
    }

    @SubCommand @Description("向当前连接的会话stdin里输出内容但不换行")
    suspend fun CommandSender.write2(
        @Name("text") vararg text: String
    ) {
        withCatch {
            val session = SessionManager.getSessionByUserConnected(SessionUser(user))
                ?: throw throw UserDidnotConnectedYetException()
            session.stdin.print(text.joinToString(" "))
        }
    }

    @SubCommand @Description("向目标会话的stdin里输出内容并换行")
    suspend fun CommandSender.writeto(
        @Name("pid") pid: Long,
        @Name("text") vararg text: String
    ) {
        withCatch {
            val session = SessionManager.getSessionByPid(pid)
                ?: throw NoSuchSessionException(pid)
            session.stdin.println(text.joinToString(" "))
        }
    }

    @SubCommand @Description("向目标会话的stdin里输出内容但不换行")
    suspend fun CommandSender.writeto2(
        @Name("pid") pid: Long,
        @Name("text") vararg text: String
    ) {
        withCatch {
            val session = SessionManager.getSessionByPid(pid)
                ?: throw NoSuchSessionException(pid)
            session.stdin.print(text.joinToString(" "))
        }
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
            sendMessage("进程已终止($pid)")
        }
    }

    @SubCommand @Description("连接到一个会话")
    suspend fun CommandSender.connect(
        @Name("pid") pid: Long
    ) {
        withCatch {
            SessionManager.connect(SessionUser(user), pid, )
        }
    }

    @SubCommand @Description("断开当前会话")
    suspend fun CommandSender.disconnect()
    {
        withCatch {
            SessionManager.disconnect(SessionUser(user))
        }
    }

    @SubCommand @Description("显示所有会话")
    suspend fun CommandSender.list()
    {
        var output = ""
        for ((index, session) in SessionManager.getAllSessions().withIndex())
        {
            val pid = session.pid
            val usersConnected = session.usersConnected.map { u -> u.name}
            
            output += "[$index] pid: $pid: $usersConnected\n"
        }
        sendMessage(output.ifEmpty { " " })
    }

    @SubCommand @Description("模拟戳一戳(窗口抖动)消息")
    suspend fun CommandSender.poke()
    {
        val user = SessionUser(user)
        val session = SessionManager.getSessionByUserConnected(user)

        if(session != null)
        {
            session.disconnect(user)
        } else {
            SessionManager.reconnectOrCreate(user)
        }
    }

    fun getSessionByPidWithThrow(pid: Long): Session
    {
        return SessionManager.getSessionByPid(pid)
            ?: throw NoSuchSessionException(pid)
    }

    suspend inline fun CommandSender.withCatch(block: CommandSender.() -> Unit)
    {
        try { block() } catch (e: BaseExternalException) { sendMessage(e.message ?: e.stackTraceToString()) }
    }
}