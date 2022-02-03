package com.github.asforest.mshell.exception.system

import com.github.asforest.mshell.exception.AbstractSystemException

class SingleInstanceException(val preset: String)
    : AbstractSystemException("the preset '$preset' is single instance")