package com.github.asforest.mshell.exception.system

import com.github.asforest.mshell.exception.AbstractSystemException

class EventLoopReachCountLimitException(maxCount: Int)
    : AbstractSystemException("Event loop reached the max count($maxCount) of Event class")