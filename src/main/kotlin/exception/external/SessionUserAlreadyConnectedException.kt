package com.github.asforest.mshell.exception.external

class SessionUserAlreadyConnectedException : BaseExternalException
{
    constructor(pid: Long) : super("当前已经连接到了另一个会话(pid:$pid)")

    constructor(groupId: Long, pid: Long) : super("群聊($groupId)已经连接到了另一个会话($pid)")
}