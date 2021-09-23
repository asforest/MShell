package com.github.asforest.mshell.exception

class UserAlreadyConnectedException(pid: Long? = null)
    : BaseException("你已经连接到了另一个会话"+(if(pid != null) " (pid:$pid) " else "")+"上了")