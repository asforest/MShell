package com.github.asforest.mshell.session

class Connection(
    val user: SessionUser,
    val session: Session,
) {
    val isValid: Boolean get() = session.isAlive
    val sessionPid: Long get() = session.pid
    val since: Long = System.currentTimeMillis()
}