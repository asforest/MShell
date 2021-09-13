package com.github.asforest.mshell.configuration

class EnvPresets : ConfigProxy.ProxiableConfiguration
{
    var defaultPreset: String = ""
    var presets = mutableMapOf<String, Preset>()

    class Preset
    {
        var shell: String = ""
        var cwd: String = ""
        var env: MutableMap<String, String> = mutableMapOf()
        var exec: String = ""

        override fun toString(): String = "shell($shell), cwd($cwd), env($env), exec($exec)"
    }

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