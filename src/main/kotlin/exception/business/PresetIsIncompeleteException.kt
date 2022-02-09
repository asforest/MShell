package com.github.asforest.mshell.exception.business

import com.github.asforest.mshell.exception.AbstractBusinessException
import com.github.asforest.mshell.model.Preset

class PresetIsIncompeleteException(preset: Preset)
    : AbstractBusinessException("环境预设还未配置完毕'${preset.name}'，请检查并完善以下选项: shell, charset")