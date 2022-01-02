package com.github.asforest.mshell.exception.external

class NoPermissionToUsePresetExcetption(preset: String)
    : BaseExternalException("没有权限使用预设 '$preset'")