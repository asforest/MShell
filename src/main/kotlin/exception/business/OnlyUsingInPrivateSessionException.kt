package com.github.asforest.mshell.exception.business

import com.github.asforest.mshell.exception.AbstractBusinessException

class OnlyUsingInPrivateSessionException(command: String)
    : AbstractBusinessException("指令 '$command' 只能在私聊会话中使用")