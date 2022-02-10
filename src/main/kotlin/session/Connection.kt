package com.github.asforest.mshell.session

import com.github.asforest.mshell.session.user.AbstractSessionUser
import com.github.asforest.mshell.stream.BatchingWriter

/**
 * Connection 代表一个用户与会话之间连接对象
 *
 * 当用户从会话上断开时，连接对象不会销毁，而是被标记为offline状态，
 * 以便当用户重新连接上来时，向用户发送遗愿消息
 */
class Connection(
    val user: AbstractSessionUser,
    val session: Session,
    val connectionManager: ConnectionManager
) {
    val sessionPid: Long get() = session.pid
    val batchingWriter = BatchingWriter(session.preset) { msg -> user.sendMessage(msg) }

    /**
     * 分包发送消息，发送间隔较短的消息会被合并成一条
     * @param message 要发送的消息
     */
    fun sendMessage(message: String)
    {
        batchingWriter += message
    }

    /**
     * 打断消息合并
     */
    fun sendTruncation()
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

    /**
     * 关闭连接（设为离线状态）
     */
    fun close()
    {
        connectionManager.closeConnection(this)
    }

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