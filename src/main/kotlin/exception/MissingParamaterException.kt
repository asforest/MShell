package com.github.asforest.mshell.exception

class MissingParamaterException(parameterName: String)
    : BaseException("参数'$parameterName'不能为空")