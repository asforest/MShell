package com.github.asforest.mshell

import com.github.asforest.mshell.command.AdminsCommand
import com.github.asforest.mshell.command.EnvCommand
import com.github.asforest.mshell.command.MainCommand
import com.github.asforest.mshell.configuration.ConfigProxy
import com.github.asforest.mshell.configuration.EnvPresets
import com.github.asforest.mshell.configuration.MainConfig
import com.github.asforest.mshell.exception.BaseException
import com.github.asforest.mshell.permission.MShellPermissions
import com.github.asforest.mshell.session.SessionManager
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandSender.Companion.toCommandSender
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.BotEvent
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.message.data.PokeMessage
import net.mamoe.mirai.message.data.content

object MShell : KotlinPlugin(JvmPluginDescription.loadFromResource())
{
    val ep = ConfigProxy(EnvPresets::class.java, "presets.yml")

    override fun onEnable()
    {
        MShellPermissions.all

        ep.read()
        MainConfig.reload()
        MainCommand.register()
        EnvCommand.register()
        AdminsCommand.register()

        // 订阅好友消息
        GlobalEventChannel.filter { it is BotEvent }.subscribeAlways<FriendMessageEvent> {
            if (!toCommandSender().hasPermission(MShellPermissions.all) )
                return@subscribeAlways

            withCatch {
                val pokePresent = message.filterIsInstance<PokeMessage>().isNotEmpty()
                val session = SessionManager.getSessionByUserConnected(user)

                if(pokePresent)
                {
                    if(session != null)
                    {
                        session.disconnect(user)
                    } else {
                        SessionManager.ResumeOrCreate(user)
                    }
                } else {
                    session?.stdin?.println(message.content)
                }
            }

        }

//        GlobalEventChannel.filter { it is BotEvent }.subscribeAlways<GroupMessageEvent> {
//            val user = this.sender
//            if(user.id !in Admins.admins)
//                return@subscribeAlways
//
//            val facePresent = message.filterIsInstance<Face>().isNotEmpty()
//            val session = SessionManager.getSessionByUserConnected(user)
//
//            if(facePresent)
//            {
//
//            }
//        }
    }

    suspend inline fun FriendMessageEvent.withCatch(block: FriendMessageEvent.() -> Unit)
    {
        try { block() } catch (e: BaseException) { user.sendMessage(e.message ?: e.stackTraceToString()) }
    }

}