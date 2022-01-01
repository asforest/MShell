package com.github.asforest.mshell.exception

class SingleInstanceException(val preset: String)
    : BaseException("the preset '$preset' is single instance")