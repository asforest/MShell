package com.github.asforest.mshell.configuration

import com.github.asforest.mshell.data.Preset

object PresetsConfig : YamlConfig("presets.yml")
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

    private fun map2preset(name: String, fromMap: HashMap<String, Any>): Preset
    {
        val preset = Preset(name)

        preset.command = fromMap["shell"] as String? ?: preset.command
        preset.charset = fromMap["charset"] as String? ?: preset.charset
        preset.workdir = fromMap["workdir"] as String? ?: preset.workdir
        preset.env = fromMap["env"] as MutableMap<String, String>? ?: preset.env
        preset.input = fromMap["exec"] as String? ?: preset.input
        preset.singleInstance = fromMap["single-instance"] as Boolean? ?: preset.singleInstance
        preset.columns = fromMap["terminal-columns"] as Int? ?: preset.columns
        preset.rows = fromMap["terminal-rows"] as Int? ?: preset.rows
        preset.truncationThreshold = fromMap["truncation-threshold"] as Int? ?: preset.truncationThreshold
        preset.batchingInteval = fromMap["batching-inteval"] as Int? ?: preset.batchingInteval
        preset.lastwillCapacity = fromMap["lastwill-capacity"] as Int? ?: preset.lastwillCapacity
        preset.jsonMode = fromMap["json-mode"] as Boolean? ?: preset.jsonMode

        return preset
    }

    private fun preset2map(preset: Preset): HashMap<String, Any>
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
        map["truncation-threshold"] = preset.truncationThreshold
        map["batching-inteval"] = preset.batchingInteval
        map["lastwill-capacity"] = preset.lastwillCapacity
        map["json-mode"] = preset.jsonMode

        return map
    }

//    @Retention(AnnotationRetention.RUNTIME)
//    @Target(AnnotationTarget.FIELD)
//    annotation class StoreField(val name: String)
}

