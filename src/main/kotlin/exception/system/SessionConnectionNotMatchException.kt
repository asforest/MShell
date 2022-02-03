package com.github.asforest.mshell.exception.system

import com.github.asforest.mshell.exception.AbstractSystemException

class SessionConnectionNotMatchException(pidToClose: Long, pidCurrent: Long)
    : AbstractSystemException("the Session(pid: $pidToClose) which will be closed did not belongs to current Session(pid: $pidCurrent)")