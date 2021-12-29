package com.github.asforest.mshell.exception

import com.github.asforest.mshell.session.Session

class SessionNotRegisteredException(session: Session)
    : BaseException("the session(pid: ${session.pid}) has not been registered yet")