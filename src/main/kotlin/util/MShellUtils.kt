package com.github.asforest.mshell.util

import com.github.asforest.mshell.session.SessionUser
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.ConsoleCommandSender
import net.mamoe.mirai.console.command.getGroupOrNull
import net.mamoe.mirai.console.command.isConsole
import net.mamoe.mirai.contact.User

object MShellUtils
{
    private fun getFriendNick(qqnumber: Long, bot: Bot?): String
    {
        if(bot != null)
        {
            val friend = bot.getFriend(qqnumber)
            if(friend != null)
                return "${friend.nick}(${friend.id})"
        } else {
            Bot.instances.forEach { bot ->
                val friend = bot.getFriend(qqnumber)
                if(friend != null)
                    return "${friend.nick}(${friend.id})"
            }
        }

        if(qqnumber == 0L)
            return "0(Anyone)"

        return "UnknownUser($qqnumber)"
    }

    fun CommandSender.getFriendNick(qqnumber: Long): String
    {
        return getFriendNick(qqnumber, bot)
    }

    /**
     * 向MiraiUser或者Console发送消息（user为null时代表是Console）
     */
    suspend fun sendMessage2(user: User?, message: String)
    {
        user?.sendMessage(message) ?: ConsoleCommandSender.sendMessage(message)
    }

    /**
     * 根据指令发送人获取对应类型的SessionUser
     * @return GroupUser or ConsoleUser or GroupUser
     */
    fun CommandSender.toSessionUser(): SessionUser
    {
        val group = getGroupOrNull()

        // 目前Mirai好像无法在群聊里执行指令，那么永远不会进入此if分支
        if (group != null)
            return SessionUser.GroupUser(group)

        if (isConsole())
            return SessionUser.ConsoleUser()

        return SessionUser.FriendUser(user!!)
    }
}
