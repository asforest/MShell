package com.github.asforest.mshell.configuration

class EnvironmentPresets : ConfigProxy.ProxiableConfiguration
{
    var defaultPreset: String = ""
    var presets = mutableMapOf<String, Preset>()

    fun validate()
    {
        if(defaultPreset !in presets.keys)
            defaultPreset = ""
    }

    override fun onLoad()
    {
        validate()
    }

    override fun onSave()
    {
        validate()
    }
}

data class Preset(
    var shell: String = "",
    var charset: String = "",
    var cwd: String = "",
    var env: MutableMap<String, String> = mutableMapOf(),
    var exec: String = ""
)