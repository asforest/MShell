package com.github.asforest.mshell.exception.external

class NotAllowedToUseInConsoleException(command: String)
    : BaseExternalException("指令 '$command' 不允许在控制台里使用")