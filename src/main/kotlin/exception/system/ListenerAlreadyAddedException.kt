package com.github.asforest.mshell.exception.system

import com.github.asforest.mshell.exception.AbstractSystemException

class ListenerAlreadyAddedException(label: String)
    : AbstractSystemException("The Listener($label) has already been added to the event object")