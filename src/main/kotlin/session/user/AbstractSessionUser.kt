package com.github.asforest.mshell.session.user

/**
 * AbstractSessionUser表示一个用户（MShell用户，不是其它概念中的用户）
 *
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