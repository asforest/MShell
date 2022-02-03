package com.github.asforest.mshell.exception.business

import com.github.asforest.mshell.exception.AbstractBusinessException

class NoPermissionToUsePresetExcetption(preset: String)
    : AbstractBusinessException("没有权限使用预设 '$preset'")