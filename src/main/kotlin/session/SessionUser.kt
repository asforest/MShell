package com.github.asforest.mshell.session

import net.mamoe.mirai.console.command.ConsoleCommandSender
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.User

/**
 * AbstractSessionUser表示一个用户（MShell用户，不是其它概念中的用户）
 */
sealed class SessionUser
{
    /**
     * 发送消息的实现方法。此方法应该由子类重写
     */
    abstract suspend fun sendMessage(message: String)

    abstract override fun toString(): String

    abstract override fun equals(other: Any?): Boolean

    abstract override fun hashCode(): Int

    companion object {

    }

    class ConsoleUser : SessionUser()
    {
        override suspend fun sendMessage(message: String)
        {
            ConsoleCommandSender.sendMessage(message)
        }

        override fun toString(): String
        {
            return "<Console>"
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

    class FriendUser(val user: User) : SessionUser()
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

    class GroupUser(val group: Group) : SessionUser()
    {

        override suspend fun sendMessage(message: String)
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
}