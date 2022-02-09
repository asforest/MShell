package com.github.asforest.mshell.session.user

import net.mamoe.mirai.contact.User

class FriendUser(val user: User) : AbstractSessionUser()
{
    override suspend fun sendMessage(message: String)
    {
        user.sendMessage(message)
    }

    override fun toString(): String
    {
        return "${user.nick}(${user.id})"
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