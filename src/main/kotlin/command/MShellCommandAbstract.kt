package com.github.asforest.mshell.command

import com.github.asforest.mshell.MShellPlugin
import com.github.asforest.mshell.command.resolver.AbstractArgumentParsers
import com.github.asforest.mshell.command.resolver.CommandSignature
import com.github.asforest.mshell.command.resolver.TreeCommand
import com.github.asforest.mshell.permission.MShellPermissions
import com.github.asforest.mshell.session.SessionUser
import com.github.asforest.mshell.util.MShellUtils.buildUsage
import com.github.asforest.mshell.util.MShellUtils.toSessionUser
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.permission.PermissionService.Companion.testPermission
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.message.MessageReceipt
import net.mamoe.mirai.message.data.MessageChain

object MShellCommandAbstract : AbstractMiraiTreeCommand(
    owner = MShellPlugin,
    primaryName = "mshell",
    secondaryNames = arrayOf("ms"),
    description = "MShell插件指令",
    permission = MShellPermissions.use
) {
    val rootLabal = secondaryNames.first()

    const val Nobody = 0
    const val User = 1 shl 0
    const val Admin = 1 shl 1
    const val All = User or Admin

    override val subCommandFunctions: List<TreeCommand.PrefixedCommandSignature> by lazy {
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

            val prefix = rootLabal
            val arguments = commandText.split(" ").filter { it.isNotEmpty() }

            if (arguments.isNotEmpty())
            {
                val afun = MainCommand.resolveCommandText(prefix, arguments, senderPermission)
                afun.callSuspend(CallContext(this, senderPermission))
            } else {
                sendMessage("输入 /$prefix help 来查看帮助信息")
            }
        } catch (e: TreeCommand.MissingSubCommandException) {
            sendMessage("可用的子指令：\n${e.availables.generateUsage(e.prefix)}")
        } catch (e: TreeCommand.NoFunctionMatchedException) {
            sendMessage("未知子指令 ${e.label} 。可用的子指令：${e.availables.joinToString(", ") { it.name }}")
        } catch (e: TreeCommand.TooFewArgumentsException) {
            val signature = listOf(e.prefix, e.signature.name, e.signature.params)
                .filter { it.isNotEmpty() }
                .joinToString(" ")
            sendMessage("参数太少。正确的参数列表：$signature")
        } catch (e: TreeCommand.TooManyArgumentsException) {
            val signature = listOf(e.prefix, e.signature.name, e.signature.params)
                .filter { it.isNotEmpty() }
                .joinToString(" ")
            sendMessage("参数太多。正确的参数列表：$signature")
        } catch (e: AbstractArgumentParsers.ArgumentParserException) {
            sendMessage("参数#${e.argIndex + 3} '${e.raw}'不是${e.typeExcepted.simpleName}类型")
        } catch (e: TreeCommand.PermissionDeniedException) {
            sendMessage("权限不够")
        }
    }

    private fun List<CommandSignature>.generateUsage(prefix: String): String
    {
        return buildString {
            for (funcation in this@generateUsage)
            {
                append("/")
                append(buildUsage(prefix, funcation))
                append("\n")
            }
        }.trim()
    }

    data class CallContext(val sender: CommandSender, val permission: Int)
    {
        val isUser = permission and User
        val isAdmin = permission and Admin

        suspend fun sendMessage(message: String): MessageReceipt<Contact>? = sender.sendMessage(message)

        fun toSessionUser(): SessionUser = sender.toSessionUser()
    }

}