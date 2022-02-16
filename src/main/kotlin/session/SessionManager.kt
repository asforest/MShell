package com.github.asforest.mshell.session

import com.github.asforest.mshell.configuration.PresetsConfig
import com.github.asforest.mshell.exception.business.*
import com.github.asforest.mshell.exception.system.SessionAlreadyRegisteredException
import com.github.asforest.mshell.exception.system.SessionNotRegisteredException
import com.github.asforest.mshell.model.Preset

object SessionManager
{
    /**
     * 所有注册过的会话
     */
    val sessions = mutableListOf<Session>()

    /**
     * 快速开启一个新的Session并完成注册。如果不嫌麻烦，也可以手动new一个Session对象，然后自己注册
     * @param presetName 环境预设
     * @param user 会话启动后，要立即连接上来的用户
     * @return 创建的Session，也可能是复用的Session
     * @throws SessionUserAlreadyConnectedException 用户已经连接到了另一个会话上了
     */
    fun createSession(presetName: String?, user: SessionUser?): Session
    {
        // user不能连接到任何会话，否则会抛异常
        if (user != null)
        {
            val session = getSession(user)
            if(session != null)
            {
                if(user is SessionUser.GroupUser)
                    throw SessionUserAlreadyConnectedException(user.group.id, session.pid)
                else
                    throw SessionUserAlreadyConnectedException(session.pid)
            }
        }

        val preset = useDefaultPreset(presetName)
        val session: Session

        // 单实例
        val hasIns = sessions.any { it.preset == preset }
        if(preset.singleInstance && hasIns)
        {
            // 复用会话
            session = sessions.first { it.preset == preset }

            // 用户自动连接
            if(user != null)
                session.connect(user)
        } else {
            // 创建会话
            session = Session(this, preset, user) // 用户自动连接由构造函数代为完成
            registerSession(session)
        }

        return session
    }

    /**
     * 尝试重连会话，如果无法重连，会创建一个新的会话
     * @param user 要重连/新建会话的用户
     * @param preset 可选的预设，只在创建新会话时生效
     * @throws SessionUserAlreadyConnectedException 用户已经连接到了另一个会话上了
     */
    fun reconnectOrCreate(user: SessionUser, preset: String? = null)
    {
        getSession(user)?.also { throw SessionUserAlreadyConnectedException(it.pid) }

        val lastSession = sessions.firstOrNull { it.connectionManager.getConnection(user, true) != null }

        if (lastSession != null)
        {
            lastSession.connect(user)
//            lastSession.connectionManager.openConnection(user)
        } else {
            createSession(preset, user)
        }
    }

    /**
     * 使一个用户通过pid连接到一个会话
     * @param user 用户
     * @param pid 子进程的pid
     * @throws NoSuchSessionException 如果 pid 无效
     */
    fun connect(user: SessionUser, pid: Long): Connection
    {
        val session = getSession(pid) ?: throw NoSuchSessionException(pid)
        return session.connect(user)
    }

    /**
     * 使一个用户从当前会话上断开
     * @param user 用户
     * @throws UserNotConnectedException 如果用户未连接到任何会话上
     */
    fun disconnect(user: SessionUser): Connection
    {
        val session = getSession(user) ?: throw UserNotConnectedException()
        return session.disconnect(user)
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
     * 根据PID获取Session
     */
    fun getSession(pid: Long): Session?
    {
        return sessions.firstOrNull { it.pid == pid }
    }

    /**
     * 根据User获取相应已经连接的Session
     */
    fun getSession(user: SessionUser): Session?
    {
        return sessions.firstOrNull { it.isUserConnected(user) }
    }

    /**
     * 检查一个User是否连接到了任意一个Session上
     */
    fun hasUserConnectedToAnySession(user: SessionUser): Boolean
    {
        return sessions.any { it.isUserConnected(user) }
    }

    /**
     * 使用指定环境预设或者默认环境预设
     * @throws NoDefaultPresetException 默认预设不存在或者无效
     * @throws PresetNotFoundException 指定的环境预设不存在
     */
    fun useDefaultPreset(preset: String?): Preset
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

    /**
     * 注册一个Session
     * @throws SessionAlreadyRegisteredException 会话已经被注册过了
     */
    private fun registerSession(session: Session)
    {
        if(isSessionRegistered(session))
            throw SessionAlreadyRegisteredException(session)

        sessions += session

        session.onProcessExit.once { unregisterSession(session) }
    }

    /**
     * 取消注册一个Session
     * @throws SessionNotRegisteredException 会话从未注册过
     */
    private fun unregisterSession(session: Session)
    {
        if(!isSessionRegistered(session))
            throw SessionNotRegisteredException(session)

        // 取消注册
        sessions.remove(session)
    }

    /**
     * 判断一个会话是否注册过了
     */
    private fun isSessionRegistered(session: Session): Boolean
    {
        return session in sessions
    }
}