
package com.github.asforest.mshell.session

import com.github.asforest.mshell.MShell
import com.github.asforest.mshell.configuration.EnvPresets
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
        val ep = MShell.ep.ins
        val envPreset: EnvPresets.Preset

        // 加载环境预设
        val epName: String = if(preset == null) {
            val def = ep.defaultPreset
            if(def == "")
                throw NoDefaultPresetException("The default preset has not set yet or it was invalid.")
            def
        } else {
            if(preset !in ep.presets.keys)
                throw PresetNotFoundException("The preset '$preset' was not found")
            ep.defaultPreset
        }
        envPreset = MShell.ep.ins.presets[epName] ?: throw PresetNotFoundException("The preset '$epName' was not found")

        if(envPreset.shell == "")
            throw PresetIsIncompeleteException("The preset '$envPreset' is incomplete, the field 'shell' is not be set yet")

        val session = Session(this, user, envPreset.shell, envPreset.cwd, envPreset.env)

        // 自动执行exec
        if(envPreset.exec != "")
            session.stdin.println(envPreset.exec)

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

        // 分发事件
        session.onUserConnect { it(user) }

        user.sendMessage("Connected to pid(${session.pid})")
    }

    suspend fun disconnect(user: SessionUser)
    {
        if(!isUserConnected(user))
            throw UserNotConnectedYetException("You have not connected to a session yet")

        val session = getSessionByUserConnected(user)!!

        user.sendMessage("Disconnected from pid(${session.pid})")

        // 分发事件
        session.onUserDisconnect { it(user) }

        // 注销连接(Connection)
        connections.remove(user)
    }

    suspend fun disconnectAll(session: Session)
    {
        for(user in getUsersConnectedToSession(session))
        {
            // 分发事件
            session.onUserDisconnect { it(user) }

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
        for ((u, s) in connections)
            if(u == user)
                return s
        return null
    }

    fun isUserConnected(user: SessionUser): Boolean
    {
        return user in connections.keys
    }

    fun getUsersConnectedToSession(session: Session): List<SessionUser>
    {
        return connections.filter { it.value == session }.map { it.key }
    }
}