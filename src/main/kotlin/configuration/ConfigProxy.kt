package com.github.asforest.mshell.configuration

import com.esotericsoftware.yamlbeans.YamlReader
import com.esotericsoftware.yamlbeans.YamlWriter
import com.github.asforest.mshell.MShellPlugin
import com.github.asforest.mshell.util.FileObj
import java.io.ByteArrayOutputStream
import java.io.PrintWriter

class ConfigProxy<T> where T : ConfigProxy.ProxiableConfiguration
{
    val filename: String
    val pluginDataFolder: FileObj = FileObj(MShellPlugin.configFolder)
    val configFile: FileObj
    var ins: T

    constructor(clazz: Class<T>, filename: String)
    {
        this.filename = filename
        this.configFile = pluginDataFolder + filename
        ins = clazz.getConstructor().newInstance()
    }

    fun read()
    {
        if(configFile.exists)
        {
            val serialized =  configFile.content
            val reader = YamlReader(serialized)
            ins = reader.read().also { reader.close() } as T
            ins.onLoad()
        }
    }

    fun write()
    {
        val buf = ByteArrayOutputStream()
        val writer = YamlWriter(PrintWriter(buf))
        ins.onSave()
        writer.write(ins)
        writer.close()

        val serialized = buf.toByteArray().decodeToString()
        configFile.content = serialized
    }

    operator fun invoke(): T = ins

    interface ProxiableConfiguration
    {
        fun onLoad()
        fun onSave()
    }
}