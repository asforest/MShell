package com.github.asforest.mshell.exception.system

import com.github.asforest.mshell.exception.AbstractSystemException

class ListenerNotFoundException(label: String)
    : AbstractSystemException("The Listener(label) not found in event object")