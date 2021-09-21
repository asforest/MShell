package com.github.asforest.mshell.session

import com.github.asforest.mshell.stream.BatchingWriter
import com.github.asforest.mshell.util.FunctionUtil
import net.mamoe.mirai.console.command.ConsoleCommandSender
import net.mamoe.mirai.contact.User

data class SessionUser (
    private val user: User?
) {
    val name: String get() = if(user != null) "${user.nick}(${user.id})" else "<Console>"
    val origin: User? get() = user
    val isConsole: Boolean get() = user == null
    val id: String get() = if(user != null) "${user.id}" else "<Console>"

    private val batchingWriter = BatchingWriter { msg -> sendMessageImmediately(msg) }

    fun sendMessageBatchly(message: String, truncation: Boolean =false)
    {
        sendRawMessageBatchly(message+"\n", truncation)
    }

    fun sendRawMessageBatchly(message: String, truncation: Boolean =false)
    {
        batchingWriter += message
        if(truncation)
            sendMessageTruncation()
    }

    fun sendMessageTruncation()
    {
        batchingWriter.flush()
    }

    suspend fun sendMessageImmediately(message: String)
    {
        user.sendMessageImmediately(message)
    }

    suspend fun User?.sendMessageImmediately(msg: String)
    {
        if(msg.isNotEmpty())
            if(this != null) sendMessage(msg) else ConsoleCommandSender.sendMessage(msg)
    }

    override fun equals(other: Any?): Boolean
    {
        return FunctionUtil.SessionUserEquals(this, other)
    }

}