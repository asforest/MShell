package com.github.asforest.mshell.exception

class SessionConnectionNotMatchException(pidToClose: Long, pidCurrent: Long)
    : BaseException("the Session(pid: $pidToClose) which will be closed did not belongs to current Session(pid: $pidCurrent)")