package com.github.asforest.mshell.exception.external

import com.github.asforest.mshell.exception.external.BaseExternalException

class NoSuchSessionException(pid: Long)
    : BaseExternalException("会话 pid($pid) 找不到")