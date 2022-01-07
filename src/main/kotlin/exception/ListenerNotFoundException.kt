package com.github.asforest.mshell.exception

import com.github.asforest.mshell.event.Event

class ListenerNotFoundException(label: String)
    : BaseException("The Listener(label) not found in event object")