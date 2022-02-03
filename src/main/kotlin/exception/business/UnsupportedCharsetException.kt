package com.github.asforest.mshell.exception.business

import com.github.asforest.mshell.exception.AbstractBusinessException

class UnsupportedCharsetException(charset: String)
    : AbstractBusinessException("不支持的字符集'$charset'")