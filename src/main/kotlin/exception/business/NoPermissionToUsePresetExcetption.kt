package com.github.asforest.mshell.exception.business

import com.github.asforest.mshell.exception.AbstractBusinessException

class NoPermissionToUsePresetExcetption(preset: String? = null)
    : AbstractBusinessException(if (preset != null)
        "没有权限使用任何预设" else "没有权限使用预设 '$preset'")