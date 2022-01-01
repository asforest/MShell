package com.github.asforest.mshell.session.user

import com.github.asforest.mshell.session.SessionUser
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member

class GroupUser(
    val group: Group
) : SessionUser() {

    override suspend fun onSendMessage(message: String)
    {
        group.sendMessage(message)
    }

    override fun toString(): String
    {
        return "${group.name}<${group.id}>"
    }

    override fun equals(other: Any?): Boolean
    {
        if (other == null || other !is SessionUser)
            return false

        return toString() == other.toString()
    }

    override fun hashCode(): Int
    {
        val i = toString().hashCode()
        return (i shl 5) - i
    }

}