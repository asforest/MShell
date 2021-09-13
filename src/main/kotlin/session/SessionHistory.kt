
package com.github.asforest.mshell.session

import net.mamoe.mirai.contact.User
import com.github.asforest.mshell.exception.BaseException
import com.github.asforest.mshell.exception.SessionNotFoundException
import com.github.asforest.mshell.exception.UserAlreadyConnectedException
import com.github.asforest.mshell.session.SessionManager.name

object SessionHistory
{
    val records = mutableMapOf<User, Long>()

    suspend fun tryToResume(user: User)
    {
        if(SessionManager.isUserConnected(user))
            throw UserAlreadyConnectedException("The user ${user.name} had already connected with a session")

        suspend fun newSession() { SessionManager.openSessionAutomatically(user).also { records[user] = it.pid } }

        if(user in records.keys)
        {
            try {
                val pid = records[user]!!
                SessionManager.connectToSession(user, pid)
                user.sendMessage("Reconnected!")
            } catch (e: SessionNotFoundException) {
                newSession()
            }
        } else {
            newSession()
        }
    }
}