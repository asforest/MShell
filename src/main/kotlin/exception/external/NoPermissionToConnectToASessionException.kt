package com.github.asforest.mshell.exception.external

class NoPermissionToConnectToASessionException (pid: Long)
    : BaseExternalException("没有权限连接到会话(pid: $pid)")