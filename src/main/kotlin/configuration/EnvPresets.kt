package com.github.asforest.mshell.configuration

object EnvPresets : YamlConfig("presets.yml")
{
    var defaultPreset: String = ""
    var presets = HashMap<String, Preset>()

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
            presets[entry.key] = Preset(entry.value as HashMap<String, Any>)

        validate()
    }

    override fun onSave(serialized: HashMap<String, Any>)
    {
        serialized["default-preset"] = defaultPreset

        val temp = HashMap<String, Any>()
        for (entry in presets)
            temp[entry.key] = entry.value.toMap()
        serialized["presets"] = temp

        validate()
    }

    data class Preset(
        var shell: String = "",
        var charset: String = "",
        var workdir: String = "",
        var env: HashMap<String, String> = HashMap(),
        var exec: String = ""
    ) {
        constructor(fromMap: HashMap<String, Any>) : this()
        {
            shell = fromMap["shell"] as String
            charset = fromMap["charset"] as String
            workdir = fromMap["workdir"] as String
            env = fromMap["env"] as HashMap<String, String>
            exec = fromMap["exec"] as String
        }

        fun toMap(): HashMap<String, Any>
        {
            val map = HashMap<String, Any>()

            map["shell"] = shell
            map["charset"] = charset
            map["workdir"] = workdir
            map["env"] = env
            map["exec"] = exec

            return map
        }
    }
}

