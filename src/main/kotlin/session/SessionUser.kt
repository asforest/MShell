package com.github.asforest.mshell.session

import com.github.asforest.mshell.stream.BatchingWriter
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

    /**
     * 分包发送消息，发送间隔较短的消息会被合并成一条
     */
    fun sendMessageBatchly(message: String, truncation: Boolean =false)
    {
        sendRawMessageBatchly(message+"\n", truncation)
    }

    /**
     * 分包发送消息，但是末尾没有换行，发送间隔较短的消息会被合并成一条
     */
    fun sendRawMessageBatchly(message: String, truncation: Boolean =false)
    {
        batchingWriter += message
        if(truncation)
            sendMessageTruncation()
    }

    /**
     * 强制打断消息合并，拆分成2条消息发送
     */
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
        if (other == null || other !is SessionUser)
            return false

        return id == other.id
    }

    override fun hashCode(): Int
    {
        val i = id.hashCode()
        return (i shl 5) - i
    }
}