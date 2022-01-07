package com.github.asforest.mshell.exception

class ListenerAlreadyAddedException(label: String)
    : BaseException("The Listener($label) has already been added to the event object")