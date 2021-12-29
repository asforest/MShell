package com.github.asforest.mshell.exception

import com.github.asforest.mshell.session.Session

class SessionAlreadyRegisteredException(session: Session)
    : BaseException("Seesion(pid: ${session.pid}) attempted to register repeatedly")