package com.github.asforest.mshell.command

import com.github.asforest.mshell.MShellPlugin
import com.github.asforest.mshell.permission.MShellPermissions
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand

object UserCommand : CompositeCommand(
    MShellPlugin,
    primaryName = "mshellu",
    description = "MShell插件预设授权用户普通指令",
    secondaryNames = arrayOf("msu", "mu"),
    parentPermission = MShellPermissions.use
) {
    @SubCommand
    suspend fun CommandSender.sd()
    {
        println("safasfsafsafasf")
    }
}