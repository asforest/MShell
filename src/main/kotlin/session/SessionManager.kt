package com.github.asforest.mshell.session

import com.github.asforest.mshell.MShell
import com.github.asforest.mshell.configuration.Preset
import com.github.asforest.mshell.exception.*

object SessionManager
{

    /**
     * 所有处于运行中的会话
     */
    val sessions = mutableListOf<Session>()

    /**
     * 会话连接管理器
     */
    val connectionManager = ConnectionManager()

    fun ResumeOrCreate(user: SessionUser)
    {
        if(isUserConnected(user))
            throw UserAlreadyConnectedException("You have already connected to a session")

        if(connectionManager.hasHistoricalConnection(user))
        {
            val conn = connectionManager.getHistoricalConnection(user)!!
            if(conn.isValid)
            {
                // session依然是有效的，复用session创建一个新的Connection
                connect(user, conn.session, isReconnection = true)
            } else {
                openSession(null, user)
            }
        } else {
            openSession(null, user)
        }
    }

    /**
     * 快速开启一个新的Session。如果不嫌麻烦，也可以手动new一个Session对象
     */
    fun openSession(preset: String? = null, user: SessionUser): Session
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

    fun connect(user: SessionUser, pid: Long)
    {
        val session = getSessionByPid(pid)
            ?: throw SessionNotFoundException("The session of pid($pid) was not be found")
        connect(user, session)
    }

    fun connect(user: SessionUser, session: Session, isReconnection: Boolean = false)
    {
        getSessionByUserConnected(user)?.also {
            if(it == session)
                throw UserAlreadyConnectedException("You have has already connected to this session")
            throw UserAlreadyConnectedException("You have already connected to a other session pid(${it.pid})")
        }

        connectionManager.createConnection(user, session)

        user.sendMessageBatchly((if(isReconnection) "Reconnected" else "Connected")+" to pid(${session.pid})", true)
    }

    fun disconnect(user: SessionUser)
    {
        if(!isUserConnected(user))
            throw UserNotConnectedYetException("You have not connected to a session yet")

        val pid = connectionManager.closeConnection(user).sessionPid

        user.sendMessageBatchly("Disconnected from pid($pid)", true)
    }

    fun disconnectAll(session: Session)
    {
        connectionManager.getConnections(session).forEach {
            it.session.sendMessageBatchly("Disconnected from pid(${session.pid})", true)
            connectionManager.closeConnection(it)
        }
    }

    fun getSessionByPid(pid: Long): Session?
    {
        for (session in sessions)
            // 如果子进程退出，就不应该再通过pid能获取到了
            if(session.isAlive && session.pid == pid)
                return session
        return null
    }

    fun getSessionByUserConnected(user: SessionUser): Session?
    {
        return connectionManager.getConnection(user)?.session
    }

    fun isUserConnected(user: SessionUser): Boolean
    {
        return connectionManager.hasConnection(user)
    }

    fun getUsersConnectedToSession(session: Session): Collection<SessionUser>
    {
        return getConnections(session).map { it.user }
    }

    fun getConnections(session: Session): Collection<Connection>
    {
        return connectionManager.getConnections(session)
    }
}