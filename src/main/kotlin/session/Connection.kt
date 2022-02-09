package com.github.asforest.mshell.session

class Connection(
    val user: AbstractSessionUser,
    val session: Session,
) {
    val sessionPid: Long get() = session.pid

    /**
     * 距离上一个在线或者离线状态切换过后所经过的时间
     */
    var whenOnlineChanged: Long = System.currentTimeMillis()

    /**
     * 当前连接是否在线
     */
    var isOnline: Boolean
        get() = _isOnline
        set(value) {
            if(value != isOnline)
                whenOnlineChanged = System.currentTimeMillis()
            _isOnline = value
        }

    private var _isOnline = true

    override fun equals(other: Any?): Boolean
    {
        if (other == null || other !is Connection)
            return false

        return hashCode() == other.hashCode()
    }

    override fun hashCode(): Int
    {
        val i = sessionPid.hashCode() xor user.hashCode()
        return (i shl 5) - i
    }
}