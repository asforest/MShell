package com.github.asforest.mshell.exception.business

import com.github.asforest.mshell.exception.AbstractBusinessException

class NoPermissionToKillSessionException(pid: Long)
    : AbstractBusinessException("没有权限结束会话(pid: $pid)")