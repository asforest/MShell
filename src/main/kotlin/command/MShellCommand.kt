package com.github.asforest.mshell.command

import com.github.asforest.mshell.MShellPlugin
import com.github.asforest.mshell.command.resolver.AbstractArgumentParsers
import com.github.asforest.mshell.command.resolver.SmartCommand
import com.github.asforest.mshell.permission.MShellPermissions
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.RawCommand
import net.mamoe.mirai.message.data.MessageChain

object MShellCommand : RawCommand(
    MShellPlugin,
    primaryName = "m",
    description = "MShell插件指令",
    secondaryNames = arrayOf("m"),
    parentPermission = MShellPermissions.use
) {
    override suspend fun CommandSender.onCommand(args: MessageChain)
    {
        val commandText = args.map { it.contentToString() }.joinToString(" ")

        println(commandText)

        SubCommands.onCommand(commandText, this)
    }

    object SubCommands : SmartCommand()
    {
        lateinit var context: CommandSender

        suspend fun onCommand(commandText: String, ctx: CommandSender)
        {
            context = ctx

            try {
                val afun = resolveCommandText(commandText)
                if(afun != null)
                    afun.callSuspend(this)
                else
                    help()
            } catch (e: AbstractArgumentParsers.ArgumentParserException) {
                ctx.sendMessage("参数 #${e.argIndex + 3} '${e.raw}' 不能解析为类型 ${e.typeExcepted.simpleName}")
            }
        }

        @SmartCommandFunc(description = "插件插件帮助")
        suspend fun help()
        {
            var o = StringBuffer().run {
                for (func in smartFunctions.values)
                {
                    append("/$primaryName ${func.name} ")

                    for (parameter in func.parameters)
                    {
                        if(parameter.isOptional)
                            append("[$parameter] ")
                        else if (parameter.isVararg)
                            append("[$parameter..] ")
                        else
                            append("<$parameter> ")
                    }

                    if(func.description.isNotEmpty())
                        append("  -   ${func.description}")

                    append("\n")
                }

                this.toString()
            }

            context.sendMessage(o)
        }

        @SmartCommandFunc
        suspend fun q(vararg qa: String)
        {
            println(qa.joinToString(" - "))
        }

        @SmartCommandFunc
        suspend fun a(y: Int, a: Short, d: Double, b: Boolean, vararg qa: Int)
        {
            println(qa.joinToString(" - "))
        }
    }

}