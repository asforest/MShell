package com.github.asforest.mshell.exception.system

import com.github.asforest.mshell.exception.AbstractSystemException
import com.github.asforest.mshell.session.Session

class SessionAlreadyRegisteredException(session: Session)
    : AbstractSystemException("Seesion(pid: ${session.pid}) attempted to register repeatedly")