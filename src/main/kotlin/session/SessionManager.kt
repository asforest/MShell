
package com.github.asforest.mshell.session

import com.github.asforest.mshell.MShell
import com.github.asforest.mshell.configuration.Preset
import com.github.asforest.mshell.exception.*

object SessionManager
{
    val scd = SessionContinuationDispatcher()
    val sessions = mutableListOf<Session>()
    val connections = mutableMapOf<SessionUser, Session>()
    val historicalConnections = mutableMapOf<SessionUser, Long>()

    suspend fun ResumeOrCreate(user: SessionUser)
    {
        if(isUserConnected(user))
            throw UserAlreadyConnectedException("You have already connected to a session")

        if(user in historicalConnections.keys)
        {
            try {
                val pid = historicalConnections[user]!!
                connect(user, pid)
                user.sendMessage("Reconnected to pid($pid)")
            } catch (e: SessionNotFoundException) {
                historicalConnections[user] = openSession(null, user).pid
            }
        } else {
            historicalConnections[user] = openSession(null, user).pid
        }
    }

    suspend fun openSession(preset: String? = null, user: SessionUser): Session
    {
        val epins = MShell.ep.ins
        val ep: Preset

        // 加载环境预设
        val epName: String = if(preset == null) {
            if(epins.defaultPreset == "")
                throw NoDefaultPresetException("The default preset has not be set yet or it was invalid.")
            epins.defaultPreset
        } else {
            if(preset !in epins.presets.keys)
                throw PresetNotFoundException("The preset '$preset' was not found")
            preset
        }
        ep = MShell.ep.ins.presets[epName] ?: throw PresetNotFoundException("The preset '$epName' was not found")

        if(ep.shell == "")
            throw PresetIsIncompeleteException("The preset '$ep' is incomplete, either the field 'shell' or the field 'charset' is not set yet")

        val session = Session(this, user, ep.shell, ep.cwd, ep.env, ep.charset)

        // 自动执行exec
        if(ep.exec != "")
            session.stdin.println(ep.exec)

        return session
    }

    suspend fun connect(user: SessionUser, pid: Long)
    {
        val session = getSessionByPid(pid)
            ?: throw SessionNotFoundException("The session of pid($pid) was not be found")
        connect(user, session)
    }

    suspend fun connect(user: SessionUser, session: Session)
    {
        getSessionByUserConnected(user)?.also {
            if(it == session)
                throw UserAlreadyConnectedException("You have has already connected to this session")
            throw UserAlreadyConnectedException("You have already connected to a other session (${it.pid})")
        }

        // 记录链接历史
        if(!user.isConsole)
            historicalConnections[user] = session.pid

        // 注册连接(Connection)
        connections[user] = session

        user.sendMessage("Connected to pid(${session.pid})")
    }

    suspend fun disconnect(user: SessionUser)
    {
        if(!isUserConnected(user))
            throw UserNotConnectedYetException("You have not connected to a session yet")

        val session = getSessionByUserConnected(user)!!

        user.sendMessage("Disconnected from pid(${session.pid})")

        // 注销连接(Connection)
        connections.remove(user)
    }

    suspend fun disconnectAll(session: Session)
    {
        for(user in getUsersConnectedToSession(session))
        {
            // 注销连接(Connection)
            connections.remove(user)

            user.sendMessage("Disconnected from pid(${session.pid})")
        }
    }

    fun getSessionByPid(pid: Long): Session?
    {
        for (session in sessions)
            if(session.pid == pid)
                return session
        return null
    }

    fun getSessionByUserConnected(user: SessionUser): Session?
    {
        for ((u, session) in connections)
            if(u == user)
                return session
        return null
    }

    fun isUserConnected(user: SessionUser): Boolean
    {
        // do not use like this (the behivor is not the same with getSessionByUserConnected)
        // return user in connections.keys
        return getSessionByUserConnected(user) != null
    }

    fun getUsersConnectedToSession(session: Session): List<SessionUser>
    {
        return connections.filter { it.value == session }.map { it.key }
    }
}