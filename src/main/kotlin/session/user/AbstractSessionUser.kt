package com.github.asforest.mshell.session.user

/**
 * 其子类有：[GroupUser]、[FriendUser]、[ConsoleUser]
 */
abstract class AbstractSessionUser
{
    /**
     * 发送消息的实现方法。此方法应该由子类重写
     */
    abstract suspend fun sendMessage(message: String)

    abstract override fun toString(): String

    abstract override fun equals(other: Any?): Boolean

    abstract override fun hashCode(): Int
}