package com.github.asforest.mshell.exception

class SessionConnectionNotFoundException(user: String)
    : BaseException("The session connection with user($user) was not be found)")