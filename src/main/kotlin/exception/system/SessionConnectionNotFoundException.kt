package com.github.asforest.mshell.exception.system

import com.github.asforest.mshell.exception.AbstractSystemException

class SessionConnectionNotFoundException(user: String)
    : AbstractSystemException("The session connection with user($user) not found)")