package com.github.asforest.mshell.command.mshell

import com.github.asforest.mshell.MShellPlugin
import com.github.asforest.mshell.command.SmartCommand
import com.github.asforest.mshell.command.mshell.MainCommand.help
import com.github.asforest.mshell.command.resolver.AbstractArgumentParsers
import com.github.asforest.mshell.command.resolver.AbstractSmartCommand
import com.github.asforest.mshell.command.resolver.CommandSignature
import com.github.asforest.mshell.permission.MShellPermissions
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.permission.PermissionService.Companion.testPermission
import net.mamoe.mirai.message.data.MessageChain

object MShellCommand : SmartCommand(
    owner = MShellPlugin,
    primaryName = "mshell",
    description = "MShell插件指令",
    secondaryNames = arrayOf("ms"),
    permission = MShellPermissions.use
) {
    const val Nobody = 0
    const val User = 1 shl 0
    const val Admin = 1 shl 1
    const val All = User or Admin

    override val subCommandFunctions: Map<String, CommandSignature> by lazy {
        MainCommand.allCommandFunctions
    }

    override suspend fun CommandSender.onCommand(args: MessageChain)
    {
        val commandText = args.joinToString(" ") { it.contentToString() }.trim()

        try {
            val senderPermission = if (user == null) {
                All
            } else {
                if (MShellPermissions.root.testPermission(permitteeId))
                    Admin
                else if (MShellPermissions.use.testPermission(permitteeId))
                    User
                else
                    Nobody
            }

            val afun = MainCommand.resolveCommandText(commandText, senderPermission)
            if (afun != null) afun.callSuspend(this) else this.help()
        } catch (e: AbstractArgumentParsers.ArgumentParserException) {
            sendMessage("参数#${e.argIndex + 3} '${e.raw}'不是${e.typeExcepted.simpleName}类型")
        } catch (e: AbstractSmartCommand.PermissionDeniedException) {
            sendMessage("权限不够")
        }
    }

    override suspend fun CommandSender.onDefaultCommand()
    {
        this.help()
    }

}