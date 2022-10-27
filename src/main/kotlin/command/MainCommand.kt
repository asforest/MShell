package com.github.asforest.mshell.command

import com.github.asforest.mshell.MShellPlugin
import com.github.asforest.mshell.command.MShellCommandAbstract.Admin
import com.github.asforest.mshell.command.MShellCommandAbstract.CallContext
import com.github.asforest.mshell.command.MShellCommandAbstract.User
import com.github.asforest.mshell.command.resolver.TreeCommand
import com.github.asforest.mshell.configuration.MShellConfig
import com.github.asforest.mshell.exception.business.*
import com.github.asforest.mshell.model.Preset
import com.github.asforest.mshell.permission.PresetGrants
import com.github.asforest.mshell.session.Session
import com.github.asforest.mshell.session.SessionManager
import com.github.asforest.mshell.util.MShellUtils.buildUsage

object MainCommand : TreeCommand()
{
    @Tree(name = "auth", aliases = ["a"])
    val authCommand = AuthCommand

    @Tree(name = "preset", aliases = ["p"])
    val presetCommand = PresetCommand

    @Tree(name = "group", aliases = ["g"])
    val groupCommand = GroupCommand

    @Command(desc = "插件帮助信息", permission = User or Admin)
    suspend fun CallContext.help()
    {
        sendMessage(buildString {
            for ((prefix, func) in allCommands)
            {
                if (func.permissionMask and permission == 0)
                    continue

                append("/")
                append(buildUsage(MShellCommandAbstract.rootLabal + if (prefix.isNotEmpty()) " $prefix" else "", func))
                append("\n")
            }
        }.trim())
    }

    @Command(desc = "开启一个会话并将当前用户连接到这个会话", permission = Admin or User)
    suspend fun CallContext.open(preset: String? = null, vararg argument: String)
    {
        withCatch {
            val user = toSessionUser()

            // 不能有连接中的会话
            SessionManager.getSession(user)?.also { throw SessionUserAlreadyConnectedException(it.pid) }

            val _preset = PresetGrants.useDefaultPreset(preset, user)
            val _argument = if (argument.isNotEmpty()) argument.joinToString(" ") else null
            SessionManager.createSession(_preset.name, _argument, user)
        }
    }

    @Command(desc = "向目标会话的stdin里输出内容", permission = Admin or User)
    suspend fun CallContext.write(pid: Long, newline: Boolean, vararg text: String)
    {
        withCatch {
            val session = pidToSession(pid)

            check(session.preset) ?: throw NoPermissionToUsePresetExcetption(session.preset.name)

            session.stdin.print(text.joinToString(" ") + (if(newline) "\n" else ""))
        }
    }

    @Command(desc = "强制结束一个会话", permission = Admin or User)
    suspend fun CallContext.kill(pid: Long)
    {
        withCatch {
            val session = pidToSession(pid)

            // 检查权限
            check(session.preset) ?: throw NoPermissionToKillSessionException(pid)

            pidToSession(pid).kill()
            sendMessage("进程已终止($pid)")
        }
    }

    @Command(desc = "连接到一个会话", permission = Admin or User)
    suspend fun CallContext.connect(pid: Long)
    {
        withCatch {
            val session = pidToSession(pid)

            // 检查权限
            check(session.preset) ?: throw NoPermissionToConnectToASessionException(pid)

            SessionManager.connect(toSessionUser(), pid)
        }
    }

    @Command(desc = "断开当前会话", permission = Admin or User)
    suspend fun CallContext.disconnect()
    {
        withCatch {
            val user = toSessionUser()
            val session = SessionManager.getSession(user) ?: throw UserNotConnectedException()
            session.disconnect(user)
        }
    }

    @Command(desc = "强制断开一个会话的所有连接", permission = Admin)
    suspend fun CallContext.disconnect(pid: Long)
    {
        withCatch {
            pidToSession(pid).disconnectAll()
        }
    }

    @Command(desc = "显示所有会话", permission = Admin or User)
    suspend fun CallContext.list()
    {
        withCatch {
            val presetsAvailable = PresetGrants.getAvailablePresets(toSessionUser())
            val sessionsVisible = SessionManager.sessions.filter { it.preset in presetsAvailable }

            sendMessage(sessionsVisible.withIndex().joinToString("\n") { (index, session) ->
                val pid = session.pid
                val usersConnected = session.users
                "[$index] ${session.preset.name} | $pid: $usersConnected"
            }.ifEmpty { "当前没有运行中的会话" })
        }
    }

    @Command(desc = "列出所有可用的环境预设", permission = Admin or User)
    suspend fun CallContext.presets()
    {
        withCatch {
            val presetsAvailable = PresetGrants.getAvailablePresets(toSessionUser())

            sendMessage(presetsAvailable.withIndex().joinToString("\n") { (index, preset) ->
                "$index. ${preset.name}"
            }.ifEmpty { "没有可用的环境预设" })
        }
    }

    @Command(desc = "模拟戳一戳(窗口抖动)消息", permission = Admin)
    suspend fun CallContext.poke()
    {
        withCatch {
            val user = toSessionUser()
            val session = SessionManager.getSession(user)

            if (session != null) {
                session.disconnect(user)
            } else {
                SessionManager.reconnectOrCreate(user)
            }
        }
    }

    @Command(desc = "重新加载config.yml配置文件", permission = Admin)
    suspend fun CallContext.reload()
    {
        withCatch {
            MShellConfig.read()
            sendMessage("config.yml配置文件重载完成")
        }
    }

    /**
     * 使用 pid 获取对应的回话
     * @throws NoSuchSessionException 如果 pid 无效
     */
    @Suppress("NOTHING_TO_INLINE")
    private inline fun pidToSession(pid: Long): Session
    {
        return SessionManager.getSession(pid) ?: throw NoSuchSessionException(pid)
    }

    /**
     * 检查指定用户有没有一个预设的使用权限
     */
    @Suppress("NOTHING_TO_INLINE")
    private inline fun CallContext.check(preset: Preset): Preset?
    {
        return if (PresetGrants.isPresetAvailable(preset, toSessionUser())) preset else null
    }

    private suspend inline fun CallContext.withCatch(block: CallContext.() -> Unit)
    {
        MShellPlugin.catchException(sender.user) { block() }
    }
}