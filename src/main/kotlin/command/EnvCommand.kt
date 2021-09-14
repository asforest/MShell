package com.github.asforest.mshell.command

import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import com.github.asforest.mshell.MShell
import com.github.asforest.mshell.configuration.ConfigProxy
import com.github.asforest.mshell.configuration.Preset
import com.github.asforest.mshell.configuration.EnvironmentPresets
import com.github.asforest.mshell.exception.*
import com.github.asforest.mshell.permission.MShellPermissions
import java.nio.charset.Charset

object EnvCommand : CompositeCommand(
    MShell,
    primaryName = "mshelle",
    description = "MShell插件配置指令",
    secondaryNames = arrayOf("mse", "me"),
    parentPermission = MShellPermissions.all
) {
    val ep: ConfigProxy<EnvironmentPresets> get() = MShell.ep

    @SubCommand @Description("创建一个环境预设")
    suspend fun CommandSender.add(
        @Name("preset") presetName: String,
        @Name("charset") charset: String,
        @Name("shell") vararg shell: String
    ) {
        withCatch {
            if (presetName in ep.ins.presets.keys)
                throw PresetAlreadyExistedYetException("The preset '$presetName' has already existed yet")
            if(!Charset.isSupported(charset))
                throw UnsupportedCharsetException("The charset '$charset' is unsupported")
            if(shell.isEmpty())
                throw MissingParamaterException("The paramater 'shell' is missing or empty")

            ep.ins.presets[presetName] = Preset(shell.joinToString(" "), charset)

            // 如果这是创建的第一个预设，就设置为默认预设
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
        sendMessage(output.ifEmpty { " " })
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

    @SubCommand @Description("设置环境的编码方式")
    suspend fun CommandSender.charset(
        @Name("preset") presetName: String,
        @Name("charset") charset: String =""
    ) {
        withCatch {
            val preset = getPresetWithThrow(presetName)

            if(charset.isEmpty()) {
                ep.ins.presets[presetName]!!.charset = ""
            } else {
                if(Charset.isSupported(charset))
                    ep.ins.presets[presetName]!!.charset = charset
                else
                    throw UnsupportedCharsetException("The charset '$charset' is unsupported")
            }
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

    fun getPresetWithThrow(presetName: String): Preset
    {
        return ep.ins.presets[presetName]
            ?: throw PresetNotFoundException("The preset '$presetName' was not found")
    }

    suspend inline fun CommandSender.withCatch(block: CommandSender.() -> Unit)
    {
        try { block() } catch (e: BaseException) { sendMessage(e.message ?: e.stackTraceToString()) }
    }
}