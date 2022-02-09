package com.github.asforest.mshell.session.user

import com.github.asforest.mshell.session.AbstractSessionUser
import net.mamoe.mirai.contact.Group

class GroupUser(
    val group: Group
) : AbstractSessionUser() {

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
        if (other == null || other !is AbstractSessionUser)
            return false

        return toString() == other.toString()
    }

    override fun hashCode(): Int
    {
        val i = toString().hashCode()
        return (i shl 5) - i
    }

}