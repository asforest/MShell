
package com.github.asforest.mshell.session

import net.mamoe.mirai.console.command.ConsoleCommandSender
import com.github.asforest.mshell.MShell
import com.github.asforest.mshell.command.MainCommand
import com.github.asforest.mshell.configuration.EnvPresets
import com.github.asforest.mshell.exception.*
import com.github.asforest.mshell.type.USER
import net.mamoe.mirai.contact.User
import java.io.File

object SessionManager
{
    val scd = SessionContinuationDispatcher()
    val sessions = mutableListOf<Session>()
    val connections = mutableMapOf<USER, Session>()
    val historicalConnections = mutableMapOf<User, Long>()

    suspend fun ResumeOrCreate(user: User)
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
                historicalConnections[user] = openSessionAutomatically(user).pid
            }
        } else {
            historicalConnections[user] = openSessionAutomatically(user).pid
        }
    }

    suspend fun openSessionAutomatically(user: USER, preset: String? = null): Session
    {
        return openSession(preset).connect(user).start()
    }

    fun openSession(preset: String? = null): Session
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
        envPreset = MainCommand.getPresetWithThrow(epName)

        if(envPreset.shell == "")
            throw PresetIsIncompeleteException("The preset '$envPreset' is incomplete, the field 'shell' is not be set yet")

        val session = createSession(envPreset.shell, envPreset.cwd, envPreset.env)

        // 自动执行exec
        if(envPreset.exec != "")
            session.stdin.println(envPreset.exec)

        return session
    }

    fun createSession(command: String, workdir: String? =null, env: Map<String, String>? =null): Session
    {
        val _workdir = File(if(workdir!=null && workdir!= "") workdir else System.getProperty("user.dir"))
        val _env = env ?: mapOf()

        val process = ProcessBuilder()
            .command(command)
            .directory(_workdir)
            .also { it.environment().putAll(_env) }
            .redirectErrorStream(true)
            .redirectInput(ProcessBuilder.Redirect.PIPE)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .start()
        return Session(process, this).also { sessions += it }
    }

    suspend fun connect(user: USER, pid: Long)
    {
        val session = getSessionByPid(pid)
            ?: throw SessionNotFoundException("The session of pid($pid) was not be found")
        connect(user, session)
    }

    suspend fun connect(user: USER, session: Session)
    {
        getSessionByUserConnected(user)?.also {
            if(it == session)
                throw UserAlreadyConnectedException("You have has already connected to this session")
            throw UserAlreadyConnectedException("You have already connected to a other session (${it.pid})")
        }

        // 记录链接历史
        if(user != null)
            historicalConnections[user] = session.pid

        // 注册连接(Connection)
        connections[user] = session

        // 分发事件
        session.onUserConnect { it(user) }

        user.sendMessage2("Connected to pid(${session.pid})")
    }

    suspend fun disconnect(user: USER)
    {
        if(!isUserConnected(user))
            throw UserNotConnectedYetException("You have not connected to a session yet")

        val session = getSessionByUserConnected(user)!!

        // 分发事件
        session.onUserDisconnect { it(user) }

        // 注销连接(Connection)
        connections.remove(user)

        user.sendMessage2("Disconnected from pid(${session.pid})")
    }

    suspend fun disconnectAll(session: Session)
    {
        for(user in getUsersConnectedToSession(session))
        {
            // 分发事件
            session.onUserDisconnect { it(user) }

            // 注销连接(Connection)
            connections.remove(user)

            user.sendMessage2("Disconnected from pid(${session.pid})")
        }
    }

    fun getSessionByPid(pid: Long): Session?
    {
        for (session in sessions)
            if(session.pid == pid)
                return session
        return null
    }

    fun getSessionByUserConnected(user: USER): Session?
    {
        for ((u, s) in connections)
            if(u == user)
                return s
        return null
    }

    fun isUserConnected(user: USER): Boolean
    {
        return user in connections.keys
    }

    fun getUsersConnectedToSession(session: Session): List<USER>
    {
        return connections.filter { it.value == session }.map { it.key }
    }

    val USER.name: String get() = if(this != null) "$nick($id)" else "<Console>"

    suspend fun USER.sendMessage2(msg: String) {
        if(this != null) sendMessage(msg) else ConsoleCommandSender.sendMessage(msg)
    }
}