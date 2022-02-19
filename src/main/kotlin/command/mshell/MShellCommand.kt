package com.github.asforest.mshell.command.mshell

import com.github.asforest.mshell.MShellPlugin
import com.github.asforest.mshell.command.MiraiTreeCommand
import com.github.asforest.mshell.command.resolver.AbstractArgumentParsers
import com.github.asforest.mshell.command.resolver.TreeCommand
import com.github.asforest.mshell.command.resolver.PrefixedCommandSignature
import com.github.asforest.mshell.permission.MShellPermissions
import com.github.asforest.mshell.session.SessionUser
import com.github.asforest.mshell.util.MShellUtils.toSessionUser
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.permission.PermissionService.Companion.testPermission
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.message.MessageReceipt
import net.mamoe.mirai.message.data.MessageChain

object MShellCommand : MiraiTreeCommand(
    owner = MShellPlugin,
    primaryName = "mshell",
    secondaryNames = arrayOf("ms"),
    description = "MShell插件指令",
    permission = MShellPermissions.use
) {
    const val Nobody = 0
    const val User = 1 shl 0
    const val Admin = 1 shl 1
    const val All = User or Admin

    override val subCommandFunctions: List<PrefixedCommandSignature> by lazy {
        MainCommand.allCommands
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

            val afun = MainCommand.resolveCommandText(commandText.split(" "), senderPermission)
            if (afun != null) {
                afun.callSuspend(CallContext(this, senderPermission))
            } else {
                if (commandText.isEmpty())
                    sendMessage("输入 /$primaryName help 来查看帮助信息")
                else
                    sendMessage("未知指令。输入 /$primaryName help 来查看帮助信息")
            }
        } catch (e: AbstractArgumentParsers.ArgumentParserException) {
            sendMessage("参数#${e.argIndex + 3} '${e.raw}'不是${e.typeExcepted.simpleName}类型")
        } catch (e: TreeCommand.PermissionDeniedException) {
            sendMessage("权限不够")
        }
    }

    data class CallContext(val sender: CommandSender, val permission: Int)
    {
        val isUser = permission and User
        val isAdmin = permission and Admin

        suspend fun sendMessage(message: String): MessageReceipt<Contact>? = sender.sendMessage(message)

        fun toSessionUser(): SessionUser = sender.toSessionUser()
    }

}