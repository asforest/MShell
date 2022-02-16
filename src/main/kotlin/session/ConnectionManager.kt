package com.github.asforest.mshell.session

import com.github.asforest.mshell.exception.system.SessionConnectionNotFoundException
import com.github.asforest.mshell.exception.system.SessionConnectionNotMatchException

class ConnectionManager (
    val session: Session
) {
    private val connections = mutableListOf<Connection>()

    /**
     * 建立/打开连接
     * @param user 发起连接的用户
     * @return <连接对象, 是否是重连的回话>
     */
    fun openConnection(user: SessionUser): Pair<Connection, Boolean>
    {
        var connection = getConnection(user, true)
        val isReconnection = connection != null

        if(connection == null)
        {
            connection = Connection(user, session, this)
            connections += connection
        } else {
            connection.isOnline = true
        }

        return Pair(connection, isReconnection)
    }

    /**
     * 关闭一个用户的连接
     */
    fun closeConnection(user: SessionUser)
    {
        val connection = getConnection(user, includeOffline = false)
            ?: throw SessionConnectionNotFoundException(user.toString())

        closeConnection(connection)
    }

    /**
     * 关闭指定的连接
     */
    fun closeConnection(connection: Connection)
    {
        if(connection.session != session)
            throw SessionConnectionNotMatchException(connection.session.pid, session.pid)

        connection.isOnline = false
    }

    /**
     * 根据用户获取指定连接
     * @param user 用户
     * @param includeOffline 是否包括离线连接
     */
    fun getConnection(user: SessionUser, includeOffline: Boolean): Connection?
    {
        for (conn in connections)
            if(conn.user == user && (includeOffline || conn.isOnline))
                return conn

        return null
    }

    /**
     * 获取当前会话的所有连接
     * @param includeOffline 结果是否包括离线的连接
     */
    fun getConnections(includeOffline: Boolean): List<Connection>
    {
        return connections.filter { includeOffline || it.isOnline }
    }

    /**
     * 关闭连接管理器本身
     */
    suspend fun closeAndWait()
    {
        // 刷新缓冲区
        for (connection in getConnections(includeOffline = true))
        {
            connection.batchingWriter.close()
            connection.batchingWriter.wait()
        }
    }
}