package com.github.asforest.mshell.exception.system

import com.github.asforest.mshell.exception.AbstractSystemException

class ListenerNotFoundException
    : AbstractSystemException("Listener is not registered yet")