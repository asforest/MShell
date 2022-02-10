package com.github.asforest.mshell.exception.system

import com.github.asforest.mshell.exception.AbstractSystemException

class ListenerAlreadyAddedException
    : AbstractSystemException("Listener is registered repeatedly")