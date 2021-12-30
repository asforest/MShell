package com.github.asforest.mshell

import com.github.asforest.mshell.command.AdminsCommand
import com.github.asforest.mshell.command.EnvCommand
import com.github.asforest.mshell.command.GroupCommand
import com.github.asforest.mshell.command.MainCommand
import com.github.asforest.mshell.configuration.EnvPresets
import com.github.asforest.mshell.configuration.MShellConfig
import com.github.asforest.mshell.exception.external.BaseExternalException
import com.github.asforest.mshell.permission.MShellPermissions
import com.github.asforest.mshell.session.SessionManager
import com.github.asforest.mshell.session.user.FriendUser
import com.github.asforest.mshell.session.user.GroupUser
import com.github.asforest.mshell.session.SessionUser
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
import net.mamoe.mirai.message.data.content


object MShellPlugin : KotlinPlugin(MiraiUtil.pluginDescription)
{
    override fun onEnable()
    {
        MShellPermissions.all

        MShellConfig.read(saveDefault = true)
        EnvPresets.read()
        MainCommand.register()
        EnvCommand.register()
        AdminsCommand.register()
        GroupCommand.register()

        val botEvents = GlobalEventChannel.filter { it is BotEvent }

        // 订阅群聊消息
        botEvents.subscribeAlways<GroupMessageEvent> {
            if(sender !is NormalMember || !sender.isFriend)
                return@subscribeAlways

            if (!sender.asFriend().asCommandSender().hasPermission(MShellPermissions.all))
                return@subscribeAlways

            withCatch {
                handleMessage(message, GroupUser(group))
            }
        }

        // 订阅好友消息
        botEvents.subscribeAlways<FriendMessageEvent> {
            if (!sender.asCommandSender().hasPermission(MShellPermissions.all))
                return@subscribeAlways

            withCatch {
                handleMessage(message, FriendUser(user))
            }
        }
    }

    fun handleMessage(message: MessageChain, user: SessionUser)
    {
        val pokePresent = message.filterIsInstance<PokeMessage>().isNotEmpty()
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
            val message = message.content

            if(session != null)
            {
                val inputPrefix = MShellConfig.sessionInputPrefix

                if(inputPrefix.isNotEmpty())
                {
                    if(message.startsWith(inputPrefix) && message.length > inputPrefix.length)
                    {
                        val content = message.substring(inputPrefix.length)
                        session.stdin.println(content)
                    }
                } else {
                    session.stdin.println(message)
                }
            }
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