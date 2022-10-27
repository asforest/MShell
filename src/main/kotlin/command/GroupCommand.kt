package com.github.asforest.mshell.command

import com.github.asforest.mshell.MShellPlugin
import com.github.asforest.mshell.command.MShellCommandAbstract.CallContext
import com.github.asforest.mshell.command.resolver.TreeCommand
import com.github.asforest.mshell.exception.business.AmbiguousGroupIdException
import com.github.asforest.mshell.exception.business.QQGroupNotFoundException
import com.github.asforest.mshell.session.SessionManager
import com.github.asforest.mshell.session.SessionUser
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Group

object GroupCommand : TreeCommand()
{
    @Command(desc = "开启一个会话并将这个会话连接到一个群聊", permission = MShellCommandAbstract.Admin)
    suspend fun CallContext.open(group: Long, preset: String? = null)
    {
        withCatch {
            val groupUser = getGroupUser(group)
            val session = SessionManager.createSession(preset, groupUser)

            sendMessage("会话 ${session.identity} 已创建到群聊 $groupUser")
        }
    }

    @Command(desc = "将一个群聊连接到一个会话上", permission = MShellCommandAbstract.Admin)
    suspend fun CallContext.connect(group: Long, pid: Long)
    {
        withCatch {
            val groupUser = getGroupUser(group)
            val conn = SessionManager.connect(groupUser, pid)

            sendMessage("会话 ${conn.session.identity} 已连接到群聊 $groupUser")
        }
    }

    @Command(desc = "断开一个群聊的会话", permission = MShellCommandAbstract.Admin)
    suspend fun CallContext.disconnect(group: Long)
    {
        withCatch {
            val groupUser = getGroupUser(group)
            val conn = SessionManager.disconnect(groupUser)

            sendMessage("会话 ${conn.session.identity} 已从群聊 $groupUser 上断开")
        }
    }

    private fun CallContext.getGroupUser(groupId: Long): SessionUser.GroupUser
    {
        val groupIdStr = groupId.toString()

        // 群聊号码简写支持
        val groups = mutableMapOf<String, Group>()
        for (bot in Bot.instances)
            for (group in bot.groups)
                groups[group.id.toString()] = group

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

        return SessionUser.GroupUser(groupMatched)
    }

    private suspend inline fun CallContext.withCatch(block: CallContext.() -> Unit)
    {
        MShellPlugin.catchException(sender.user) { block() }
    }
}