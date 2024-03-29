package com.github.asforest.mshell

import com.github.asforest.mshell.command.MShellCommand
import com.github.asforest.mshell.configuration.MShellConfig
import com.github.asforest.mshell.configuration.PresetsConfig
import com.github.asforest.mshell.data.JsonMessage
import com.github.asforest.mshell.exception.AbstractBusinessException
import com.github.asforest.mshell.data.Preset
import com.github.asforest.mshell.permission.MShellPermissions
import com.github.asforest.mshell.permission.PresetGrants
import com.github.asforest.mshell.session.Session
import com.github.asforest.mshell.session.SessionManager
import com.github.asforest.mshell.session.SessionUser
import com.github.asforest.mshell.util.EnvUtil
import com.github.asforest.mshell.util.MShellUtils
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandSender.Companion.asCommandSender
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.console.util.safeCast
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.data.UserProfile
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.BotEvent
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.*

object MShellPlugin : KotlinPlugin(EnvUtil.pluginDescription)
{
    var stoped = true

    override fun onEnable()
    {
        stoped = false

        // 加载配置文件
        MShellConfig.read(saveDefault = true)
        PresetsConfig.read()

        // 注册权限
        MShellPermissions.root
        MShellPermissions.use
        PresetGrants.registerAllPresetPermissions()

        // 注册指令
        MShellCommand.register()

        val botEvents = GlobalEventChannel.filter { it is BotEvent }

        // 订阅群聊消息
        botEvents.subscribeAlways<GroupMessageEvent> {
            withCatch {
                if(sender !is NormalMember)
                    return@subscribeAlways

                val groupUser = SessionUser.GroupUser(group)
                val session = SessionManager.getSession(groupUser) ?: return@subscribeAlways

                if (sender.asFriend().asCommandSender().hasPermission(MShellPermissions.root))
                {
                    handleSessionInput(message, session, groupUser, sender)
                } else {
                    // 当做普通用户处理
                    val presetName = session.preset.name
                    if (PresetGrants.testGrant(presetName, sender.id) || PresetGrants.testGrant(presetName, 0)) // 0: anyone
                        handleSessionInput(message, session, groupUser, sender)
                }
            }
        }

        // 订阅好友消息
        botEvents.subscribeAlways<FriendMessageEvent> {
            withCatch {
                val fuser = SessionUser.FriendUser(user)
                if (sender.asCommandSender().hasPermission(MShellPermissions.root))
                {
                    val session = SessionManager.getSession(fuser)
                    handleMessage(message, session, null, fuser, sender)
                } else if (PresetGrants.isGranted(fuser.user.id)) { // 处理授权用户
                    val session = SessionManager.getSession(fuser)
                    val preset = PresetGrants.useDefaultPreset(null, fuser)
                    handleMessage(message, session, preset, fuser, sender)
                }
            }
        }
    }

    override fun onDisable()
    {
        // mirai / mirai-console 2.12有bug会导致onDisable()调用两遍
        if (stoped)
            return

        stoped = true

        for (session in SessionManager.sessions)
        {
            if (session.isLive)
            {
                logger.info("Kill the running session ${session.identity}")
                session.kill()
            }
        }
    }

    suspend fun handleMessage(
        message: MessageChain,
        session: Session?,
        preset: Preset?,
        user: SessionUser,
        sender: User
    ) {
        val pokePresent = message.anyIsInstance<PokeMessage>()

        if(pokePresent)
        {
            if(session != null)
                session.disconnect(user)
             else
                SessionManager.reconnectOrCreate(user, preset?.name)
        } else {
            if(session != null)
                handleSessionInput(message, session, user, sender)
        }
    }

    suspend fun handleSessionInput(
        message: MessageChain,
        session: Session,
        user: SessionUser,
        sender: User
    ) {
        val messageText = message.content
        val inputPrefix = MShellConfig.sessionInputPrefix
        var text = if(inputPrefix.isNotEmpty()) {
            if(messageText.startsWith(inputPrefix) && messageText.length > inputPrefix.length)
                messageText.substring(inputPrefix.length)
            else
                return
        } else {
            messageText
        }

        if (text.isEmpty())
            return

        // 处理转义字符
        text = text.replace(Regex("(?<!\\\\)\\\\n"), "\n")
        text = text.replace(Regex("(?<!\\\\)\\\\r"), "\r")
        text = text.replace(Regex("(?<!\\\\)\\\\t"), "\t")
        text = text.replace(Regex("(?<!\\\\)\\\\b"), "\b")
        text = text.replace(Regex("(?<!\\\\)\\\\e"), "\u001b")
        text = text.replace(Regex("\\\\\\\\"), "\\\\")

        // 处理json模式
        if (session.preset.jsonMode)
        {
            val profile = sender.queryProfile()
            val normalMember = sender.safeCast<NormalMember>()

            text = JsonMessage(
                bot = sender.bot.id,
                group = if (user is SessionUser.GroupUser) user.group.id else -1,
                relation = when (user) {
                    is SessionUser.ConsoleUser -> "console" // 永远不可能执行到此分支
                    is SessionUser.FriendUser -> "friend"
                    is SessionUser.GroupUser -> when (normalMember!!.permission) {
                        MemberPermission.MEMBER -> "member"
                        MemberPermission.ADMINISTRATOR -> "admin"
                        MemberPermission.OWNER -> "owner"
                    }
                },
                message = text,
                nick = sender.nick,
                id = sender.id,
                remark = sender.remark,
                join = normalMember?.joinTimestamp ?: -1,
                speak = normalMember?.lastSpeakTimestamp ?: -1,
                namecard = normalMember?.nameCard ?: "",
                title = normalMember?.specialTitle ?: "",
                at = message.filterIsInstance<At>().firstOrNull()?.target ?: -1,
                email = profile.email,
                age = profile.age,
                level = profile.qLevel,
                sex = profile.sex,
            ).toString()
        }

        session.stdin.println(text)
    }

    private suspend inline fun MessageEvent.withCatch(func: () -> Unit)
    {
        catchException(sender) { func() }
    }

    /**
     * 尝试捕捉异常
     * @param user 遇到异常时，将异常信息发送给的用户（如果为null则代表是控制台）
     * @param func
     */
    suspend inline fun catchException(user: User?, func: () -> Unit)
    {
        try {
            func()
        } catch (e: AbstractBusinessException) {
            MShellUtils.sendMessage2(user, e.message ?: e.stackTraceToString())
        } catch (e: Exception) {
            val detail = e.message ?: "没有错误详情可显示，异常类: ${e::class.qualifiedName}"
            MShellUtils.sendMessage2(user, "发生错误：$detail\n异常调用堆栈：\n${e.stackTraceToString()}")
            throw e
        }
    }


}