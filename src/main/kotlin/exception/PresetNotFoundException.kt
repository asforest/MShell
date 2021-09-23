package com.github.asforest.mshell.exception

class PresetNotFoundException(presetName: String)
    : BaseException("找不到环境预设'$presetName'")