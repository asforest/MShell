package com.github.asforest.mshell.command.mshell

import com.github.asforest.mshell.command.mshell.MShellCommand.Admin
import com.github.asforest.mshell.command.resolver.AbstractSmartCommand
import com.github.asforest.mshell.configuration.PresetsConfig
import com.github.asforest.mshell.permission.MShellPermissions
import com.github.asforest.mshell.permission.PresetGrants
import com.github.asforest.mshell.session.SessionManager
import com.github.asforest.mshell.session.SessionUser
import com.github.asforest.mshell.util.MShellUtils.getFriendNick
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.permission.AbstractPermitteeId
import net.mamoe.mirai.console.permission.PermissionService.Companion.cancel
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.console.permission.PermissionService.Companion.permit
import net.mamoe.mirai.console.permission.PermitteeId

object AuthCommand : AbstractSmartCommand()
{
    @CommandFunc(desc = "添加管理员", permission = Admin)
    suspend fun CommandSender.add(qqnumber: Long)
    {
        val friend = getFriendNick(qqnumber)
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

    @CommandFunc(desc = "移除管理员", permission = Admin)
    suspend fun CommandSender.remove(qqnumber: Long)
    {
        val friend = getFriendNick(qqnumber)
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

    @CommandFunc(desc = "列出所有管理员", permission = Admin)
    suspend fun CommandSender.list() {
        val f = grantings.filterIsInstance<AbstractPermitteeId.ExactFriend>()
        var output = ""
        for ((idx, p) in f.withIndex()) {
            output += "[$idx] ${p.id}\n"
        }

        sendMessage(output.ifEmpty { "还没有任何管理员" })
    }

    @CommandFunc(desc = "添加预设授权用户", permission = Admin)
    suspend fun CommandSender.adduser(preset: String, qqnumber: Long)
    {
        if(preset !in PresetsConfig.presets)
        {
            sendMessage("预设不存在：$preset")
            return
        }

        val friend = getFriendNick(qqnumber)

        if(PresetGrants.addGrant(preset, qqnumber))
        {
            sendMessage("已添加预设授权用户${friend}，预设${preset}当前共有${getUsersCount(preset)}位预设授权用户")
        } else {
            sendMessage("预设授权用户添加失败，${friend}已是预设${preset}的授权用户")
        }
    }

    @CommandFunc(desc = "移除预设授权用户", permission = Admin)
    suspend fun CommandSender.removeuser(preset: String, qqnumber: Long)
    {
        if(preset !in PresetsConfig.presets)
        {
            sendMessage("预设不存在：$preset")
            return
        }

        val friend = getFriendNick(qqnumber)

        if(PresetGrants.removeGrant(preset, qqnumber))
        {
            sendMessage("已移除预设授权用户${friend}，预设${preset}当前共有${getUsersCount(preset)}位预设授权用户")
        } else {
            sendMessage("预设授权用户移除失败，${friend}不是预设${preset}的授权用户")
        }
    }

    @CommandFunc(desc = "列出所有预设授权用户", permission = Admin)
    suspend fun CommandSender.listuser(preset: String? = null)
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
                    .map { getFriendNick(it.id) }
                output += "[$idx] $_preset: $friends\n"
            }
        } else {
            val friends = PresetGrants[preset]!!.filterIsInstance<AbstractPermitteeId.ExactFriend>()
                .map { getFriendNick(it.id) }

            output += "$preset: $friends\n"
        }

        sendMessage(output.ifEmpty { "还没有任何预设授权用户" })
    }

    private fun CommandSender.getFriendUser(qqnumber: Long): SessionUser.FriendUser?
    {
        val _bot = bot
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
    private val adminCount get() = grantings.filterIsInstance<AbstractPermitteeId.ExactFriend>().size

    private val grantings: Collection<PermitteeId> get() =
        MShellPermissions.permissionMap
            .filter { it.key == MShellPermissions.root.id }
            .firstNotNullOfOrNull { it.value } ?: mutableListOf()
}