package com.github.asforest.mshell.session

import com.github.asforest.mshell.stream.BatchingWriter

/**
 * 其子类有：[GroupUser]、[FriendUser]、[ConsoleUser]
 */
abstract class SessionUser
{
    private val batchingWriter = BatchingWriter { msg -> onSendMessage(msg) }

    /**
     * 分包发送消息，发送间隔较短的消息会被合并成一条
     * @param message 要发送的消息
     */
    fun sendMessage(message: String)
    {
        if(message.isEmpty())
            return

        batchingWriter += message
    }

    /**
     * 强制打断消息合并
     */
    fun sendTruncation()
    {
        batchingWriter.flush()
    }

    /**
     * 发送消息的实现方法。此方法应该由子类重写
     */
    protected abstract suspend fun onSendMessage(message: String)

    abstract override fun toString(): String

    abstract override fun equals(other: Any?): Boolean

    abstract override fun hashCode(): Int
}