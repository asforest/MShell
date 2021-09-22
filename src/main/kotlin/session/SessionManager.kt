package com.github.asforest.mshell.session

import com.github.asforest.mshell.MShell
import com.github.asforest.mshell.configuration.Preset
import com.github.asforest.mshell.exception.*
import java.io.File
import java.nio.charset.Charset

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

    /**
     * 快速开启一个新的Session。如果不嫌麻烦，也可以手动new一个Session对象
     * @param preset 环境预设
     * @param user 会话启动后，要立即连接上来的用户
     */
    fun createSession(preset: String? = null, user: SessionUser): Session
    {
        val epins = MShell.ep.ins
        val ep: Preset

        // 加载环境预设
        val epName: String = if(preset == null) {
            if(epins.defaultPreset == "")
                throw NoDefaultPresetException("The default preset has not been set yet or it is invalid.")
            epins.defaultPreset
        } else {
            if(preset !in epins.presets.keys)
                throw PresetNotFoundException("The preset '$preset' was not found")
            preset
        }
        ep = MShell.ep.ins.presets[epName] ?: throw PresetNotFoundException("The preset '$epName' is not found")

        // 参数检查
        if(ep.shell == "")
            throw PresetIsIncompeleteException("The preset '$ep' is incomplete, either the field 'shell' or the field 'charset' is not set yet")

        val _command = ep.shell
        val _workdir = File(if(ep.cwd!= "") ep.cwd else System.getProperty("user.dir"))
        val _env = ep.env
        val _charset = if(Charset.isSupported(ep.charset)) Charset.forName(ep.charset)
            else throw UnsupportedCharsetException("The charset '${ep.charset}' is unsupported")

        val session = Session(this, user, _command, _workdir, _env, _charset)

        // 自动执行exec
        if(ep.exec != "")
            session.stdin.println(ep.exec)

        return session
    }

    /**
     * 尝试重连会话，如果无法重连，会创建一个新的会话
     * @param user 要重连/新建会话的用户
     */
    fun reconnectOrCreate(user: SessionUser)
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
                createSession(null, user)
            }
        } else {
            createSession(null, user)
        }
    }

    /**
     * 使一个用户通过pid连接到一个会话
     * @param user 用户
     * @param pid 子进程的pid
     */
    fun connect(user: SessionUser, pid: Long)
    {
        val session = getSessionByPid(pid)
            ?: throw SessionNotFoundException("The session of pid($pid) was not be found")
        connect(user, session)
    }

    /**
     * 使一个用户连接到一个会话
     * @param user 用户
     * @param session 要连接到的会话
     * @param isReconnection 此次连接是普通连接还是重新连接
     */
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

    /**
     * 使一个用户从连接到的会话上断开（每个用户同时只能连接到一个会话，因此只需要一个user参数就能完成断开操作）
     * @param user 要断开的用户
     */
    fun disconnect(user: SessionUser)
    {
        if(!isUserConnected(user))
            throw UserNotConnectedYetException("You have not connected to a session yet")

        val pid = connectionManager.closeConnection(user).sessionPid

        user.sendMessageBatchly("Disconnected from pid($pid)", true)
    }

    /**
     * 使所有连接到指定会话上的用户全部断开连接
     */
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