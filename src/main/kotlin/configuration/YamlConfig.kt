package com.github.asforest.mshell.configuration

import com.github.asforest.mshell.MShellPlugin
import com.github.asforest.mshell.util.FileObj
import org.yaml.snakeyaml.Yaml

abstract class YamlConfig(val filename: String)
{
    val pluginDataFolder: FileObj = FileObj(MShellPlugin.configFolder)
    val configFile: FileObj = pluginDataFolder + filename

    fun read(saveDefault: Boolean = false)
    {
        if(saveDefault && !configFile.exists)
            write()

        if(configFile.exists)
        {
            val yaml = Yaml()
            val deserialized = yaml.load<HashMap<String, Any>>(configFile.content)
            onLoad(deserialized)
        }
    }

    fun write()
    {
        val serialized = LinkedHashMap<String, Any>()
        onSave(serialized)

        val serializedText = Yaml().dumpAsMap(serialized)
        configFile.content = serializedText
    }

    protected abstract fun onLoad(deserialized: HashMap<String, Any>)

    protected abstract fun onSave(serialized: HashMap<String, Any>)
}