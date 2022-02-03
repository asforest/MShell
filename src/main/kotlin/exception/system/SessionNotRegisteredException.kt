package com.github.asforest.mshell.exception.system

import com.github.asforest.mshell.exception.AbstractSystemException
import com.github.asforest.mshell.session.Session

class SessionNotRegisteredException(session: Session)
    : AbstractSystemException("the session(pid: ${session.pid}) has not been registered yet")