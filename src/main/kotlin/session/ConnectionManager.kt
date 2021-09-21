package com.github.asforest.mshell.session

import com.github.asforest.mshell.exception.SessionConnectionNotFoundException

class ConnectionManager
{
    val currentConnections = mutableSetOf<Connection>()
    val historicalConnections = mutableSetOf<Connection>()

    fun createConnection(user: SessionUser, session: Session): Connection
    {
        val connection = Connection(user, session)
        currentConnections += connection
        return connection
    }

    fun hasConnection(user: SessionUser): Boolean
    {
        return getConnection(user) != null
    }

    fun hasConnection(session: Session): Boolean
    {
        return getConnections(session).isNotEmpty()
    }

    fun hasHistoricalConnection(user: SessionUser): Boolean
    {
        return getHistoricalConnection(user) != null
    }

    fun hasHistoricalConnection(session: Session): Boolean
    {
        return getHistoricalConnections(session).isNotEmpty()
    }

    fun closeConnection(user: SessionUser): Connection
    {
        val connection = getConnection(user) ?:
            throw SessionConnectionNotFoundException("The session connection with user($user) was not be found")
        closeConnection(connection)
        return connection
    }

    fun closeConnections(session: Session)
    {
        getConnections(session).forEach { closeConnection(it) }
    }

    fun closeConnection(connection: Connection)
    {
        // 控制台不需要保留连接历史
        if(!connection.user.isConsole)
            historicalConnections += connection

        currentConnections -= connection
    }

    fun getConnection(user: SessionUser): Connection?
    {
        for (conn in currentConnections)
            if(conn.user == user)
                return conn
        return null
    }

    fun getConnections(session: Session): Collection<Connection>
    {
        return currentConnections.filter { it.session == session }
    }

    fun getHistoricalConnection(user: SessionUser): Connection?
    {
        for (conn in historicalConnections)
            if(conn.user == user)
                return conn
        return null
    }

    fun getHistoricalConnections(session: Session): Collection<Connection>
    {
        return historicalConnections.filter { it.session == session }
    }
}