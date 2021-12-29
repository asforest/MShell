package com.github.asforest.mshell.exception.external

import com.github.asforest.mshell.exception.external.BaseExternalException

class PresetNotFoundException(presetName: String)
    : BaseExternalException("找不到环境预设'$presetName'")