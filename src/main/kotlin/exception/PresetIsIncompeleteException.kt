package com.github.asforest.mshell.exception

class PresetIsIncompeleteException(presetName: String)
    : BaseException("环境预设还未配置完毕'$presetName'，请检查并完善以下选项: shell, charset")