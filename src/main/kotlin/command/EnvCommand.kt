package com.github.asforest.mshell.command

import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import com.github.asforest.mshell.MShell
import com.github.asforest.mshell.configuration.MainConfig
import com.github.asforest.mshell.configuration.ConfigProxy
import com.github.asforest.mshell.configuration.EnvPresets
import com.github.asforest.mshell.exception.BaseException
import com.github.asforest.mshell.exception.PresetAlreadyExistedYetException
import com.github.asforest.mshell.exception.PresetNotFoundException

object EnvCommand : CompositeCommand(
    MShell,
    primaryName = "mshelle",
    description = "MShell插件配置指令",
    secondaryNames = arrayOf("mse", "me")
) {
    val ep: ConfigProxy<EnvPresets> get() = MShell.ep

    @SubCommand @Description("创建一个环境预设")
    suspend fun CommandSender.add(
        @Name("preset") presetName: String
    ) {
        withCatch {
            if (presetName in ep.ins.presets.keys)
                throw PresetAlreadyExistedYetException("The preset '$presetName' has already existed yet")
            ep.ins.presets[presetName] = EnvPresets.Preset()
            if(ep.ins.defaultPreset == "")
                ep.ins.defaultPreset = presetName
            ep.write()
            list(presetName)
        }
    }

    @SubCommand @Description("删除一个环境预设")
    suspend fun CommandSender.remove(
        @Name("preset") presetName: String
    ) {
        withCatch {
            getPresetWithThrow(presetName)
            ep.ins.presets.remove(presetName)
            ep.write()
            list()
        }
    }

    @SubCommand @Description("列出所有环境预设配置")
    suspend fun CommandSender.list(
        @Name("preset") presetName: String? = null
    ) {
        var output = ""
        ep.ins.presets.filter { presetName==null || presetName in it.key }.forEach {
            output += "${it.key}: ${it.value}\n"
        }
        sendMessage(output)
    }

    @SubCommand @Description("设置会话(子进程)的入口程序(一般是shell程序)")
    suspend fun CommandSender.shell(
        @Name("preset")presetName: String,
        @Name("shell") vararg shell: String
    ) {
        withCatch {
            val preset = getPresetWithThrow(presetName)

            if(shell.isEmpty())
                ep.ins.presets[presetName]!!.shell = ""
            else
                ep.ins.presets[presetName]!!.shell = shell.joinToString(" ")
            ep.write()

            sendMessage(preset.toString())
        }
    }

    @SubCommand @Description("设置环境的工作目录")
    suspend fun CommandSender.cwd(
        @Name("preset")presetName: String,
        @Name("cwd") vararg cwd: String
    ) {
        withCatch {
            val preset = getPresetWithThrow(presetName)

            if(cwd.isEmpty())
                ep.ins.presets[presetName]!!.cwd = ""
            else
                ep.ins.presets[presetName]!!.cwd = cwd.joinToString(" ")
            ep.write()

            sendMessage(preset.toString())
        }
    }

    @SubCommand @Description("设置环境的环境变量")
    suspend fun CommandSender.env(
        @Name("preset") presetName: String,
        @Name("key") key: String,
        @Name("value") vararg value: String
    ) {
        withCatch {
            val preset = getPresetWithThrow(presetName)

            if(value.isEmpty())
                ep.ins.presets[presetName]!!.env.remove(key)
            else
                ep.ins.presets[presetName]!!.env[key] = value.joinToString(" ")
            ep.write()

            sendMessage(preset.toString())
        }
    }

    @SubCommand @Description("设置环境的初始化命令")
    suspend fun CommandSender.exec(
        @Name("preset") presetName: String,
        @Name("exec") vararg exec: String
    ) {
        withCatch {
            val preset = getPresetWithThrow(presetName)

            if(exec.isEmpty())
                ep.ins.presets[presetName]!!.exec = ""
            else
                ep.ins.presets[presetName]!!.exec = exec.joinToString(" ")
            ep.write()

            sendMessage(preset.toString())
        }
    }

    @SubCommand @Description("设置默认的环境预设方案")
    suspend fun CommandSender.def(
        @Name("preset") presetName: String? =null
    ) {
        withCatch {
            if(presetName == null)
            {
                sendMessage("The default preset is <${ep.ins.defaultPreset}>")
            } else {
                val preset = getPresetWithThrow(presetName)

                ep.ins.defaultPreset = presetName
                ep.write()

                sendMessage(preset.toString())
            }
        }
    }

    @SubCommand @Description("从配置文件重新加载环境预设方案")
    suspend fun CommandSender.reload() {
        withCatch {
            ep.read()
            list()
        }
    }

    fun getPresetWithThrow(presetName: String): EnvPresets.Preset
    {
        return ep.ins.presets[presetName]
            ?: throw PresetNotFoundException("The preset '$presetName' was not found")
    }

    suspend inline fun CommandSender.withCatch(block: CommandSender.() -> Unit)
    {
        try { block() } catch (e: BaseException) { sendMessage(e.message ?: e.stackTraceToString()) }
    }

    suspend inline fun CommandSender.withPermission(block: suspend CommandSender.() -> Unit)
    {
        if(user == null || user!!.id in MainConfig.admins)
            block()
    }
}