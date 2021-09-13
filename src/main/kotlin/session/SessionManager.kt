
package com.github.asforest.mshell.session

import net.mamoe.mirai.console.command.ConsoleCommandSender
import com.github.asforest.mshell.MShell
import com.github.asforest.mshell.command.MainCommand
import com.github.asforest.mshell.configuration.EnvPresets
import com.github.asforest.mshell.exception.*
import com.github.asforest.mshell.session.SessionManager.sendMessage2
import com.github.asforest.mshell.type.USER
import java.io.File

object SessionManager
{
    val scd = SessionContinuationDispatcher()
    val sessions = mutableListOf<Session>()
    val connections = mutableMapOf<USER, Session>()

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
                throw NoDefaultPresetException("The default preset had not set yet or it was invalid.")
            def
        } else {
            if(preset !in ep.presets.keys)
                throw PresetNotFoundException("The preset '$preset' was not found")
            ep.defaultPreset
        }
        envPreset = MainCommand.getPresetWithThrow(epName)

        if(envPreset.shell == "")
            throw PresetIsIncompeleteException("The preset '$envPreset' is incomplete, the field 'shell' is not set yet")

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

    suspend fun connectToSession(user: USER, pid: Long)
    {
        val session = getSessionByPid(pid)
            ?: throw SessionNotFoundException("The session of pid($pid) was not be found")
        connectToSession(user, session)
    }

    suspend fun connectToSession(user: USER, session: Session)
    {
        getSessionByUserConnected(user)?.also {
            if(it == session)
                throw UserAlreadyConnectedException("The user ${user.name} had already connected with this session")
            throw UserAlreadyConnectedException("The user ${user.name} had already connected with a other session ${it.pid}")
        }

        // 记录链接历史
        if(user != null)
            SessionHistory.records[user] = session.pid

        // 注册连接(Connection)
        connections[user] = session

        // 分发事件
        session.onUserConnect(session) { it(user) }

        user.sendMessage2("Connect ${user.name} to (${session.pid})")
    }

    suspend fun disconnectFromSession(user: USER)
    {
        if(!isUserConnected(user))
            throw UserNotConnectedYetException("The user ${user.name} has not connected with a session yet")

        val session = getSessionByUserConnected(user)!!

        // 分发事件
        session.onUserDisconnect(session) { it(user) }

        // 注销连接(Connection)
        connections.remove(user)

        user.sendMessage2("Disconnect ${user.name} from (${session.pid})")
    }

    suspend fun disconnectAllUsers(session: Session)
    {
        for(user in getUsersConnectedToSession(session))
        {
            // 分发事件
            session.onUserDisconnect(session) { it(user) }

            // 注销连接(Connection)
            connections.remove(user)

            user.sendMessage2("Disconnect ${user.name} from (${session.pid})")
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

//    val USER.name: String get() = if(user != null) "${user!!.nick}(${user!!.id})" else "<Console>"
    val USER.name: String get() = if(this != null) "$nick($id)" else "<Console>"

    suspend fun USER.sendMessage2(msg: String) {
        if(this != null) sendMessage(msg) else ConsoleCommandSender.sendMessage(msg)
    }
}