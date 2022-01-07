package com.github.asforest.mshell

import com.github.asforest.mshell.authentication.Authentication
import com.github.asforest.mshell.command.*
import com.github.asforest.mshell.configuration.MShellConfig
import com.github.asforest.mshell.configuration.PresetsConfig
import com.github.asforest.mshell.exception.external.BaseExternalException
import com.github.asforest.mshell.model.EnvironmentalPreset
import com.github.asforest.mshell.permission.MShellPermissions
import com.github.asforest.mshell.permission.PresetGrants
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
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
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

@ConsoleExperimentalApi
object MShellPlugin : KotlinPlugin(MiraiUtil.pluginDescription)
{
    override fun onEnable()
    {
        // 加载配置文件
        MShellConfig.read(saveDefault = true)
        PresetsConfig.read()

        // 注册权限
        MShellPermissions.all
        MShellPermissions.use
        PresetGrants.registerAllPresetPermissions()

        // 注册指令
        MainCommand.register()
        PresetCommand.register()
        AuthCommand.register()
        GroupCommand.register()
        UserCommand.register()

        val botEvents = GlobalEventChannel.filter { it is BotEvent }

        // 订阅群聊消息
        botEvents.subscribeAlways<GroupMessageEvent> {
            withCatch {
                if(sender !is NormalMember || !sender.isFriend)
                    return@subscribeAlways

                if (sender.asFriend().asCommandSender().hasPermission(MShellPermissions.all))
                {
                    val session = SessionManager.getSessionByUserConnected(GroupUser(group))
                    if(session != null)
                        handleSessionInput(message, session)
                } else {
                    // 当做普通用户处理
                    val session = SessionManager.getSessionByUserConnected(GroupUser(group))
                    if(session != null)
                    {
                        // 授权用户
                        val presetName = session.preset.name
                        if (PresetGrants.testGrant(presetName, sender.id) || PresetGrants.testGrant(presetName, 0)) // 0: anyone
                            handleSessionInput(message, session)
                    }
                }
            }
        }

        // 订阅好友消息
        botEvents.subscribeAlways<FriendMessageEvent> {
            withCatch {
                val fuser = FriendUser(user)
                if (sender.asCommandSender().hasPermission(MShellPermissions.all))
                {
                    val session = SessionManager.getSessionByUserConnected(fuser)

                    handleMessage(message, session, fuser)
                } else if (PresetGrants.isGranted(fuser.user.id)) { // 处理授权用户
                    val session = SessionManager.getSessionByUserConnected(fuser)
                    val preset = Authentication.useDefaultPreset(null, fuser.user.id)
                    handleMessage(message, session, fuser, preset)
                }
            }
        }
    }

    fun handleMessage(
        message: MessageChain,
        session: Session?,
        user: SessionUser,
        preset: EnvironmentalPreset? = null
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
        try {
            func()
        } catch (e: BaseExternalException) {
            sender.sendMessage(e.message ?: e.stackTraceToString())
        } catch (e: Exception) {
            val detail = e.message ?: "没有错误详情可显示，异常类: ${e::class.qualifiedName}"
            sender.sendMessage("发生错误：$detail")
            throw e
        }
    }
}