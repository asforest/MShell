package com.github.asforest.mshell.exception.external

class MissingParamaterException(parameterName: String)
    : BaseExternalException("参数'$parameterName'不能为空")