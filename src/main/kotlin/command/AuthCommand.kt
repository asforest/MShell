package com.github.asforest.mshell.command

import com.github.asforest.mshell.MShellPlugin
import com.github.asforest.mshell.configuration.PermissionsConfig
import com.github.asforest.mshell.configuration.PresetsConfig
import com.github.asforest.mshell.permission.MShellPermissions
import com.github.asforest.mshell.session.SessionManager
import com.github.asforest.mshell.session.user.FriendUser
import com.github.asforest.mshell.util.MShellUtils.getFriendNick
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.permission.*
import net.mamoe.mirai.console.permission.PermissionService.Companion.cancel
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.console.permission.PermissionService.Companion.permit
import net.mamoe.mirai.console.util.ConsoleExperimentalApi

@ConsoleExperimentalApi
object AuthCommand : CompositeCommand(
    MShellPlugin,
    primaryName = "mshella",
    description = "MShell插件管理员配置指令（仅能用于内置权限管理模块）",
    secondaryNames = arrayOf("msa", "ma"),
    parentPermission = MShellPermissions.all
) {
    @SubCommand @Description("添加管理员")
    suspend fun CommandSender.add(
        @Name("qq") qqnumber: Long
    ) {
        val friend = getFriendNick(qqnumber)
        val permitte = AbstractPermitteeId.ExactFriend(qqnumber)
        val permission = MShellPermissions.all
        if (!permitte.hasPermission(permission))
        {
            permitte.permit(permission)
            sendMessage("已添加管理员$friend，当前共有${adminCount}位管理员")
        } else {
            sendMessage("${friend}已是管理员，不需要重复添加，当前共有${adminCount}位管理员")
        }
    }

    @SubCommand @Description("移除管理员")
    suspend fun CommandSender.remove(
        @Name("qq") qqnumber: Long
    ) {
        val friend = getFriendNick(qqnumber)
        val permittee = AbstractPermitteeId.ExactFriend(qqnumber)
        val permission = MShellPermissions.all
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

    @SubCommand @Description("列出所有管理员")
    suspend fun CommandSender.list() {
        val f = grantings.filterIsInstance<AbstractPermitteeId.ExactFriend>()
        var output = ""
        for ((idx, p) in f.withIndex()) {
            output += "[$idx] ${p.id}\n"
        }

        sendMessage(output.ifEmpty { "还没有任何管理员" })
    }

    @SubCommand @Description("添加使用者")
    suspend fun CommandSender.add(
        @Name("preset") preset: String,
        @Name("qq") qqnumber: Long
    ) {
        if(preset !in PresetsConfig.presets)
        {
            sendMessage("预设不存在：$preset")
            return
        }

        val friend = getFriendNick(qqnumber)

        if(PermissionsConfig.addGrant(preset, qqnumber))
        {
            sendMessage("已添加预设授权用户${friend}，当前共有${getUsersCountOfPreset(preset)}位预设授权用户")
        } else {
            sendMessage("预设授权用户添加失败，${friend}已是${preset}预设的授权用户")
        }
    }

    @SubCommand @Description("移除使用者")
    suspend fun CommandSender.remove(
        @Name("preset") preset: String,
        @Name("qq") qqnumber: Long
    ) {
        if(preset !in PresetsConfig.presets)
        {
            sendMessage("预设不存在：$preset")
            return
        }

        val friend = getFriendNick(qqnumber)

        if(PermissionsConfig.removeGrant(preset, qqnumber))
        {
            sendMessage("已移除预设授权用户${friend}，当前共有${getUsersCountOfPreset(preset)}位预设授权用户")
        } else {
            sendMessage("预设授权用户移除失败，${friend}不是${preset}预设的授权用户")
        }
    }

    @SubCommand @Description("列出所有使用者")
    suspend fun CommandSender.list(
        @Name("preset") preset: String? = null
    ) {
        if(preset != null && preset !in PresetsConfig.presets)
        {
            sendMessage("预设不存在：$preset")
            return
        }

        var output = ""
        for ((idx, kvp,) in PermissionsConfig.getAllPersetGrantings(preset).entries.withIndex())
            output += "[$idx] ${kvp.key}: ${kvp.value.map { getFriendNick(it) }}\n"

        sendMessage(output.ifEmpty { "还没有任何使用者或者预设不存在" })
    }

    fun CommandSender.getFriendUser(qqnumber: Long): FriendUser?
    {
        val _bot = bot
        if(_bot != null)
            return FriendUser(_bot?.getFriend(qqnumber) ?: return null)

        Bot.instances.forEach { bot ->
            val friend = bot.getFriend(qqnumber)
            if(friend != null)
                return FriendUser(friend)
        }

        return null
    }

    fun getUsersCountOfPreset(preset: String) = PermissionsConfig.getUserCountOfPreset(preset)

    private val adminCount get() = grantings.filterIsInstance<AbstractPermitteeId.ExactFriend>().size

    private val grantings: MutableCollection<PermitteeId> get() =
        MShellPermissions.permissionMap
            .filter { it.key == MShellPermissions.all.id }
            .firstNotNullOfOrNull { it.value } ?: mutableListOf()

}