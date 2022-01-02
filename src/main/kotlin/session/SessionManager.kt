package com.github.asforest.mshell.session

import com.github.asforest.mshell.configuration.PresetsConfig
import com.github.asforest.mshell.configuration.MShellConfig
import com.github.asforest.mshell.exception.*
import com.github.asforest.mshell.exception.external.*
import com.github.asforest.mshell.model.EnvironmentalPreset
import com.github.asforest.mshell.permission.PresetGrants
import com.github.asforest.mshell.session.user.FriendUser
import com.github.asforest.mshell.session.user.GroupUser
import java.text.SimpleDateFormat

object SessionManager
{
    /**
     * 所有注册过的会话
     */
    private val sessions = mutableMapOf<Session, ConnectionManager>()

    /**
     * 快速开启一个新的Session并完成注册。如果不嫌麻烦，也可以手动new一个Session对象，然后自己注册
     * @param preset 环境预设
     * @param user 会话启动后，要立即连接上来的用户
     * @return 创建的Session，也可能是复用的Session
     */
    fun createSession(preset: String?, user: SessionUser?): Session
    {
        // user不能连接到任何会话，否则会抛异常
        user?.also { getSessionByUserConnected(user)?.also { session ->
            if(user is GroupUser)
                throw SessionUserAlreadyConnectedException(user.group.id, session.pid)
            else
                throw SessionUserAlreadyConnectedException(session.pid)
        } }

        val presetObj = useDefaultPreset(preset)
        var session: Session
        var created: Boolean

        // 单实例
        val hasSecondIns = sessions.keys.any { it.preset == presetObj }
        if(!presetObj.singleInstance || !hasSecondIns)
        {
            // 创建会话
            session = Session(this, presetObj, MShellConfig.lastwillCapacity)
            registerSession(session)

            session.start()

            // 自动执行exec
            if(presetObj.exec != "")
                session.stdin.println(presetObj.exec)

            created = true
        } else {
            // 复用会话
            session = sessions.keys.first { it.preset == presetObj }

            created = false
        }

        // 用户自动连接
        if(user != null)
        {
            if(created)
                user.sendMessage("会话已创建(pid: ${session.pid})，环境预设(${session.preset.name})\n")
            session.connect(user)
            user.sendTruncation()
        }

        return session
    }

    /**
     * 注册一个Session
     */
    private fun registerSession(session: Session)
    {
        if(isSessionRegistered(session))
            throw SessionAlreadyRegisteredException(session)

        sessions[session] = ConnectionManager(session)

        // listen for the event of subprocess ends
        session.onProcessEnd.once {
            unregisterSession(this)
        }
    }

    /**
     * 取消注册一个Session
     */
    private fun unregisterSession(session: Session)
    {
        if(!isSessionRegistered(session))
            throw SessionNotRegisteredException(session)

        // 发送退出消息
        session.broadcaseMessageTruncation()
        session.broadcastMessageBatchly("会话已结束(pid: ${session.pid})，环境预设(${session.preset.name})\n")

        // 关掉所有连接
        session.manager.disconnectAll(session)

        // 取消注册
        sessions.remove(session)
    }

    /**
     * 判断一个会话是否注册过了
     */
    private fun isSessionRegistered(session: Session): Boolean
    {
        return sessions.containsKey(session)
    }

    /**
     * 尝试重连会话，如果无法重连，会创建一个新的会话
     * @param user 要重连/新建会话的用户
     * @param preset 可选的预设
     */
    fun reconnectOrCreate(user: SessionUser, preset: String? = null)
    {
        getSessionByUserConnected(user)?.also {
            throw SessionUserAlreadyConnectedException(it.pid)
        }

        val cm = getConnectionManager(user, true)
        val connection = cm?.getConnection(user, includeOffline = true)
        if(connection != null)
        {
            connect(user, connection.session)
        } else {
            createSession(preset, user)
        }
    }

    /**
     * 使一个用户通过pid连接到一个会话
     * @param user 用户
     * @param pid 子进程的pid
     */
    fun connect(user: SessionUser, pid: Long): Session
    {
        val session = getSessionByPid(pid) ?: throw NoSuchSessionException(pid)
        connect(user, session)
        return session
    }

    /**
     * 使一个用户连接到一个会话
     * @param user 用户
     * @param session 要连接到的会话
     */
    fun connect(user: SessionUser, session: Session)
    {
        // 安全检查
        getSessionByUserConnected(user)?.also {
            throw SessionUserAlreadyConnectedException(it.pid)
        }

        val connectionManager = getConnectionManager(session) ?: throw SessionNotRegisteredException(session)
        val whenOnlineChanged = connectionManager.getConnection(user, true)?.whenOnlineChanged ?: -1
        val (_, isReconnection) = connectionManager.openConnection(user)

        // 发送遗愿消息
        if(whenOnlineChanged != -1L && session.lwm.hasMessage(whenOnlineChanged))
        {
            var last: Long = 0
            val sb = StringBuffer()
            for (msg in session.lwm.getAllLines(whenOnlineChanged))
            {
                sb.append(msg.message)
                last = msg.time
            }
            sb.append("\n==========最后输出(${SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(last)})==========\n")

            user.sendTruncation()
            user.sendMessage(sb.toString())
            user.sendTruncation()
        }

        user.sendMessage((if(isReconnection) "已重连" else "已连接")+"到会话(pid: ${session.pid})，环境预设(${session.preset.name})\n")
        user.sendTruncation()
    }

    /**
     * 使一个用户从所连接的会话上断开
     * @param user 要断开的用户
     * @return 连接到的Session
     */
    fun disconnect(user: SessionUser): Session
    {
        if(!hasUserConnectedToAnySession(user))
        {
            if(user is GroupUser)
                throw UserDidNotConnectedYetException(user)
            else
                throw UserDidNotConnectedYetException()
        }

        // 关闭连接
        val cm = getConnectionManager(user, includeOffine = false)!!
        val session = getSessionByUserConnected(user)!!

        cm.closeConnection(user)

        // 发送消息
        user.sendTruncation()
        user.sendMessage("已从会话断开(pid: ${cm.session.pid})，环境预设(${session.preset.name})\n")
        user.sendTruncation()

        return cm.session
    }

    /**
     * 尝试将一个用户从所连接的会话上断开，如果用户没有连接到任何会话，则不做任何事情
     * @param user 要断开的用户
     */
    fun tryToDisconnect(user: SessionUser)
    {
        if(hasUserConnectedToAnySession(user))
            disconnect(user)
    }

    /**
     * 使所有连接到指定会话上的用户全部断开连接
     */
    fun disconnectAll(session: Session)
    {
        val cm = getConnectionManager(session) ?: throw SessionNotRegisteredException(session)

        session.broadcastMessageBatchly("已从会话断开(pid: ${session.pid})，环境预设(${session.preset.name})")
        session.broadcaseMessageTruncation()
        cm.closeAllConnections()
    }

    /**
     * 根据PID获取Session
     */
    fun getSessionByPid(pid: Long): Session?
    {
        for (session in sessions.keys)
            if(session.pid == pid)
                return session

        return null
    }

    /**
     * 根据User获取相应已经连接的Session
     */
    fun getSessionByUserConnected(user: SessionUser): Session?
    {
        for (connectionManager in sessions.values)
            if(connectionManager.isUserConnected(user))
                return connectionManager.session

        return null
    }

    /**
     * 判断一个User是否连接到了任意一个Session上
     */
    fun hasUserConnectedToAnySession(user: SessionUser): Boolean
    {
        for (connectionManager in sessions.values)
            if(connectionManager.isUserConnected(user))
                return true

        return false
    }

    /**
     * 获取连接到某个Session上的所有User
     */
    fun getUsersConnectedTo(session: Session): Collection<SessionUser>
    {
        return getConnectionManager(session)?.getConnections(false)?.map { it.user } ?: return listOf()
    }

    /**
     * 根据连接到的用户来获取对应的ConnectionManager
     */
    fun getConnectionManager(user: SessionUser, includeOffine: Boolean): ConnectionManager?
    {
        val session = null
        for (cm in sessions.values)
            if(cm.getConnection(user, includeOffine) != null)
                return cm

        return null
    }

    fun getConnectionManager(session: Session): ConnectionManager?
    {
        return sessions[session]
    }

    fun getAllSessions(): MutableSet<Session>
    {
        return sessions.keys
    }

    /**
     * 使用默认环境预设
     */
    fun useDefaultPreset(preset: String?): EnvironmentalPreset
    {
        // 加载环境预设
        val name: String = if(preset == null) {
            if(PresetsConfig.defaultPreset == "")
                throw NoDefaultPresetException("默认环境预设未指定或者指向一个不存在的预设")
            PresetsConfig.defaultPreset
        } else {
            if(preset !in PresetsConfig.presets.keys)
                throw PresetNotFoundException(preset)
            preset
        }

        return PresetsConfig.presets[name] ?: throw PresetNotFoundException(name)
    }

    fun authenticate(fuser: FriendUser, preset: String?): Boolean
    {
        if(preset != null)
        {
            if(PresetGrants.testGrant(preset, fuser.user.id))
                return true
        }



        return false
    }
}