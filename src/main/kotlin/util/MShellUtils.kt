package com.github.asforest.mshell.util

import com.github.asforest.mshell.session.SessionUser
import com.github.asforest.mshell.session.user.ConsoleUser
import com.github.asforest.mshell.session.user.FriendUser
import com.github.asforest.mshell.session.user.GroupUser
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.getGroupOrNull
import net.mamoe.mirai.console.command.isConsole

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
            return "(Anyone)"

        return "$qqnumber(Unknown)"
    }

    fun CommandSender.getFriendNick(qqnumber: Long): String
    {
        return getFriendNick(qqnumber, bot)
    }

    /**
     * 根据指令发送人获取对应类型的SessionUser
     */
    fun getSessionUser(commandSender: CommandSender): SessionUser
    {
        val group = commandSender.getGroupOrNull()

        // 目前Mirai好像无法在群聊里执行指令，那么永远不会进入此if分支
        if (group != null)
            return GroupUser(group)

        if (commandSender.isConsole())
            return ConsoleUser()

        return FriendUser(commandSender.user!!)
    }
}