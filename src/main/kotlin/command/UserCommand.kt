package com.github.asforest.mshell.command

import com.github.asforest.mshell.MShellPlugin
import com.github.asforest.mshell.configuration.PresetsConfig
import com.github.asforest.mshell.configuration.PermissionsConfig
import com.github.asforest.mshell.permission.MShellPermissions
import com.github.asforest.mshell.util.MShellUtils.getFriendNick
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand

object UserCommand : CompositeCommand(
    MShellPlugin,
    primaryName = "mshellu",
    description = "MShell插件用户管理指令",
    secondaryNames = arrayOf("msu", "mu"),
    parentPermission = MShellPermissions.all
) {
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

    fun getUsersCountOfPreset(preset: String) = PermissionsConfig.getUserCountOfPreset(preset)
}