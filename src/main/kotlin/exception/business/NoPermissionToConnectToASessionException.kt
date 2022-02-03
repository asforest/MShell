package com.github.asforest.mshell.exception.business

import com.github.asforest.mshell.exception.AbstractBusinessException

class NoPermissionToConnectToASessionException (pid: Long)
    : AbstractBusinessException("没有权限连接到会话(pid: $pid)")