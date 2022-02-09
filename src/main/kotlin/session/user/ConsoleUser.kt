package com.github.asforest.mshell.session.user

import net.mamoe.mirai.console.command.ConsoleCommandSender

class ConsoleUser : AbstractSessionUser()
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