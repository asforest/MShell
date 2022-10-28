package com.github.asforest.mshell

import com.github.asforest.mshell.command.MShellCommand
import com.github.asforest.mshell.configuration.MShellConfig
import com.github.asforest.mshell.configuration.PresetsConfig
import com.github.asforest.mshell.exception.AbstractBusinessException
import com.github.asforest.mshell.model.Preset
import com.github.asforest.mshell.permission.MShellPermissions
import com.github.asforest.mshell.permission.PresetGrants
import com.github.asforest.mshell.session.Session
import com.github.asforest.mshell.session.SessionManager
import com.github.asforest.mshell.session.SessionUser
import com.github.asforest.mshell.util.EnvUtil
import com.github.asforest.mshell.util.MShellUtils
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandSender.Companion.asCommandSender
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.contact.NormalMember
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.contact.asFriend
import net.mamoe.mirai.contact.isFriend
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.BotEvent
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.PokeMessage
import net.mamoe.mirai.message.data.anyIsInstance
import net.mamoe.mirai.message.data.content

object MShellPlugin : KotlinPlugin(EnvUtil.pluginDescription)
{
    override fun onEnable()
    {
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

                val session = SessionManager.getSession(SessionUser.GroupUser(group)) ?: return@subscribeAlways

                if (sender.asFriend().asCommandSender().hasPermission(MShellPermissions.root))
                {
                    handleSessionInput(message, session)
                } else {
                    // 当做普通用户处理
                    val presetName = session.preset.name
                    if (PresetGrants.testGrant(presetName, sender.id) || PresetGrants.testGrant(presetName, 0)) // 0: anyone
                        handleSessionInput(message, session)
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
                    handleMessage(message, session, fuser)
                } else if (PresetGrants.isGranted(fuser.user.id)) { // 处理授权用户
                    val session = SessionManager.getSession(fuser)
                    val preset = PresetGrants.useDefaultPreset(null, fuser)
                    handleMessage(message, session, fuser, preset)
                }
            }
        }
    }

    fun handleMessage(
        message: MessageChain,
        session: Session?,
        user: SessionUser,
        preset: Preset? = null
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
                handleSessionInput(message, session)
        }
    }

    fun handleSessionInput(message: MessageChain, session: Session)
    {
        val messageText = message.content
        val inputPrefix = MShellConfig.sessionInputPrefix

        if(inputPrefix.isNotEmpty())
        {
            if(messageText.startsWith(inputPrefix) && messageText.length > inputPrefix.length)
                session.stdin.println(messageText.substring(inputPrefix.length))
        } else {
            session.stdin.println(messageText)
        }
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