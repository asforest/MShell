package com.github.asforest.mshell.exception.business

import com.github.asforest.mshell.exception.AbstractBusinessException

class SessionUserAlreadyConnectedException : AbstractBusinessException
{
    constructor(pid: Long) : super("当前已经连接到了另一个会话(pid:$pid)")

    constructor(groupId: Long, pid: Long) : super("群聊($groupId)已经连接到了另一个会话($pid)")
}