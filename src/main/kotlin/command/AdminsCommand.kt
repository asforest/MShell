package com.github.asforest.mshell.command

import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import com.github.asforest.mshell.MShell
import com.github.asforest.mshell.configuration.MainConfig

object AdminsCommand : CompositeCommand(
    MShell,
    primaryName = "mshella",
    description = "MShell插件管理员配置指令",
    secondaryNames = arrayOf("msa", "ma")
) {
    @SubCommand @Description("添加管理员")
    suspend fun CommandSender.add(
        @Name("qq号") qqnumber: Long
    ) {
        withPermission {
            if(qqnumber !in MainConfig.admins)
                MainConfig.admins += qqnumber

            var output = ""
            for((k, v) in MainConfig.admins.withIndex())
                output += "$k: $v\n"
            sendMessage(output)
        }
    }

    @SubCommand @Description("删除管理员")
    suspend fun CommandSender.remove(
        @Name("索引") index: Int
    ) {
        withPermission {
            if(index >= MainConfig.admins.size)
            {
                sendMessage("索引($index)过大，范围:(0 ~ ${MainConfig.admins.size - 1})")
            } else if (index < 0) {
                sendMessage("索引($index)过小，最小只能是0")
            } else {
                sendMessage("已移除(${MainConfig.admins[index]})")
                MainConfig.admins.removeAt(index)
            }
        }
    }

    @SubCommand @Description("列出所有管理员")
    suspend fun CommandSender.list() {
        withPermission {
            var output = ""
            for((k, v) in MainConfig.admins.withIndex())
                output += "$k: $v\n"
            sendMessage(output)
        }
    }

    suspend inline fun CommandSender.withPermission(block: CommandSender.() -> Unit)
    {
        if(user == null || user!!.id in MainConfig.admins)
            block()
    }
}