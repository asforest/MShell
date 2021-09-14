package com.github.asforest.mshell.command

import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import com.github.asforest.mshell.MShell
import com.github.asforest.mshell.configuration.MainConfig
import com.github.asforest.mshell.permission.GrantingsUtil
import com.github.asforest.mshell.permission.MShellPermissions
import net.mamoe.mirai.console.permission.*
import net.mamoe.mirai.console.permission.PermissionService.Companion.cancel
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.console.permission.PermissionService.Companion.permit
import net.mamoe.mirai.console.permission.PermitteeId.Companion.permitteeId

object AdminsCommand : CompositeCommand(
    MShell,
    primaryName = "mshella",
    description = "MShell插件管理员配置指令",
    secondaryNames = arrayOf("msa", "ma"),
    parentPermission = MShellPermissions.all
) {
    @SubCommand @Description("添加管理员")
    suspend fun CommandSender.add(
        @Name("qq号") qqnumber: Long
    ) {
        val permitte = AbstractPermitteeId.ExactFriend(qqnumber)
        val permission = MShellPermissions.all
        if (!permitte.hasPermission(permission))
            permitte.permit(permission)
        list()
    }

    @SubCommand @Description("删除管理员")
    suspend fun CommandSender.remove(
        @Name("qq号") qqnumber: Long
    ) {
        val permittee = AbstractPermitteeId.ExactFriend(qqnumber)
        val permission = MShellPermissions.all
        if(permittee.hasPermission(permission))
        {
            permittee.cancel(permission, false)
            sendMessage("已移除($qqnumber)")
        } else {
            sendMessage("没有这个管理员($qqnumber)，不需要删除")
        }
    }

    @SubCommand @Description("列出所有管理员")
    suspend fun CommandSender.list() {
        val f = GrantingsUtil.grantings.filterIsInstance<AbstractPermitteeId.ExactFriend>()
        var output = ""
        for ((idx, p) in f.withIndex()) {
            output += "$idx: ${p.id}\n"
        }
        sendMessage(output.ifEmpty { " " })
    }
}