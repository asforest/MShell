package com.github.asforest.mshell.command

import com.github.asforest.mshell.MShellPlugin
import com.github.asforest.mshell.command.MShellCommand.Admin
import com.github.asforest.mshell.command.MShellCommand.CallingContext
import com.github.asforest.mshell.command.MShellCommand.User
import com.github.asforest.mshell.command.resolver.TreeCommand
import com.github.asforest.mshell.configuration.MShellConfig
import com.github.asforest.mshell.exception.business.*
import com.github.asforest.mshell.data.Preset
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

    @Command(desc = "插件帮助信息", aliases = ["h"], permission = User or Admin)
    suspend fun CallingContext.help()
    {
        sendMessage(buildString {
            for ((prefix, func) in allCommands)
            {
                if (func.permissionMask and permission == 0)
                    continue

                append("/")
                append(buildUsage(MShellCommand.rootLabal + if (prefix.isNotEmpty()) " $prefix" else "", func))
                append("\n")
            }
        }.trim())
    }

    @Command(desc = "开启一个会话并将当前用户连接到这个会话", aliases = ["o"], permission = Admin or User)
    suspend fun CallingContext.open(preset: String? = null, vararg argument: String)
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

    @Command(desc = "向目标会话的stdin里输出内容", aliases = ["w"], permission = Admin or User)
    suspend fun CallingContext.write(pid: Long, newline: Boolean, vararg text: String)
    {
        withCatch {
            val session = pidToSession(pid)

            checkPermission(session.preset) ?: throw NoPermissionToUsePresetExcetption(session.preset.name)

            session.stdin.print(text.joinToString(" ") + (if(newline) "\n" else ""))
        }
    }

    @Command(desc = "给当前连接中的会话发送kill信号", aliases = ["k"], permission = Admin or User)
    suspend fun CallingContext.kill()
    {
        withCatch {
            val user = toSessionUser()
            val session = SessionManager.getSession(user) ?: throw UserNotConnectedException()

            session.kill()
            sendMessage("kill 信号已发送给 ${session.identity}")
        }
    }

    @Command(desc = "给指定的会话发送kill信号", aliases = ["k"], permission = Admin or User)
    suspend fun CallingContext.kill(pid: Long)
    {
        withCatch {
            val session = pidToSession(pid)

            // 检查权限
            checkPermission(session.preset) ?: throw NoPermissionToKillSessionException(pid)

            session.kill()
            sendMessage("kill 信号已发送给 ${session.identity}")
        }
    }

    @Command(desc = "连接到一个会话", aliases = ["c"], permission = Admin or User)
    suspend fun CallingContext.connect(pid: Long)
    {
        withCatch {
            val session = pidToSession(pid)

            // 检查权限
            checkPermission(session.preset) ?: throw NoPermissionToConnectToASessionException(pid)

            SessionManager.connect(toSessionUser(), pid)
        }
    }

    @Command(desc = "断开当前会话", aliases = ["d"], permission = Admin or User)
    suspend fun CallingContext.disconnect()
    {
        withCatch {
            val user = toSessionUser()
            val session = SessionManager.getSession(user) ?: throw UserNotConnectedException()
            session.disconnect(user)
        }
    }

    @Command(desc = "强制断开一个会话的所有连接", aliases = ["d"], permission = Admin)
    suspend fun CallingContext.disconnect(pid: Long)
    {
        withCatch {
            pidToSession(pid).disconnectAll()
        }
    }

    @Command(desc = "显示所有会话", aliases = ["l"], permission = Admin or User)
    suspend fun CallingContext.list()
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
    suspend fun CallingContext.presets()
    {
        withCatch {
            val presetsAvailable = PresetGrants.getAvailablePresets(toSessionUser())

            sendMessage(presetsAvailable.withIndex().joinToString("\n") { (index, preset) ->
                "$index. ${preset.name}"
            }.ifEmpty { "没有可用的环境预设" })
        }
    }

    @Command(desc = "模拟戳一戳(窗口抖动)消息", aliases = ["s", "poke"], permission = Admin)
    suspend fun CallingContext.shake()
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

    @Command(desc = "重新加载config.yml配置文件", aliases = ["r"], permission = Admin)
    suspend fun CallingContext.reload()
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
    private inline fun CallingContext.checkPermission(preset: Preset): Preset?
    {
        return if (PresetGrants.isPresetAvailable(preset, toSessionUser())) preset else null
    }

    private suspend inline fun CallingContext.withCatch(block: CallingContext.() -> Unit)
    {
        MShellPlugin.catchException(sender.user) { block() }
    }
}