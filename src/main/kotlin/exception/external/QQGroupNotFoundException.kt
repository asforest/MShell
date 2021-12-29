package com.github.asforest.mshell.exception.external

class QQGroupNotFoundException(group: Long)
    : BaseExternalException("找不到群聊($group)")