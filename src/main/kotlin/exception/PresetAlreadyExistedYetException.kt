package com.github.asforest.mshell.exception

class PresetAlreadyExistedYetException(presetName: String)
    : BaseException("环境预设'$presetName'已经存在了")