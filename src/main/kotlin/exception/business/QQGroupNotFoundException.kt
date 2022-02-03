package com.github.asforest.mshell.exception.business

import com.github.asforest.mshell.exception.AbstractBusinessException

class QQGroupNotFoundException(group: Long)
    : AbstractBusinessException("找不到群聊($group)")