package com.github.asforest.mshell.exception

class EventLoopReachCountLimitException(maxCount: Int)
    : BaseException("Event loop reached the max count($maxCount) of Event class")