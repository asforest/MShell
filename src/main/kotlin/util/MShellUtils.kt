package com.github.asforest.mshell.util

import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.command.CommandSender

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
}