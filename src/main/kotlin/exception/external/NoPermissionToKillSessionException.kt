package com.github.asforest.mshell.exception.external

class NoPermissionToKillSessionException(pid: Long)
    : BaseExternalException("没有权限结束会话(pid: $pid)")