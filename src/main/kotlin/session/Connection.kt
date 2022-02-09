package com.github.asforest.mshell.session

import com.github.asforest.mshell.session.user.AbstractSessionUser
import com.github.asforest.mshell.stream.BatchingWriter

class Connection(
    val user: AbstractSessionUser,
    val session: Session,
) {
    val sessionPid: Long get() = session.pid
    val batchingWriter = BatchingWriter(session.preset) { msg -> user.sendMessage(msg) }

    /**
     * 分包发送消息，发送间隔较短的消息会被合并成一条
     * @param message 要发送的消息
     */
    fun appendMessage(message: String)
    {
        batchingWriter += message
    }

    /**
     * 打断消息合并
     */
    fun appendTruncation()
    {
        batchingWriter.flush()
    }

    /**
     * 距离上一个在线或者离线状态切换过后所经过的时间
     */
    var whenOnlineChanged: Long = System.currentTimeMillis()

    /**
     * 当前连接是否在线
     */
    var isOnline: Boolean
        get() = _isOnline
        set(value) {
            if(value != isOnline)
                whenOnlineChanged = System.currentTimeMillis()
            _isOnline = value
        }

    private var _isOnline = true

    override fun equals(other: Any?): Boolean
    {
        if (other == null || other !is Connection)
            return false

        return hashCode() == other.hashCode()
    }

    override fun hashCode(): Int
    {
        val i = sessionPid.hashCode() xor user.hashCode()
        return (i shl 5) - i
    }
}