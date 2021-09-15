package com.github.asforest.mshell.session

import net.mamoe.mirai.console.command.ConsoleCommandSender
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.message.data.Message
import com.github.asforest.mshell.util.FunctionUtil

data class SessionUser (
    private val user: User?
) {
    val name: String get() = if(user != null) "${user.nick}(${user.id})" else "<Console>"

    val origin: User? get() = user

    val isConsole: Boolean get() = user == null

    val id: String get() = if(user != null) "${user.id}" else "<Console>"

    suspend fun sendMessage(message: String)
    {
        user.sendMessage(message)
    }

    suspend fun sendMessage(message: Message)
    {
        user.sendMessage(message)
    }

    suspend fun User?.sendMessage(msg: String) {
        if(this != null) sendMessage(msg) else ConsoleCommandSender.sendMessage(msg)
    }

    suspend fun User?.sendMessage(msg: Message) {
        if(this != null) sendMessage(msg) else ConsoleCommandSender.sendMessage(msg)
    }

    override fun equals(other: Any?): Boolean
    {
        return FunctionUtil.SessionUserEquals(this, other)
    }

}