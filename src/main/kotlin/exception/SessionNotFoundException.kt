package com.github.asforest.mshell.exception

class SessionNotFoundException(pid: Long)
    : BaseException("会话 pid($pid) 找不到")