package com.github.asforest.mshell.command

import com.github.asforest.mshell.command.MShellCommand.Admin
import com.github.asforest.mshell.command.MShellCommand.CallingContext
import com.github.asforest.mshell.command.resolver.TreeCommand
import com.github.asforest.mshell.configuration.PresetsConfig
import com.github.asforest.mshell.permission.MShellPermissions
import com.github.asforest.mshell.permission.PresetGrants
import com.github.asforest.mshell.session.SessionManager
import com.github.asforest.mshell.session.SessionUser
import com.github.asforest.mshell.util.MShellUtils.getFriendNick
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.permission.AbstractPermitteeId
import net.mamoe.mirai.console.permission.PermissionService.Companion.cancel
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.console.permission.PermissionService.Companion.permit
import net.mamoe.mirai.console.permission.PermitteeId

object AuthCommand : TreeCommand()
{
    @Command(desc = "添加管理员", permission = Admin)
    suspend fun CallingContext.add(qqnumber: Long)
    {
        val friend = sender.getFriendNick(qqnumber)
        val permitte = AbstractPermitteeId.ExactFriend(qqnumber)
        val permission = MShellPermissions.root
        if (!permitte.hasPermission(permission))
        {
            permitte.permit(permission)
            sendMessage("已添加管理员$friend，当前共有${adminCount}位管理员")
        } else {
            sendMessage("${friend}已是管理员，不需要重复添加，当前共有${adminCount}位管理员")
        }
    }

    @Command(desc = "移除管理员", permission = Admin)
    suspend fun CallingContext.remove(qqnumber: Long)
    {
        val friend = sender.getFriendNick(qqnumber)
        val permittee = AbstractPermitteeId.ExactFriend(qqnumber)
        val permission = MShellPermissions.root
        if(permittee.hasPermission(permission))
        {
            // 实时断开链接
            getFriendUser(qqnumber)?.also { SessionManager.tryToDisconnect(it) }

            // 撤销授权
            permittee.cancel(permission, false)
            sendMessage("已移除管理员$friend，当前共有${adminCount}位管理员")
        } else {
            sendMessage("没有这个管理员$friend，当前共有${adminCount}位管理员)")
        }
    }

    @Command(desc = "列出所有管理员和授权用户", permission = Admin)
    suspend fun CallingContext.list() {
        val f = adminGrantings.filterIsInstance<AbstractPermitteeId.ExactFriend>()

        sendMessage("-----管理员列表-----")

        sendMessage(f.withIndex().joinToString("\n") { (index, user) ->
            "[$index] ${sender.getFriendNick(user.id)}\n"
        }.ifEmpty { "还没有任何管理员" })

        sendMessage("\n-----授权用户列表-----")

        sendMessage(PresetGrants.allPresetGrantings.entries.withIndex().joinToString("\n") { (index, grants) ->
            val _preset = grants.key.name.substring(PresetGrants.Prefix.length)
            val friends = grants.value.filterIsInstance<AbstractPermitteeId.ExactFriend>()
                .map { sender.getFriendNick(it.id) }
            "[$index] $_preset: $friends"
        }.ifEmpty { "还没有任何授权用户" })
    }

    @Command(desc = "添加预设授权用户", permission = Admin)
    suspend fun CallingContext.adduser(preset: String, qqnumber: Long)
    {
        if(preset !in PresetsConfig.presets)
        {
            sendMessage("预设不存在：$preset")
            return
        }

        val friend = sender.getFriendNick(qqnumber)

        if(PresetGrants.addGrant(preset, qqnumber))
        {
            sendMessage("已添加预设授权用户${friend}，预设${preset}当前共有${getUsersCount(preset)}位预设授权用户")
        } else {
            sendMessage("预设授权用户添加失败，${friend}已是预设${preset}的授权用户")
        }
    }

    @Command(desc = "移除预设授权用户", permission = Admin)
    suspend fun CallingContext.removeuser(preset: String, qqnumber: Long)
    {
        if(preset !in PresetsConfig.presets)
        {
            sendMessage("预设不存在：$preset")
            return
        }

        val friend = sender.getFriendNick(qqnumber)

        if(PresetGrants.removeGrant(preset, qqnumber))
        {
            sendMessage("已移除预设授权用户${friend}，预设${preset}当前共有${getUsersCount(preset)}位预设授权用户")
        } else {
            sendMessage("预设授权用户移除失败，${friend}不是预设${preset}的授权用户")
        }
    }

    @Command(desc = "列出所有预设授权用户", permission = Admin)
    suspend fun CallingContext.listuser(preset: String? = null)
    {
        if(preset != null && preset !in PresetsConfig.presets)
        {
            sendMessage("预设不存在：$preset")
            return
        }

        var output = ""
        if(preset == null)
        {
            for ((idx, kvp) in PresetGrants.allPresetGrantings.entries.withIndex())
            {
                val _preset = kvp.key.name.substring(PresetGrants.Prefix.length)
                val friends = kvp.value.filterIsInstance<AbstractPermitteeId.ExactFriend>()
                    .map { sender.getFriendNick(it.id) }
                output += "[$idx] $_preset: $friends\n"
            }
        } else {
            val friends = PresetGrants[preset]!!.filterIsInstance<AbstractPermitteeId.ExactFriend>()
                .map { sender.getFriendNick(it.id) }

            output += "$preset: $friends\n"
        }

        sendMessage(output.ifEmpty { "还没有任何预设授权用户" })
    }

    private fun CallingContext.getFriendUser(qqnumber: Long): SessionUser.FriendUser?
    {
        val _bot = sender.bot
        if(_bot != null)
            return SessionUser.FriendUser(_bot.getFriend(qqnumber) ?: return null)

        Bot.instances.forEach { bot ->
            val friend = bot.getFriend(qqnumber)
            if(friend != null)
                return SessionUser.FriendUser(friend)
        }

        return null
    }

    /**
     * 获取预设授权用户的数量
     */
    private fun getUsersCount(preset: String) = PresetGrants[preset]?.size ?: 0

    /**
     * 获取管理员的数量
     */
    private val adminCount get() = adminGrantings.filterIsInstance<AbstractPermitteeId.ExactFriend>().size

    /**
     * 获取所有管理员
     */
    private val adminGrantings: Collection<PermitteeId> get() =
        MShellPermissions.permissionMap
            .filter { it.key == MShellPermissions.root.id }
            .firstNotNullOfOrNull { it.value } ?: mutableListOf()
}