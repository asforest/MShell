package com.github.asforest.mshell.exception.business

import com.github.asforest.mshell.exception.AbstractBusinessException

class PresetNotFoundException(presetName: String)
    : AbstractBusinessException("找不到环境预设'$presetName'")