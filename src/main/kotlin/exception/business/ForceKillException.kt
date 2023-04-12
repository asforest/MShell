package com.github.asforest.mshell.exception.business

import com.github.asforest.mshell.exception.AbstractBusinessException

class ForceKillException(reason: String)
    : AbstractBusinessException(reason)