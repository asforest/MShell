package com.github.asforest.mshell.exception.external

class OnlyUsingInPrivateSessionException(command: String)
    : BaseExternalException("指令 '$command' 只能在私聊会话中使用")