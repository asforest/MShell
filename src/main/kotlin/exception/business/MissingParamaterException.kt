package com.github.asforest.mshell.exception.business

import com.github.asforest.mshell.exception.AbstractBusinessException

class MissingParamaterException(parameterName: String)
    : AbstractBusinessException("参数'$parameterName'不能为空")