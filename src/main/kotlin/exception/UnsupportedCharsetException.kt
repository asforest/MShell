package com.github.asforest.mshell.exception

class UnsupportedCharsetException(charset: String)
    : BaseException("不支持的字符集'$charset'")