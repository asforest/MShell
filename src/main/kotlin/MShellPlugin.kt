package com.github.asforest.mshell

import com.github.asforest.mshell.command.*
import com.github.asforest.mshell.configuration.PresetsConfig
import com.github.asforest.mshell.configuration.MShellConfig
import com.github.asforest.mshell.configuration.PermissionsConfig
import com.github.asforest.mshell.exception.external.BaseExternalException
import com.github.asforest.mshell.permission.MShellPermissions
import com.github.asforest.mshell.session.Session
import com.github.asforest.mshell.session.SessionManager
import com.github.asforest.mshell.session.SessionUser
import com.github.asforest.mshell.session.user.FriendUser
import com.github.asforest.mshell.session.user.GroupUser
import com.github.asforest.mshell.util.MiraiUtil
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandSender.Companion.asCommandSender
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.contact.NormalMember
import net.mamoe.mirai.contact.asFriend
import net.mamoe.mirai.contact.isFriend
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.BotEvent
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.PokeMessage
import net.mamoe.mirai.message.data.anyIsInstance
import net.mamoe.mirai.message.data.content


object MShellPlugin : KotlinPlugin(MiraiUtil.pluginDescription)
{
    override fun onEnable()
    {
        // 加载配置文件
        MShellConfig.read(saveDefault = true)
        PresetsConfig.read()
        PermissionsConfig.read(saveDefault = true)

        // 注册指令
        MainCommand.register()
        PresetCommand.register()
        AdminCommand.register()
        GroupCommand.register()
        UserCommand.register()

        val botEvents = GlobalEventChannel.filter { it is BotEvent }

        // 订阅群聊消息
        botEvents.subscribeAlways<GroupMessageEvent> {
            if(sender !is NormalMember || !sender.isFriend)
                return@subscribeAlways

            if (sender.asFriend().asCommandSender().hasPermission(MShellPermissions.all))
            {
                withCatch { handleMessage(message, GroupUser(group)) }
            } else {
                // 当做普通用户处理
                val session = SessionManager.getSessionByUserConnected(GroupUser(group))
                if(session != null)
                {
                    if (PermissionsConfig.testGrant(session.preset.name, sender.id))
                        handleSessionInput(message, session)
                }
            }
        }

        // 订阅好友消息
        botEvents.subscribeAlways<FriendMessageEvent> {
            if (!sender.asCommandSender().hasPermission(MShellPermissions.all))
                return@subscribeAlways

            withCatch { handleMessage(message, FriendUser(user)) }
        }
    }

    fun handleMessage(message: MessageChain, user: SessionUser)
    {
        val pokePresent = message.anyIsInstance<PokeMessage>()
        val session = SessionManager.getSessionByUserConnected(user)

        if(pokePresent)
        {
            if(session != null)
            {
                session.disconnect(user)
            } else {
                SessionManager.reconnectOrCreate(user)
            }
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

    suspend inline fun FriendMessageEvent.withCatch(block: FriendMessageEvent.() -> Unit)
    {
        try { block() } catch (e: BaseExternalException) { user.sendMessage(e.message ?: e.stackTraceToString()) }
    }

    suspend inline fun GroupMessageEvent.withCatch(block: GroupMessageEvent.() -> Unit)
    {
        try { block() } catch (e: BaseExternalException) { sender.sendMessage(e.message ?: e.stackTraceToString()) }
    }

}