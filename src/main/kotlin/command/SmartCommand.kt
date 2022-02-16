package com.github.asforest.mshell.command

import net.mamoe.mirai.console.command.*
import net.mamoe.mirai.console.command.descriptor.*
import net.mamoe.mirai.console.permission.Permission
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.buildMessageChain
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.typeOf

abstract class SmartCommand (
    /** 指令拥有者. */
    override val owner: CommandOwner,
    /** 主指令名. */
    override val primaryName: String,
    /** 次要指令名. */
    override vararg val secondaryNames: String,
    /** 执行指令需要的权限. */
    override val permission: Permission,
    /** 指令描述, 用于显示在 [BuiltInCommands.HelpCommand] */
    override val description: String = "<no descriptions given>",
    /** 为 `true` 时表示 [指令前缀][CommandManager.commandPrefix] 可选 */
    override val prefixOptional: Boolean = false,
) : Command {

    abstract val subCommandFunctions: Map<String, com.github.asforest.mshell.command.resolver.CommandSignature>

    override val usage: String by lazy {
        subCommandFunctions.entries.joinToString("\n") { (label, func) ->
            buildString {
                append(CommandManager.commandPrefix)
                append("$primaryName $label ")

                append(func.parameters.joinToString(" ") { parameter -> buildString {
                    if(parameter.isOptional)
                        append("[${parameter.name}] ")
                    else if (parameter.isVararg)
                        append("[${parameter.name}...] ")
                    else
                        append("<${parameter.name}> ")
                }})

                append("    # ${func.description}")
            }
        }
    }

    // 纯 vararg String 版本
//    @ExperimentalCommandDescriptors
//    override val overloads: List<@JvmWildcard CommandSignature> = listOf(
//        CommandSignatureImpl(
//            receiverParameter = CommandReceiverParameter(false, typeOf<CommandSender>()),
//            valueParameters = listOf(
//                AbstractCommandValueParameter.UserDefinedType.createRequired<Array<out Message>>(
//                    "args",
//                    true
//                )
//            )
//        ) { call ->
//            val sender = call.caller
//            val arguments = call.rawValueArguments
//            sender.onCommand(buildMessageChain { arguments.forEach { +it.value } })
//        }
//    )

    @ConsoleExperimentalApi
    @ExperimentalCommandDescriptors
    override val overloads: List<CommandSignature> by lazy {
        subCommandFunctions.map { (label, func) ->

            val valueParameters = label.split(' ').mapIndexed { idx, s -> AbstractCommandValueParameter.StringConstant("#$idx", s, false) } +
                    func.parameters.map { param ->
                        AbstractCommandValueParameter.UserDefinedType<Any?>(
                            param.name, param.isOptional, param.isVararg, if(!param.isVararg) String::class.starProjectedType else Array<String>::class.starProjectedType
                        )
                    }

            CommandSignatureImpl(
                receiverParameter = CommandReceiverParameter(false, typeOf<CommandSender>()),
                valueParameters = valueParameters
            ) { call ->
                val sender = call.caller
                val arguments = call.rawValueArguments
                sender.onCommand(buildMessageChain { arguments.forEach { +it.value } })
            }
        }
    }

    /**
     * 在指令被执行时调用.
     *
     * @param args 指令参数.
     *
     * @see CommandManager.executeCommand 查看更多信息
     */
    abstract suspend fun CommandSender.onCommand(args: MessageChain)
}


