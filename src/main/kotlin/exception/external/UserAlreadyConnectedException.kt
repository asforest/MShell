package com.github.asforest.mshell.exception.external

import com.github.asforest.mshell.exception.external.BaseExternalException

class UserAlreadyConnectedException(pid: Long? = null)
    : BaseExternalException("你已经连接到了另一个会话"+(if(pid != null) " (pid:$pid) " else "")+"上了")