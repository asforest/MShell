package com.github.asforest.mshell.exception.business

import com.github.asforest.mshell.exception.AbstractBusinessException

class UsingInConsoleNotAllowedException(command: String)
    : AbstractBusinessException("指令 '$command' 不允许在控制台里使用")