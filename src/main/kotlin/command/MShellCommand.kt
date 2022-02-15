package com.github.asforest.mshell.command

import com.github.asforest.mshell.MShellPlugin
import com.github.asforest.mshell.command.mshell.AuthCommand
import com.github.asforest.mshell.command.resolver.AbstractArgumentParsers
import com.github.asforest.mshell.command.resolver.SmartCommand
import com.github.asforest.mshell.configuration.MShellConfig
import com.github.asforest.mshell.exception.business.NoSuchSessionException
import com.github.asforest.mshell.exception.business.SessionUserAlreadyConnectedException
import com.github.asforest.mshell.permission.MShellPermissions
import com.github.asforest.mshell.session.Session
import com.github.asforest.mshell.session.SessionManager
import com.github.asforest.mshell.util.MShellUtils
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.RawCommand
import net.mamoe.mirai.console.permission.PermissionService.Companion.testPermission
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.message.data.MessageChain

@ConsoleExperimentalApi
object MShellCommand : RawCommand(
    MShellPlugin,
    primaryName = "mshell",
    description = "MShell插件指令",
    usage = "/mshell help",
    secondaryNames = arrayOf("ms"),
    parentPermission = MShellPermissions.all
) {
    const val MshellNobody = 0
    const val MshellUser = 1 shl 0
    const val MshellAdmin = 1 shl 1
    const val MshellAll = 1

    override suspend fun CommandSender.onCommand(args: MessageChain)
    {
        val commandText = args.joinToString(" ") { it.contentToString() }

        println(commandText)

        SubCommands.onCommand(commandText, this)
    }

    object SubCommands : SmartCommand()
    {
        @NestedCommand
        val auth = AuthCommand

        suspend fun onCommand(commandText: String, ctx: CommandSender)
        {
            try {
//                val senderPermission = if (ctx.user == null) {
//                    MshellAll
//                } else {
//                    if (MShellPermissions.all.testPermission(ctx.permitteeId))
//                        MshellAdmin
//                    else if (MShellPermissions.use.testPermission(ctx.permitteeId))
//                        MshellUser
//                    else
//                        MshellNobody
//                }

                val senderPermission = 0

                val afun = resolveCommandText(commandText, senderPermission)
                if(afun != null) afun.callSuspend(ctx) else ctx.help()
            } catch (e: AbstractArgumentParsers.ArgumentParserException) {
                ctx.sendMessage("参数#${e.argIndex + 3} '${e.raw}'不是${e.typeExcepted.simpleName}类型")
            } catch (e: PermissionDeniedException) {
                ctx.sendMessage("权限不够")
            }
        }

        @CommandFunc(desc = "插件帮助信息", permission = MshellUser or MshellAdmin)
        suspend fun CommandSender.help()
        {
            val o = StringBuffer().run {
                for ((label, func) in allCommandFunctions)
                {
                    append("/$primaryName $label ")

                    for (parameter in func.parameters)
                    {
                        if(parameter.isOptional)
                            append("[$parameter] ")
                        else if (parameter.isVararg)
                            append("[$parameter...] ")
                        else
                            append("<$parameter> ")
                    }

                    if(func.description.isNotEmpty())
                        append("  -   ${func.description}")

                    append("\n")
                }

                this.toString()
            }

            sendMessage(o)
        }

        @CommandFunc(desc = "开启一个会话并将当前用户连接到这个会话", permission = MshellAdmin)
        suspend fun CommandSender.open(preset: String? = null)
        {
            withCatch {
                val user = MShellUtils.getSessionUser(this)

                SessionManager.getSessionByUserConnected(user)?.also {
                    throw SessionUserAlreadyConnectedException(it.pid)
                }

                SessionManager.createSession(preset, user)
            }
        }


        @CommandFunc(desc = "向目标会话的stdin里输出内容", permission = MshellAdmin)
        suspend fun CommandSender.write(
            pid: Long,
            newline: Boolean,
            vararg text: String
        ) {
            withCatch {
                val session = SessionManager.getSessionByPid(pid)
                    ?: throw NoSuchSessionException(pid)
                session.stdin.print(text.joinToString(" ") + (if(newline) "\n" else ""))
            }
        }

        @CommandFunc(desc = "强制断开一个会话的所有连接", permission = MshellAdmin)
        suspend fun CommandSender.disconnect(
            pid: Long
        ) {
            withCatch {
                getSessionByPidWithThrow(pid).disconnectAll()
            }
        }

        @CommandFunc(desc = "强制结束一个会话", permission = MshellAdmin)
        suspend fun CommandSender.kill(
            pid: Long
        ) {
            withCatch {
                getSessionByPidWithThrow(pid).kill()
                sendMessage("进程已终止($pid)")
            }
        }

        @CommandFunc(desc = "连接到一个会话", permission = MshellAdmin)
        suspend fun CommandSender.connect(
            pid: Long
        ) {
            withCatch {
                SessionManager.connect(MShellUtils.getSessionUser(this), pid, )
            }
        }

        @CommandFunc(desc = "断开当前会话", permission = MshellAdmin)
        suspend fun CommandSender.disconnect()
        {
            withCatch {
                SessionManager.disconnect(MShellUtils.getSessionUser(this))
            }
        }

        @CommandFunc(desc = "显示所有会话", permission = MshellAdmin)
        suspend fun CommandSender.list()
        {
            var output = ""
            for ((index, session) in SessionManager.getAllSessions().withIndex())
            {
                val pid = session.pid
                val usersConnected = session.usersConnected

                output += "[$index] ${session.preset.name} | $pid: $usersConnected\n"
            }
            sendMessage(output.ifEmpty { "当前没有运行中的会话" })
        }

        @CommandFunc(desc = "模拟戳一戳(窗口抖动)消息", permission = MshellAdmin)
        suspend fun CommandSender.poke()
        {
            val user = MShellUtils.getSessionUser(this)
            val session = SessionManager.getSessionByUserConnected(user)

            if(session != null)
            {
                session.disconnect(user)
            } else {
                SessionManager.reconnectOrCreate(user)
            }
        }

        @CommandFunc(desc = "重新加载config.yml配置文件", permission = MshellAdmin)
        suspend fun CommandSender.reload()
        {
            MShellConfig.read()
            sendMessage("config.yml配置文件重载完成")
        }

        private fun getSessionByPidWithThrow(pid: Long): Session
        {
            return SessionManager.getSessionByPid(pid) ?: throw NoSuchSessionException(pid)
        }

        private suspend inline fun CommandSender.withCatch(block: CommandSender.() -> Unit)
        {
            MShellPlugin.catchException(user) { block() }
        }

    }

}