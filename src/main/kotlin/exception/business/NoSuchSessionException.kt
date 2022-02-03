package com.github.asforest.mshell.exception.business

import com.github.asforest.mshell.exception.AbstractBusinessException

class NoSuchSessionException(pid: Long)
    : AbstractBusinessException("会话 pid($pid) 找不到")