package com.github.asforest.mshell.session

import com.github.asforest.mshell.exception.system.SessionConnectionNotFoundException
import com.github.asforest.mshell.exception.system.SessionConnectionNotMatchException
import com.github.asforest.mshell.session.user.AbstractSessionUser

class ConnectionManager (
    val session: Session
) {
    val connections = mutableListOf<Connection>()

    /**
     * 建立/打开连接
     * @param user 发起连接的用户
     * @return <连接对象, 是否是重连的回话>
     */
    fun openConnection(user: AbstractSessionUser): Pair<Connection, Boolean>
    {
        var connection = getConnection(user, true)
        val isReconnection = connection != null

        if(connection == null)
        {
            connection = Connection(user, session)
            connections += connection
        } else {
            connection.isOnline = true
        }

        return Pair(connection, isReconnection)
    }

    /**
     * 关闭一个用户的连接
     */
    fun closeConnection(user: AbstractSessionUser)
    {
        val connection = getConnection(user, includeOffline = false)
            ?: throw SessionConnectionNotFoundException(user.toString())
        closeConnection(connection)
    }

    /**
     * 关闭一个会话的所有的连接
     */
    fun closeAllConnections()
    {
        getConnections(includeOffline = false).forEach { closeConnection(it) }
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
     * 用户是否连接到了当前会话上
     */
    fun isUserConnected(user: AbstractSessionUser): Boolean
    {
        return getConnection(user, includeOffline = false) != null
    }

    /**
     * 根据用户获取指定连接
     */
    fun getConnection(user: AbstractSessionUser, includeOffline: Boolean): Connection?
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
}