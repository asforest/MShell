package com.github.asforest.mshell.command.mshell

import com.github.asforest.mshell.MShellPlugin
import com.github.asforest.mshell.command.mshell.MShellCommand.Admin
import com.github.asforest.mshell.command.mshell.MShellCommand.User
import com.github.asforest.mshell.command.resolver.TreeCommand
import com.github.asforest.mshell.configuration.MShellConfig
import com.github.asforest.mshell.exception.business.*
import com.github.asforest.mshell.model.Preset
import com.github.asforest.mshell.permission.PresetGrants
import com.github.asforest.mshell.session.Session
import com.github.asforest.mshell.session.SessionManager
import com.github.asforest.mshell.util.MShellUtils.toSessionUser
import net.mamoe.mirai.console.command.CommandSender

object MainCommand : TreeCommand()
{
    @Tree(name = "auth", aliases = ["a"])
    val authCommand = AuthCommand

    @Tree(name = "preset", aliases = ["p"])
    val presetCommand = PresetCommand

    @Tree(name = "group", aliases = ["g"])
    val groupCommand = GroupCommand

    @Command(desc = "插件帮助信息", permission = User or Admin)
    suspend fun CommandSender.help()
    {
        sendMessage(buildString {
            for ((label, func) in allCommands)
            {
                append(listOf(
                    "/${MShellCommand.primaryName}",
                    label,
                    *func.parameters.map { p -> p.identity }.toTypedArray(),
                ).joinToString(" "))
                if(func.description.isNotEmpty())
                    append(": ${func.description}")
                append("\n")
            }
        })
    }

    @Command(desc = "开启一个会话并将当前用户连接到这个会话", permission = Admin or User)
    suspend fun CommandSender.open(preset: String? = null)
    {
        withCatch {
            val user = toSessionUser()

            // 不能有连接中的会话
            SessionManager.getSession(user)?.also { throw SessionUserAlreadyConnectedException(it.pid) }

            val _preset = PresetGrants.useDefaultPreset(preset, user)
            SessionManager.createSession(_preset.name, user)
        }
    }

    @Command(desc = "向目标会话的stdin里输出内容", permission = Admin or User)
    suspend fun CommandSender.write(pid: Long, newline: Boolean, vararg text: String)
    {
        withCatch {
            val session = pidToSession(pid)

            check(session.preset) ?: throw NoPermissionToUsePresetExcetption(session.preset.name)

            session.stdin.print(text.joinToString(" ") + (if(newline) "\n" else ""))
        }
    }

    @Command(desc = "强制结束一个会话", permission = Admin or User)
    suspend fun CommandSender.kill(pid: Long)
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
    suspend fun CommandSender.connect(pid: Long)
    {
        withCatch {
            val session = pidToSession(pid)

            // 检查权限
            check(session.preset) ?: throw NoPermissionToConnectToASessionException(pid)

            SessionManager.connect(toSessionUser(), pid)
        }
    }

    @Command(desc = "断开当前会话", permission = Admin or User)
    suspend fun CommandSender.disconnect()
    {
        withCatch {
            val user = toSessionUser()
            val session = SessionManager.getSession(user) ?: throw UserNotConnectedException()
            session.disconnect(user)
        }
    }

    @Command(desc = "强制断开一个会话的所有连接", permission = Admin)
    suspend fun CommandSender.disconnect(pid: Long)
    {
        withCatch {
            pidToSession(pid).disconnectAll()
        }
    }

    @Command(desc = "显示所有会话", permission = Admin or User)
    suspend fun CommandSender.list()
    {
        val presetsAvailable = PresetGrants.getAvailablePresets(toSessionUser())
        val sessionsVisible = SessionManager.sessions.filter { it.preset in presetsAvailable }

        sendMessage(sessionsVisible.withIndex().joinToString("\n") { (index, session) ->
            val pid = session.pid
            val usersConnected = session.users
            "[$index] ${session.preset.name} | $pid: $usersConnected"
        }.ifEmpty { "当前没有运行中的会话" })
    }

    @Command(desc = "列出所有可用的环境预设", permission = Admin or User)
    suspend fun CommandSender.presets()
    {
        val presetsAvailable = PresetGrants.getAvailablePresets(toSessionUser())

        sendMessage(presetsAvailable.withIndex().joinToString("\n") { (index, preset) ->
            "$index. ${preset.name}"
        }.ifEmpty { "没有可用的环境预设" })
    }

    @Command(desc = "模拟戳一戳(窗口抖动)消息", permission = Admin)
    suspend fun CommandSender.poke()
    {
        val user = toSessionUser()
        val session = SessionManager.getSession(user)

        if(session != null)
        {
            session.disconnect(user)
        } else {
            SessionManager.reconnectOrCreate(user)
        }
    }

    @Command(desc = "重新加载config.yml配置文件", permission = Admin)
    suspend fun CommandSender.reload()
    {
        MShellConfig.read()
        sendMessage("config.yml配置文件重载完成")
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
    private inline fun CommandSender.check(preset: Preset): Preset?
    {
        return if (PresetGrants.isPresetAvailable(preset, toSessionUser())) preset else null
    }

    private suspend inline fun CommandSender.withCatch(block: CommandSender.() -> Unit)
    {
        MShellPlugin.catchException(user) { block() }
    }
}