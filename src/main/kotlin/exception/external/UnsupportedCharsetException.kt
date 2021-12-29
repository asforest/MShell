package com.github.asforest.mshell.exception.external

import com.github.asforest.mshell.exception.BaseException

class UnsupportedCharsetException(charset: String)
    : BaseExternalException("不支持的字符集'$charset'")