package com.github.asforest.mshell.configuration

import com.github.asforest.mshell.model.EnvironmentalPreset

object PresetsConfig : YamlConfig("presets.yml")
{
    var defaultPreset: String = ""
    var presets = HashMap<String, EnvironmentalPreset>()

    fun validate()
    {
        if(defaultPreset !in presets.keys)
            defaultPreset = ""
    }

    override fun onLoad(deserialized: HashMap<String, Any>)
    {
        defaultPreset = (deserialized["default-preset"] as String)

        presets.clear()
        for (entry in deserialized["presets"] as HashMap<String, Any>)
            presets[entry.key] = map2preset(entry.key, entry.value as HashMap<String, Any>)

        validate()
    }

    override fun onSave(serialized: HashMap<String, Any>)
    {
        serialized["default-preset"] = defaultPreset

        val temp = HashMap<String, Any>()
        for (entry in presets)
            temp[entry.key] = preset2map(entry.value)
        serialized["presets"] = temp

        validate()
    }

    private fun map2preset(name: String, fromMap: HashMap<String, Any>): EnvironmentalPreset
    {
        return EnvironmentalPreset(
            name = name,
            command = fromMap["shell"] as String,
            charset = fromMap["charset"] as String,
            workdir = fromMap["workdir"] as String,
            env = fromMap["env"] as HashMap<String, String>,
            input = fromMap["exec"] as String,
            singleInstance = fromMap["single-instance"] as Boolean,
            columns = fromMap["terminal-columns"] as Int,
            rows = fromMap["terminal-rows"] as Int
        )
    }

    private fun preset2map(preset: EnvironmentalPreset): HashMap<String, Any>
    {
        val map = LinkedHashMap<String, Any>()

        map["shell"] = preset.command
        map["charset"] = preset.charset
        map["workdir"] = preset.workdir
        map["env"] = preset.env
        map["exec"] = preset.input
        map["single-instance"] = preset.singleInstance
        map["terminal-columns"] = preset.columns
        map["terminal-rows"] = preset.rows

        return map
    }
}

