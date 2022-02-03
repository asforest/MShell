package com.github.asforest.mshell.command

import com.github.asforest.mshell.MShellPlugin
import com.github.asforest.mshell.exception.business.AmbiguousGroupIdException
import com.github.asforest.mshell.exception.business.QQGroupNotFoundException
import com.github.asforest.mshell.exception.business.UsingInConsoleNotAllowedException
import com.github.asforest.mshell.permission.MShellPermissions
import com.github.asforest.mshell.session.SessionManager
import com.github.asforest.mshell.session.user.GroupUser
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.command.isConsole
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.contact.Group

@ConsoleExperimentalApi
object GroupCommand : CompositeCommand(
    MShellPlugin,
    primaryName = "mshellg",
    description = "MShell插件Group指令",
    secondaryNames = arrayOf("mg", "msg"),
    parentPermission = MShellPermissions.all
) {
    @SubCommand @Description("开启一个会话并将这个会话连接到一个群聊")
    suspend fun CommandSender.open(
        @Name("group") groupId: Long,
        @Name("preset") preset: String? = null
    ) {
        withCatch {
            if(isConsole())
                throw UsingInConsoleNotAllowedException("/mshellg open")

            val groupUser = getSessionUser(groupId)

            val session = SessionManager.createSession(preset, groupUser)

            sendMessage("会话(pid: ${session.pid})已创建到 ${groupUser.group.id}(${groupUser.group.name})")
        }
    }

    @SubCommand @Description("将一个群聊连接到一个会话上")
    suspend fun CommandSender.connect(
        @Name("group") groupId: Long,
        @Name("pid") pid: Long
    ) {
        withCatch {
            if(isConsole())
                throw UsingInConsoleNotAllowedException("/mshellg open")

            val groupUser = getSessionUser(groupId)
            val session = SessionManager.connect(groupUser, pid)

            sendMessage("会话(pid: ${session.pid})已连接到 ${groupUser.group.id}(${groupUser.group.name})")
        }
    }

    @SubCommand @Description("断开一个群聊的会话")
    suspend fun CommandSender.disconnect(
        @Name("group") groupId: Long
    ) {
        withCatch {
            if(isConsole())
                throw UsingInConsoleNotAllowedException("/mshellg disconnect")

            val groupUser = getSessionUser(groupId)
            val session = SessionManager.disconnect(groupUser)

            sendMessage("会话(pid: ${session.pid})已断开 ${groupUser.group.id}(${groupUser.group.name})")
        }
    }

    private fun CommandSender.getSessionUser(groupId: Long): GroupUser
    {
        val _bot = bot ?: throw QQGroupNotFoundException(groupId)
        val groupIdStr = groupId.toString()

        // 群聊号码简写支持
        val groups = mutableMapOf<String, Group>()
        _bot.groups.forEach { groups[it.id.toString()] = it }

        var groupMatched: Group? = null
        for (key in groups.keys)
        {
            if(key.startsWith(groupIdStr))
            {
                if(groupMatched != null)
                    throw AmbiguousGroupIdException(groupId, groupMatched, groups[key]!!)
                groupMatched = groups[key]
            }
        }

        if(groupMatched == null)
            throw QQGroupNotFoundException(groupId)

        return GroupUser(groupMatched)
    }

    private suspend inline fun CommandSender.withCatch(block: CommandSender.() -> Unit)
    {
        MShellPlugin.catchException(user) { block() }
    }
}