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
                throw PresetAlreadyExistedYetException(presetName)
            if(!Charset.isSupported(charset))
                throw UnsupportedCharsetException(charset)
            if(shell.isEmpty())
                throw MissingParamaterException("shell")

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
            sendMessage("已删除环境预设'$presetName'")
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
        sendMessage(output.ifEmpty { "还没有任何环境预设" })
    }

    @SubCommand @Description("设置会话的启动程序")
    suspend fun CommandSender.shell(
        @Name("preset")presetName: String,
        @Name("shell") vararg shell: String
    ) {
        withCatch {
            val preset = getPresetWithThrow(presetName)
            if(shell.isEmpty())
            {
                ep.ins.presets[presetName]!!.shell = ""
                sendMessage("已清空${presetName}预设的shell选项")
            } else {
                val s = shell.joinToString(" ")
                ep.ins.presets[presetName]!!.shell = s
                sendMessage("已更新${presetName}预设的shell: '$s'")
            }
            ep.write()
        }
    }

    @SubCommand @Description("设置会话的工作目录")
    suspend fun CommandSender.cwd(
        @Name("preset")presetName: String,
        @Name("dir") vararg dir: String
    ) {
        withCatch {
            val preset = getPresetWithThrow(presetName)
            if(dir.isEmpty())
            {
                ep.ins.presets[presetName]!!.cwd = ""
                sendMessage("已清空${presetName}预设的cwd选项")
            } else {
                val d = dir.joinToString(" ")
                ep.ins.presets[presetName]!!.cwd = d
                sendMessage("已更新${presetName}预设的cwd: '$d'")
            }
            ep.write()
        }
    }

    @SubCommand @Description("设置会话的环境变量")
    suspend fun CommandSender.env(
        @Name("preset") presetName: String,
        @Name("key") key: String = "",
        @Name("value") vararg value: String
    ) {
        withCatch {
            val preset = getPresetWithThrow(presetName)
            if(key.isNotEmpty())
            {
                if(value.isEmpty())
                {
                    ep.ins.presets[presetName]!!.env.remove(key)
                    sendMessage("已移除${presetName}预设的环境变量: '$key'")
                } else {
                    val v = value.joinToString(" ")
                    ep.ins.presets[presetName]!!.env[key] = v
                    sendMessage("已更新${presetName}预设的环境变量'$key': '$v'")
                }
                ep.write()
            }
            sendMessage(preset.env.toString())
        }
    }

    @SubCommand @Description("设置会话的初始化命令")
    suspend fun CommandSender.exec(
        @Name("preset") presetName: String,
        @Name("exec") vararg exec: String
    ) {
        withCatch {
            val preset = getPresetWithThrow(presetName)
            if(exec.isEmpty())
            {
                ep.ins.presets[presetName]!!.exec = ""
                sendMessage("已清空${presetName}预设的exec选项")
            } else {
                val e = exec.joinToString(" ")
                ep.ins.presets[presetName]!!.exec = e
                sendMessage("已更新${presetName}预设的exec: '$e'")
            }
            ep.write()
        }
    }

    @SubCommand @Description("设置会话标准输入输出使用的字符集")
    suspend fun CommandSender.charset(
        @Name("preset") presetName: String,
        @Name("charset") charset: String =""
    ) {
        withCatch {
            val preset = getPresetWithThrow(presetName)
            if(charset.isEmpty()) {
                ep.ins.presets[presetName]!!.charset = ""
                sendMessage("已清空${presetName}预设的charset选项")
            } else {
                if(!Charset.isSupported(charset))
                    throw UnsupportedCharsetException(charset)
                ep.ins.presets[presetName]!!.charset = charset
                sendMessage("已更新${presetName}预设的charset: '$charset'")
            }
            ep.write()
        }
    }

    @SubCommand @Description("设置默认的环境预设方案")
    suspend fun CommandSender.def(
        @Name("preset") presetName: String? =null
    ) {
        withCatch {
            if(presetName != null)
            {
                val preset = getPresetWithThrow(presetName)
                ep.ins.defaultPreset = presetName
                ep.write()
                sendMessage("已更新默认环境预设: '$presetName'")
            } else {
                sendMessage("当前默认环境预设是: ${ep.ins.defaultPreset}")
            }
        }
    }

    @SubCommand @Description("从配置文件重新加载环境预设方案")
    suspend fun CommandSender.reload() {
        withCatch {
            ep.read()
            sendMessage("环境预设配置文件重载完成")
        }
    }

    fun getPresetWithThrow(presetName: String): Preset
    {
        return ep.ins.presets[presetName]
            ?: throw PresetNotFoundException(presetName)
    }

    suspend inline fun CommandSender.withCatch(block: CommandSender.() -> Unit)
    {
        try { block() } catch (e: BaseException) { sendMessage(e.message ?: e.stackTraceToString()) }
    }
}