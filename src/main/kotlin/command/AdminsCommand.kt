package com.github.asforest.mshell.command

import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import com.github.asforest.mshell.MShell
import com.github.asforest.mshell.exception.UserDidnotConnectedYetException
import com.github.asforest.mshell.permission.PermissionUtil
import com.github.asforest.mshell.permission.MShellPermissions
import com.github.asforest.mshell.session.SessionManager
import com.github.asforest.mshell.session.SessionUser
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.permission.*
import net.mamoe.mirai.console.permission.PermissionService.Companion.cancel
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.console.permission.PermissionService.Companion.permit

object AdminsCommand : CompositeCommand(
    MShell,
    primaryName = "mshella",
    description = "MShell插件管理员配置指令",
    secondaryNames = arrayOf("msa", "ma"),
    parentPermission = MShellPermissions.all
) {
    @SubCommand @Description("添加管理员")
    suspend fun CommandSender.add(
        @Name("qq") qqnumber: Long
    ) {
        val friend = getFriendNickOrId(qqnumber)
        val permitte = AbstractPermitteeId.ExactFriend(qqnumber)
        val permission = MShellPermissions.all
        if (!permitte.hasPermission(permission))
        {
            permitte.permit(permission)
            sendMessage("已添加管理员 $friend (当前共有${adminCount}位管理员)")
        } else {
            sendMessage("$friend 已是管理员，不需要重复添加(当前共有${adminCount}位管理员)")
        }
    }

    @SubCommand @Description("移除管理员")
    suspend fun CommandSender.remove(
        @Name("qq") qqnumber: Long
    ) {
        val friend = getFriendNickOrId(qqnumber)
        val permittee = AbstractPermitteeId.ExactFriend(qqnumber)
        val permission = MShellPermissions.all
        if(permittee.hasPermission(permission))
        {
            // 断开已建立的链接
            for (bot in Bot.instances)
            {
                val user = bot.getFriend(qqnumber)
                if(user != null)
                    try {
                        SessionManager.disconnect(SessionUser(user))
                    } catch (e: UserDidnotConnectedYetException) { }
            }
            permittee.cancel(permission, false)
            sendMessage("已移除管理员 $friend (当前共有${adminCount}位管理员)")
        } else {
            sendMessage("没有这个管理员 $friend 不需要删除(当前共有${adminCount}位管理员)")
        }
    }

    @SubCommand @Description("列出所有管理员")
    suspend fun CommandSender.list() {
        val f = PermissionUtil.grantings.filterIsInstance<AbstractPermitteeId.ExactFriend>()
        var output = ""
        for ((idx, p) in f.withIndex()) {
            output += "[$idx] ${p.id}\n"
        }
        sendMessage(output.ifEmpty { "还没有任何管理员" })
    }

    private fun getFriendNickOrId(qqnumber: Long): String
    {
        Bot.instances.forEach { bot ->
            val f = bot.getFriend(qqnumber)
            if(f != null)
                return "${f.nick}(${f.id})"
        }
        return qqnumber.toString()
    }

    val adminCount by lazy { PermissionUtil.grantings.filterIsInstance<AbstractPermitteeId.ExactFriend>().size }
}